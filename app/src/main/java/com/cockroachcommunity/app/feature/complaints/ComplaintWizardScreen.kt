package com.cockroachcommunity.app.feature.complaints

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.*
import com.cockroachcommunity.app.core.session.UserProfile
import com.cockroachcommunity.app.data.complaints.*
import com.cockroachcommunity.app.feature.session.RequireMember
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

@Composable
fun ComplaintWizardScreen(policeFocus: Boolean, onDone: () -> Unit) {
    RequireMember("Sign in with Google to file a complaint. Your contact details stay private.") { profile ->
        WizardContent(profile, policeFocus, onDone)
    }
}

@Composable
private fun WizardContent(profile: UserProfile, policeFocus: Boolean, onDone: () -> Unit, viewModel: ComplaintWizardViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(profile.id) { viewModel.start(profile, policeFocus) }
    if (!s.ready) { LoadingBox(); return }

    val lang = s.draft.language
    fun t(key: String) = ComplaintStrings.t(lang, key)

    if (s.doneReference != null) {
        Column(Modifier.fillMaxSize().padding(Dimens.Space24), verticalArrangement = Arrangement.Center) {
            Text(t("success").uppercase(), style = MaterialTheme.typography.displayMedium, color = Green)
            Spacer(Modifier.height(Dimens.Space12))
            Text("Reference: ${s.doneReference}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Dimens.Space8))
            Text("Track its status anytime from Complaints or your notifications.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(Dimens.Space24))
            CjpButton("Done", onClick = onDone, gold = true, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    val steps = listOf("stepIssue", "stepContact", "stepWhere", "stepEvidence")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
        SectionTitle(t(if (policeFocus) "formTitlePolice" else "formTitle"))

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(t("language").uppercase(), style = MaterialTheme.typography.labelMedium)
            listOf("en" to "EN", "hi" to "हिन्दी").forEach { (code, label) ->
                FilterChip(selected = lang == code, onClick = { viewModel.edit { it.copy(language = code) } }, label = { Text(label) })
            }
        }
        if (s.restored) {
            Text(t("draftResume"), style = MaterialTheme.typography.bodySmall)
            CjpOutlinedButton(t("clearDraft"), onClick = viewModel::clearDraft)
        }

        Text("${s.step + 1}/${steps.size} · ${t(steps[s.step])}".uppercase(), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { (s.step + 1) / steps.size.toFloat() }, modifier = Modifier.fillMaxWidth(), color = Gold)

        when (s.step) {
            0 -> StepIssue(s, lang, policeFocus, viewModel)
            1 -> StepContact(s, lang, viewModel)
            2 -> StepWhere(s, lang, viewModel)
            else -> StepEvidence(s, lang, viewModel)
        }

        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), modifier = Modifier.fillMaxWidth()) {
            if (s.step > 0) CjpOutlinedButton(t("back"), onClick = viewModel::back, enabled = !s.submitting)
            if (s.step < steps.lastIndex) {
                CjpButton(t("next"), onClick = viewModel::next, modifier = Modifier.weight(1f))
            } else {
                CjpButton(t(if (s.submitting) "submitting" else "submit"), onClick = { viewModel.submit(profile) }, enabled = !s.submitting, gold = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier, hint: String? = null, lines: Int = 1, onChange: (String) -> Unit) {
    Column(modifier) {
        OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = lines == 1, minLines = lines, modifier = Modifier.fillMaxWidth())
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun StepIssue(s: WizardState, lang: String, policeFocus: Boolean, vm: ComplaintWizardViewModel) {
    val d = s.draft
    fun t(key: String) = ComplaintStrings.t(lang, key)
    val ordered = if (policeFocus) PoliceIssueTypes + IssueTypes.filter { it !in PoliceIssueTypes } else IssueTypes
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text ->
                vm.edit { it.copy(description = (it.description + " " + text).trim(), voiceTranscript = text) }
            }
        }
    }
    var voiceError by remember { mutableStateOf<String?>(null) }

    if (policeFocus) Text(t("policeHint"), style = MaterialTheme.typography.bodyMedium)
    Text(t("issueType").uppercase(), style = MaterialTheme.typography.labelLarge)
    FlowRowChips(ordered, d.issueType) { issue ->
        vm.edit { it.copy(issueType = issue, department = if (isPoliceIssue(issue)) "Police" else it.department, urgency = if (isPoliceIssue(issue) && it.urgency == "low") "high" else it.urgency) }
    }
    Field(t("title"), d.title, hint = t("titleHint")) { v -> vm.edit { it.copy(title = v.take(120)) } }
    Field(t("description"), d.description, hint = t("descriptionHint"), lines = 5) { v -> vm.edit { it.copy(description = v) } }
    CjpOutlinedButton(t("voiceStart"), onClick = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == "hi") "hi-IN" else "en-IN")
        }
        runCatching { speech.launch(intent) }.onFailure { voiceError = ComplaintStrings.t(lang, "voiceUnsupported") }
    })
    voiceError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    Text(t("urgency").uppercase(), style = MaterialTheme.typography.labelLarge)
    FlowRowChips(UrgencyLevels, d.urgency, labeler = { it.uppercase() }) { u -> vm.edit { it.copy(urgency = u) } }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = d.anonymous, onCheckedChange = { c -> vm.edit { it.copy(anonymous = c) } })
        Text(t("anonymous"), style = MaterialTheme.typography.bodyMedium)
    }
    // Honeypot: hidden from people, filled by bots
    OutlinedTextField(s.honeypot, vm::setHoneypot, modifier = Modifier.height(0.dp).width(0.dp))
}

