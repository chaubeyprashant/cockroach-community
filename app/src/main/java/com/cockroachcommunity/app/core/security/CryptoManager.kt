package com.cockroachcommunity.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {
    private val keyAlias = "cockroach_community_ec_key"
    private val androidKeyStore = "AndroidKeyStore"
    
    private var sharedSecretAesKey: SecretKeySpec? = null

    init {
        generateKeyPairIfNeeded()
    }

    private fun generateKeyPairIfNeeded() {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, androidKeyStore)
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_AGREE_KEY
            ).build()
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }
    }

    fun getPublicKeyBase64(): String {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        val publicKey = keyStore.getCertificate(keyAlias).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun deriveSharedSecret(peerPublicKeyBase64: String) {
        val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        val privateKey = keyStore.getKey(keyAlias, null) as PrivateKey

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Hash the shared secret to create a 256-bit AES key
        val digest = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = digest.digest(sharedSecret)
        sharedSecretAesKey = SecretKeySpec(aesKeyBytes, "AES")
    }
    
    fun clearSharedSecret() {
        sharedSecretAesKey = null
    }

    fun isReadyForEncryption(): Boolean = sharedSecretAesKey != null

    fun encrypt(plaintext: ByteArray): Pair<ByteArray, ByteArray> { // Returns IV and Ciphertext
        val key = sharedSecretAesKey ?: throw IllegalStateException("Shared secret not derived yet")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return Pair(iv, ciphertext)
    }

    fun decrypt(iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val key = sharedSecretAesKey ?: throw IllegalStateException("Shared secret not derived yet")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }
}
