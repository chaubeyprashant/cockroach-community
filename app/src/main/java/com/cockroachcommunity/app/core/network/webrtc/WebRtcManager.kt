package com.cockroachcommunity.app.core.network.webrtc

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import android.media.AudioManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var rootEglBase: EglBase? = null

    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null

    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    // For observing incoming remote video tracks
    private val _remoteVideoTrackFlow = MutableSharedFlow<VideoTrack>(extraBufferCapacity = 5)
    val remoteVideoTrackFlow: SharedFlow<VideoTrack> = _remoteVideoTrackFlow.asSharedFlow()

    // For emitting signaling messages to send over TCP
    private val _signalingMessages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 64)
    val signalingMessages: SharedFlow<SignalingMessage> = _signalingMessages.asSharedFlow()

    init {
        initWebRtc()
    }

    private fun initWebRtc() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        rootEglBase = EglBase.create()
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(rootEglBase?.eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
        
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported())
            .setUseHardwareNoiseSuppressor(JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported())
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
            
        audioDeviceModule.release()
    }
    
    fun getEglBaseContext(): EglBase.Context? {
        return rootEglBase?.eglBaseContext
    }

    fun startCall(isInitiator: Boolean, isVideoCall: Boolean = true) {
        android.util.Log.e("CockroachCommunity", "WebRtcManager startCall: initiator=$isInitiator, isVideoCall=$isVideoCall")
        
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isVideoCall
        
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList()) // Local connection, no STUN/TURN needed
        
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    _signalingMessages.tryEmit(SignalingMessage.IceCandidateMessage(it))
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let {
                    _signalingMessages.tryEmit(SignalingMessage.RemoteVideoTrackReady)
                    _remoteVideoTrackFlow.tryEmit(it)
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                receiver?.track()?.let { track ->
                    if (track.kind() == "video") {
                        val videoTrack = track as VideoTrack
                        _signalingMessages.tryEmit(SignalingMessage.RemoteVideoTrackReady)
                        _remoteVideoTrackFlow.tryEmit(videoTrack)
                    }
                }
            }
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        setupLocalMedia(isVideoCall)

        if (isInitiator) {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
            }
            android.util.Log.e("CockroachCommunity", "Creating offer...")
            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    android.util.Log.e("CockroachCommunity", "createOffer success")
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(desc: SessionDescription?) {}
                        override fun onSetSuccess() {
                            android.util.Log.e("CockroachCommunity", "setLocalDescription success. Emitting offer")
                            sdp?.let { _signalingMessages.tryEmit(SignalingMessage.OfferMessage(it, isVideoCall)) }
                        }
                        override fun onCreateFailure(p0: String?) {
                            android.util.Log.e("CockroachCommunity", "setLocalDesc onCreateFailure: $p0")
                        }
                        override fun onSetFailure(p0: String?) {
                            android.util.Log.e("CockroachCommunity", "setLocalDesc onSetFailure: $p0")
                        }
                    }, sdp)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {
                    android.util.Log.e("CockroachCommunity", "createOffer failed: $error")
                }
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }

    private fun setupLocalMedia(isVideoCall: Boolean) {
        val factory = peerConnectionFactory ?: return

        // Audio
        val audioConstraints = MediaConstraints()
        val audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))

        // Video
        if (isVideoCall) {
            videoCapturer = createVideoCapturer()
            videoCapturer?.let { capturer ->
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
                val videoSource = factory.createVideoSource(capturer.isScreencast)
                capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
                capturer.startCapture(1280, 720, 30)
                localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
                peerConnection?.addTrack(localVideoTrack, listOf("ARDAMS"))
            }
        }
    }
    
    fun getLocalVideoTrack(): VideoTrack? {
        return localVideoTrack
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun setSpeakerphoneEnabled(enabled: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = enabled
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        // Try front camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Try back camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun handleOffer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainIceCandidates()
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                answerSdp?.let { _signalingMessages.tryEmit(SignalingMessage.AnswerMessage(it)) }
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        }, answerSdp)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, constraints)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    fun handleAnswer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainIceCandidates()
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    private fun drainIceCandidates() {
        pendingIceCandidates.forEach { peerConnection?.addIceCandidate(it) }
        pendingIceCandidates.clear()
    }

    fun handleIceCandidate(iceCandidate: IceCandidate) {
        if (isRemoteDescriptionSet) {
            peerConnection?.addIceCandidate(iceCandidate)
        } else {
            pendingIceCandidates.add(iceCandidate)
        }
    }

    fun endCall() {
        try { videoCapturer?.stopCapture() } catch (e: Exception) {}
        videoCapturer?.dispose()
        videoCapturer = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        peerConnection?.close()
        peerConnection = null
        
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        
        isRemoteDescriptionSet = false
        pendingIceCandidates.clear()
    }

    sealed class SignalingMessage {
        data class OfferMessage(val sdp: SessionDescription, val isVideoCall: Boolean = true) : SignalingMessage()
        data class AnswerMessage(val sdp: SessionDescription) : SignalingMessage()
        data class IceCandidateMessage(val candidate: IceCandidate) : SignalingMessage()
        object RemoteVideoTrackReady : SignalingMessage()
    }
}