@Composable
private fun FlowRowChips(options: List<String>, selected: String, labeler: (String) -> String = { it }, onSelect: (String) -> Unit) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(labeler(o)) }) }
    }
}

@Composable
private fun StepContact(s: WizardState, lang: String, vm: ComplaintWizardViewModel) {
    val c = s.draft.contact
    fun t(key: String) = ComplaintStrings.t(lang, key)
    fun edit(block: (ReporterContact) -> ReporterContact) = vm.edit { it.copy(contact = block(it.contact)) }
    Text(t("contactIntro"), style = MaterialTheme.typography.bodyMedium)
    Field(t("fullName"), c.name) { v -> edit { it.copy(name = v) } }
    Field(t("email"), c.email) { v -> edit { it.copy(email = v) } }
    Field(t("phone"), c.mobile) { v -> edit { it.copy(mobile = v) } }
    Field(t("instagram"), c.instagram) { v -> edit { it.copy(instagram = v) } }
    Field(t("facebook"), c.facebook) { v -> edit { it.copy(facebook = v) } }
    Field(t("twitter"), c.twitter) { v -> edit { it.copy(twitter = v) } }
    Field(t("otherSocial"), c.otherSocial) { v -> edit { it.copy(otherSocial = v) } }
}

@SuppressLint("MissingPermission")
@Composable
private fun StepWhere(s: WizardState, lang: String, vm: ComplaintWizardViewModel) {
    val d = s.draft
    fun t(key: String) = ComplaintStrings.t(lang, key)
    val context = LocalContext.current
    var locating by remember { mutableStateOf(false) }

    fun locate() {
        locating = true
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc -> loc?.let { l -> vm.edit { it.copy(lat = l.latitude, lng = l.longitude) } }; locating = false }
            .addOnFailureListener { locating = false }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate()
    }

    Field(t("state"), d.state) { v -> vm.edit { it.copy(state = v) } }
    Field(t("city"), d.city) { v -> vm.edit { it.copy(city = v) } }
    Field(t("area"), d.area) { v -> vm.edit { it.copy(area = v) } }
    Field("Address (optional)", d.address) { v -> vm.edit { it.copy(address = v) } }
    Field("Pincode (optional)", d.pincode) { v -> vm.edit { it.copy(pincode = v.filter(Char::isDigit).take(6)) } }
    CjpOutlinedButton(
        if (d.lat != null) "${t("locationSet")}: %.4f, %.4f".format(Locale.US, d.lat, d.lng) else t("useLocation"),
        onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
        enabled = !locating,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepEvidence(s: WizardState, lang: String, vm: ComplaintWizardViewModel) {
    fun t(key: String) = ComplaintStrings.t(lang, key)
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        vm.addFiles(uris.mapNotNull { describe(context, it) })
    }
    Text(t("mediaLabel"), style = MaterialTheme.typography.bodyMedium)
    if (isPoliceIssue(s.draft.issueType)) Text(t("mediaRequired"), style = MaterialTheme.typography.bodyMedium, color = Red)
    CjpOutlinedButton(t("addFiles"), onClick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }, modifier = Modifier.fillMaxWidth())
    if (s.files.isEmpty()) Text(t("noFiles"), style = MaterialTheme.typography.bodySmall)
    s.files.forEach { f ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyMedium)
                s.progress[f.name]?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth(), color = Gold) }
            }
            TextButton(onClick = { vm.removeFile(f.uri) }, enabled = !s.submitting) { Text("REMOVE") }
        }
    }
    Spacer(Modifier.height(Dimens.Space8))
    Text(t("readyTitle").uppercase(), style = MaterialTheme.typography.titleMedium)
    Text(t("readyBody"), style = MaterialTheme.typography.bodyMedium)
}

private fun describe(context: Context, uri: Uri): PickedFile? {
    val mime = context.contentResolver.getType(uri) ?: return null
    var name = "file"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
            c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
        }
    }
    return PickedFile(uri.toString(), name, mime, size)
}
