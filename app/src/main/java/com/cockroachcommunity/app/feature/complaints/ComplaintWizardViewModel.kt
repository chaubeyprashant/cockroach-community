package com.cockroachcommunity.app.feature.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.UserProfile
import com.cockroachcommunity.app.data.complaints.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WizardState(
    val ready: Boolean = false,
    val restored: Boolean = false,
    val step: Int = 0,
    val draft: ComplaintDraft = ComplaintDraft(),
    val files: List<PickedFile> = emptyList(),
    val progress: Map<String, Int> = emptyMap(),
    val submitting: Boolean = false,
    val error: String? = null,
    val doneReference: String? = null,
    val honeypot: String = ""
)

@HiltViewModel
class ComplaintWizardViewModel @Inject constructor(
    private val repository: ComplaintsRepository,
    private val drafts: ComplaintDraftStore
) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    /** Loads the saved draft once, then fills empty contact fields from the profile. */
    fun start(profile: UserProfile, policeFocus: Boolean) {
        if (_state.value.ready) return
        viewModelScope.launch {
            val saved = drafts.load()
            val base = saved ?: ComplaintDraft(
                issueType = if (policeFocus) PoliceIssueTypes.first() else "Other",
                department = if (policeFocus) "Police" else "",
                urgency = if (policeFocus) "high" else "medium"
            )
            val c = base.contact
            val filled = base.copy(
                contact = c.copy(
                    name = c.name.ifBlank { profile.name },
                    email = c.email.ifBlank { profile.email },
                    mobile = c.mobile.ifBlank { profile.phone }
                ),
                city = base.city.ifBlank { profile.city },
                state = base.state.ifBlank { profile.state }
            )
            _state.value = WizardState(ready = true, restored = saved != null, draft = filled)
        }
    }

    fun edit(block: (ComplaintDraft) -> ComplaintDraft) {
        _state.update { it.copy(draft = block(it.draft), error = null) }
        persist()
    }

    fun setHoneypot(v: String) = _state.update { it.copy(honeypot = v) }

    fun addFiles(picked: List<PickedFile>) {
        val (ok, tooBig) = picked.partition { it.size <= MaxUploadBytes }
        _state.update {
            it.copy(
                files = (it.files + ok).distinctBy { f -> f.uri },
                error = tooBig.firstOrNull()?.let { f -> "${f.name} is larger than 20MB." }
            )
        }
    }

    fun removeFile(uri: String) = _state.update { it.copy(files = it.files.filterNot { f -> f.uri == uri }) }

    fun clearDraft() {
        viewModelScope.launch { drafts.clear() }
        _state.update { it.copy(draft = ComplaintDraft(language = it.draft.language), restored = false, step = 0, files = emptyList()) }
    }

    private fun persist() {
        val d = _state.value.draft
        viewModelScope.launch { drafts.save(d) }
    }

    private fun validate(step: Int, s: WizardState): String? {
        val d = s.draft
        return when (step) {
            0 -> when {
                d.title.trim().length < 5 -> ComplaintStrings.t(d.language, "titleHint")
                d.description.trim().length < 20 -> ComplaintStrings.t(d.language, "descriptionHint")
                else -> null
            }
            1 -> if (d.contact.name.isBlank() || d.contact.email.isBlank() || d.contact.mobile.isBlank())
                "${ComplaintStrings.t(d.language, "fullName")}, ${ComplaintStrings.t(d.language, "email")}, ${ComplaintStrings.t(d.language, "phone")}" else null
            2 -> if (d.city.isBlank() || d.area.isBlank()) "${ComplaintStrings.t(d.language, "city")}, ${ComplaintStrings.t(d.language, "area")}" else null
            else -> if (isPoliceIssue(d.issueType) && s.files.isEmpty()) ComplaintStrings.t(d.language, "mediaRequired") else null
        }
    }

    fun next() {
        val s = _state.value
        val error = validate(s.step, s)
        _state.update { if (error != null) it.copy(error = error) else it.copy(step = it.step + 1, error = null) }
    }

    fun back() = _state.update { it.copy(step = maxOf(0, it.step - 1), error = null) }

    fun submit(profile: UserProfile) {
        val s = _state.value
        if (s.honeypot.isNotBlank()) { // bot trap, mirrors the website
            _state.update { it.copy(doneReference = "CJP") }
            return
        }
        validate(3, s)?.let { e -> _state.update { it.copy(error = e) }; return }
        val d = s.draft.copy(
            state = s.draft.state.ifBlank { "Not specified" },
            pincode = s.draft.pincode.ifBlank { "000000" }
        )
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val result = repository.create(d, profile, s.files) { name, pct -> _state.update { st -> st.copy(progress = st.progress + (name to pct)) } }
            result.onSuccess { (_, ref) ->
                drafts.clear()
                _state.update { it.copy(submitting = false, doneReference = ref) }
            }.onFailure { e ->
                _state.update { it.copy(submitting = false, error = e.message ?: "Could not submit complaint") }
            }
        }
    }
}
