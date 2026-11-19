package com.cockroachcommunity.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.UserProfile

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.Space16),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
    ) {
        SectionTitle("Profile")

        when (val s = session) {
            is Session.Member -> {
                MemberForm(profile = s.profile, busy = busy, onSave = { n, p, c, st -> viewModel.save(s.profile.id, n, p, c, st) })
                CjpOutlinedButton("Sign out", onClick = viewModel::signOut, enabled = !busy, modifier = Modifier.fillMaxWidth())
                CjpOutlinedButton("Delete my account", onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
            is Session.Anonymous -> {
                CjpCard(modifier = Modifier.fillMaxWidth()) {
                    Text("YOU ARE BROWSING AS A GUEST", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.Space8))
                    Text(
                        "Sign in with Google to file complaints, support them, comment, join the forum and chat.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(Dimens.Space16))
                    CjpButton("Sign in with Google", onClick = { viewModel.signInWithGoogle(context) }, enabled = !busy, gold = true, modifier = Modifier.fillMaxWidth())
                }
                CjpOutlinedButton("Leave guest mode", onClick = viewModel::signOut, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
            else -> LoadingBox()
        }

        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }

    val uid = (session as? Session.Member)?.profile?.id
    if (confirmDelete && uid != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("DELETE ACCOUNT?") },
            text = { Text("This removes your profile and sign-in. Complaints and posts you already made stay on the platform.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAccount(context, uid)
                }) { Text("DELETE", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL") } }
        )
    }
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(4_000)
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun MemberForm(profile: UserProfile, busy: Boolean, onSave: (String, String, String, String) -> Unit) {
    var name by remember(profile.id, profile.name) { mutableStateOf(profile.name) }
    var phone by remember(profile.id, profile.phone) { mutableStateOf(profile.phone) }
    var city by remember(profile.id, profile.city) { mutableStateOf(profile.city) }
    var state by remember(profile.id, profile.state) { mutableStateOf(profile.state) }

    CjpCard(modifier = Modifier.fillMaxWidth()) {
        Text(profile.email.ifBlank { "Signed in" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profile.isAdmin) CjpChip("Admin", modifier = Modifier.padding(top = Dimens.Space8))
        Spacer(Modifier.height(Dimens.Space12))
        Field("Name", name) { name = it }
        Field("WhatsApp / phone", phone) { phone = it }
        Field("City", city) { city = it }
        Field("State", state) { state = it }
        Spacer(Modifier.height(Dimens.Space8))
        CjpButton("Save", onClick = { onSave(name, phone, city, state) }, enabled = !busy, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.Space8)
    )
}
