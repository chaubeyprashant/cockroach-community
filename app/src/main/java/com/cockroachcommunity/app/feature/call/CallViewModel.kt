package com.cockroachcommunity.app.feature.call

import androidx.lifecycle.ViewModel
import com.cockroachcommunity.app.core.network.repository.CallRepository
import com.cockroachcommunity.app.core.network.webrtc.WebRtcManager
import com.cockroachcommunity.app.core.network.repository.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import javax.inject.Inject

import com.cockroachcommunity.app.core.network.repository.MessageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.database.entity.CallHistoryEntity

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val webRtcManager: WebRtcManager,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val callState: StateFlow<CallState> = callRepository.callState
    val isVideoCall: StateFlow<Boolean> = callRepository.isVideoCall
    val remoteVideoTrack: StateFlow<VideoTrack?> = callRepository.remoteVideoTrack
    
    val callHistory: StateFlow<List<CallHistoryEntity>> = callRepository.callHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val connectedPeerIdentity: StateFlow<String?> = messageRepository.connectedPeerIdentity.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    fun getEglBaseContext(): EglBase.Context? {
        return webRtcManager.getEglBaseContext()
    }

    fun getLocalVideoTrack(): VideoTrack? {
        return callRepository.getLocalVideoTrack()
    }

    fun startCall(isVideoCall: Boolean = true) {
        if (connectedPeerIdentity.value == null) {
            // Cannot start call if not connected
            return
        }
        callRepository.startCall(isVideoCall)
    }

    fun acceptCall() {
        callRepository.acceptIncomingCall()
    }

    fun setSpeakerphoneEnabled(enabled: Boolean) {
        webRtcManager.setSpeakerphoneEnabled(enabled)
    }

    fun declineCall() {
        callRepository.declineIncomingCall()
    }

    fun endCall() {
        callRepository.endCall()
    }

    fun setAudioEnabled(enabled: Boolean) {
        webRtcManager.setAudioEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        webRtcManager.setVideoEnabled(enabled)
    }

    fun switchCamera() {
        webRtcManager.switchCamera()
    }
}
