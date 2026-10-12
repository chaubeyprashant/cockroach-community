package com.cockroachcommunity.app.core.common

import kotlin.math.abs

data class SocialPersona(
    val displayName: String,
    val handle: String,
    val bio: String,
    val avatarEmoji: String
)

object SocialPersonaManager {

    fun getPersonaForId(id: String): SocialPersona {
        val name = formatPeerDisplayName(id)
        val handle = getPeerHandle(id)
        val bio = getPeerBio(id)
        val emoji = getPeerEmoji(id)
        return SocialPersona(name, handle, bio, emoji)
    }

    /**
     * Preserves real user names and cleans up machine strings without using fake fictional personas.
     */
    fun formatPeerDisplayName(peerId: String): String {
        val clean = peerId.trim()
        if (clean.isBlank()) return "Nearby Friend"

        // If it starts with "Peer ·" or "Peer ", return friendly fallback
        if (clean.startsWith("Peer ·", ignoreCase = true) || clean.startsWith("Peer ", ignoreCase = true)) {
            return "Nearby Friend"
        }

        // MAC address (e.g. ca:5c:71:46:57:a6)
        if (clean.contains(":")) {
            return "Nearby Friend"
        }

        // Hex hashes (e.g. 67e38ac8a569bfb9) or endpoint IDs (e.g. ab12)
        val isLikelyHash = clean.length in 4..64 && clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        if (isLikelyHash) {
            return "Nearby Friend"
        }

        // Otherwise it is the actual user name entered during sign-up
        return clean
    }

    fun getPeerHandle(peerId: String): String {
        val name = formatPeerDisplayName(peerId)
        return if (name != "Nearby Friend") {
            "@${name.lowercase().replace(" ", "_").take(15)}"
        } else {
            "@mesh_user"
        }
    }

    fun getPeerBio(peerId: String): String {
        return "Connected via offline mesh ⚡"
    }

    fun getPeerEmoji(peerId: String): String {
        return "⚡"
    }
}

