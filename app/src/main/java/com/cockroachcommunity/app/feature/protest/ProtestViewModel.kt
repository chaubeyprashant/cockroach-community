package com.cockroachcommunity.app.feature.protest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.UserProfile
import com.cockroachcommunity.app.data.protest.JoinMode
import com.cockroachcommunity.app.data.protest.ProtestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProtestFormState(val submitting: Boolean = false, val message: String? = null, val success: Boolean = false)

@HiltViewModel
class ProtestViewModel @Inject constructor(private val repository: ProtestRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProtestFormState())
    val state: StateFlow<ProtestFormState> = _state.asStateFlow()

    fun submit(profile: UserProfile, phone: String, city: String, mode: JoinMode?) {
        val finalPhone = phone.ifBlank { profile.phone }
        if (finalPhone.isBlank()) {
            _state.value = ProtestFormState(message = "Please enter a valid WhatsApp number.")
            return
        }
        if (mode == null) {
            _state.value = ProtestFormState(message = "Please select how you will join.")
            return
        }
        viewModelScope.launch {
            _state.value = ProtestFormState(submitting = true)
            val result = repository.register(profile, finalPhone, city, mode)
            _state.value = if (result.isSuccess) {
                ProtestFormState(
                    success = true,
                    message = if (mode == JoinMode.DELHI) "Registered. See you at Jantar Mantar — 24/7 peaceful presence."
                    else "Registered for virtual solidarity. We will share WhatsApp updates soon."
                )
            } else ProtestFormState(message = "Failed to register. Please try again.")
        }
    }
}
