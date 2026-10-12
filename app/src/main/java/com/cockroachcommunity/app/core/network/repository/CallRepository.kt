package com.cockroachcommunity.app.core.network.repository

import com.cockroachcommunity.app.core.network.model.NetworkPayload
import com.cockroachcommunity.app.core.network.webrtc.WebRtcManager
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.CallHistoryEntity
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

enum class CallState {
    IDLE,
    INCOMING,
    ACTIVE
}

@Singleton
class CallRepository @Inject constructor(
    private val webRtcManager: WebRtcManager,
    private val messageRepository: MessageRepository,
    private val dao: CockroachCommunityDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isVideoCall = MutableStateFlow(true)
    val isVideoCall: StateFlow<Boolean> = _isVideoCall.asStateFlow()

    private var pendingOffer: SessionDescription? = null
    private var currentCallId: String? = null
    private var currentCallStartTime: Long = 0L
    private var currentCallDirection: Int = 1 // 0=Incoming, 1=Outgoing
    private var pendingIsVideoCall: Boolean = true

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    val callHistory = dao.getCallHistory()

    init {
        scope.launch {
            webRtcManager.signalingMessages.collect { msg ->
                when (msg) {
                    is WebRtcManager.SignalingMessage.OfferMessage -> {
                        android.util.Log.e("CockroachCommunity", "Sending RtcOffer payload")
                        messageRepository.sendPayload(NetworkPayload.RtcOffer(msg.sdp.description, msg.isVideoCall))
                    }
                    is WebRtcManager.SignalingMessage.AnswerMessage -> {
                        messageRepository.sendPayload(NetworkPayload.RtcAnswer(msg.sdp.description))
                    }
                    is WebRtcManager.SignalingMessage.IceCandidateMessage -> {
                        messageRepository.sendPayload(NetworkPayload.RtcIceCandidate(
                            sdpMid = msg.candidate.sdpMid,
                            sdpMLineIndex = msg.candidate.sdpMLineIndex,
                            sdp = msg.candidate.sdp
                        ))
                    }
                    is WebRtcManager.SignalingMessage.RemoteVideoTrackReady -> {
                        // Handled separately below
                    }
                }
            }
        }

        scope.launch {
            webRtcManager.remoteVideoTrackFlow.collect { track ->
                _remoteVideoTrack.value = track
            }
        }

        scope.launch {
            messageRepository.rtcPayloads.collect { payload ->
                when (payload) {
                    is NetworkPayload.RtcOffer -> {
                        android.util.Log.e("CockroachCommunity", "Received RtcOffer! callState is ${_callState.value}")
                        if (_callState.value == CallState.IDLE) {
                            pendingOffer = SessionDescription(SessionDescription.Type.OFFER, payload.sdp)
                            pendingIsVideoCall = payload.isVideoCall
                            _isVideoCall.value = payload.isVideoCall
                            _callState.value = CallState.INCOMING
                            currentCallDirection = 0
                            currentCallId = UUID.randomUUID().toString()
                            currentCallStartTime = System.currentTimeMillis()
                            android.util.Log.e("CockroachCommunity", "callState changed to INCOMING")
                        }
                    }
                    is NetworkPayload.RtcAnswer -> {
                        webRtcManager.handleAnswer(SessionDescription(SessionDescription.Type.ANSWER, payload.sdp))
                    }
                    is NetworkPayload.RtcIceCandidate -> {
                        webRtcManager.handleIceCandidate(IceCandidate(payload.sdpMid, payload.sdpMLineIndex, payload.sdp))
                    }
                    is NetworkPayload.CallEnded -> {
                        endCall(sendNetworkPayload = false)
                    }
                    else -> {}
                }
            }
        }
    }

    fun acceptIncomingCall() {
        if (_callState.value == CallState.INCOMING && pendingOffer != null) {
            _callState.value = CallState.ACTIVE
            currentCallStartTime = System.currentTimeMillis() // Reset start time to when answered
            webRtcManager.startCall(isInitiator = false, isVideoCall = pendingIsVideoCall)
            webRtcManager.handleOffer(pendingOffer!!)
            pendingOffer = null
        }
    }

    fun declineIncomingCall() {
        if (_callState.value == CallState.INCOMING) {
            saveCallHistory(status = 2) // 2=Rejected
            endCall(sendNetworkPayload = true)
        }
    }

    fun startCall(isVideoCall: Boolean = true) {
        android.util.Log.e("CockroachCommunity", "CallRepository startCall: isVideoCall=$isVideoCall")
        _isVideoCall.value = isVideoCall
        _callState.value = CallState.ACTIVE
        currentCallDirection = 1
        currentCallId = UUID.randomUUID().toString()
        currentCallStartTime = System.currentTimeMillis()
        webRtcManager.startCall(isInitiator = true, isVideoCall = isVideoCall)
    }

    fun endCall(sendNetworkPayload: Boolean = true) {
        if (_callState.value == CallState.ACTIVE) {
            saveCallHistory(status = 1) // 1=Answered
        } else if (_callState.value == CallState.INCOMING) {
            saveCallHistory(status = 0) // 0=Missed (if ended remotely before answering)
        }

        if (sendNetworkPayload) {
            scope.launch {
                messageRepository.sendPayload(NetworkPayload.CallEnded("User ended call"))
            }
        }
        _callState.value = CallState.IDLE
        pendingOffer = null
        currentCallId = null
        _remoteVideoTrack.value = null
        webRtcManager.endCall()
    }
    
    private fun saveCallHistory(status: Int) {
        val callId = currentCallId ?: return
        val peerId = messageRepository.connectedPeerIdentity.value ?: "Unknown"
        val duration = if (status == 1) ((System.currentTimeMillis() - currentCallStartTime) / 1000).toInt() else 0
        
        val history = CallHistoryEntity(
            callId = callId,
            peerDeviceId = peerId,
            callType = 1, // 1=Video
            direction = currentCallDirection,
            timestamp = currentCallStartTime,
            durationSeconds = duration,
            status = status
        )
        scope.launch {
            dao.saveCallHistory(history)
        }
    }
    
    fun getLocalVideoTrack(): VideoTrack? {
        return webRtcManager.getLocalVideoTrack()
    }
}
