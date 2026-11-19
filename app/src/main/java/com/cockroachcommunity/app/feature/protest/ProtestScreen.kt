package com.cockroachcommunity.app.feature.protest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.*
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.UserProfile
import com.cockroachcommunity.app.data.content.Campaign
import com.cockroachcommunity.app.data.content.CjpDemands
import com.cockroachcommunity.app.data.protest.JoinMode
import com.cockroachcommunity.app.feature.session.SessionViewModel

@Composable
fun ProtestScreen(
    onReportPolice: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    viewModel: ProtestViewModel = hiltViewModel()
) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Ink).padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            Text("LIVE CAMPAIGN · JANTAR MANTAR", style = MaterialTheme.typography.labelLarge, color = Gold)
            Text("JANTAR MANTAR.\n24/7.\nREGISTER NOW.", style = MaterialTheme.typography.displayMedium, color = Paper)
            Text(Campaign.SUBHEAD, style = MaterialTheme.typography.bodyLarge, color = Paper)
            Meta("WHEN", "${Campaign.EVENT_DATE_LABEL} · 24/7")
            Meta("WHERE", Campaign.VENUE)
            Meta("HOW", "In person or virtually")
        }

        Column(Modifier.padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space16)) {
            SectionTitle("Our Demands")
            Text("Same as CJP — five demands. Zero compromises.", style = MaterialTheme.typography.bodyMedium)
            CjpDemands.forEach { d ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space16)) {
                    Text(d.number, style = MaterialTheme.typography.displayMedium, color = Gold)
                    Column {
                        Text(d.title.uppercase(), style = MaterialTheme.typography.titleMedium)
                        Text(d.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(PaperSoft).padding(Dimens.Space16),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
        ) {
            SectionTitle("Join the protest")
            Text(
                "Tell us if you’ll be at Jantar Mantar or joining virtually. We’ll share WhatsApp updates for coordination.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("• Peaceful civic assembly only.\n• Bring water, ID, and stay with the group.", style = MaterialTheme.typography.bodyMedium)

            CjpCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.background) {
                when (val s = session) {
                    is Session.Member -> if (state.success) {
                        Text(state.message.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = Green)
                    } else RegisterForm(s.profile, state, onSubmit = viewModel::submit)
                    is Session.Anonymous, Session.SignedOut -> {
                        Text("Sign in with Google to register for the protest at Jantar Mantar.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Dimens.Space12))
                        CjpButton("Sign in with Google", onClick = { sessionViewModel.signInWithGoogle(context) }, gold = true, modifier = Modifier.fillMaxWidth())
                    }
                    Session.Loading -> LoadingBox()
                }
            }
            CjpOutlinedButton("Faced police harassment? Report evidence", onClick = onReportPolice, modifier = Modifier.fillMaxWidth())
        }

        Text(
            "This is a peaceful civic mobilisation organised by supporters of the Cockroach Janta Party movement. Participation is voluntary. Follow local laws and police directions at all times.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Dimens.Space16)
        )
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Gold)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Paper)
    }
}

@Composable
private fun RegisterForm(
    profile: UserProfile,
    state: ProtestFormState,
    onSubmit: (UserProfile, String, String, JoinMode?) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var city by remember(profile.city) { mutableStateOf(profile.city) }
    var mode by remember { mutableStateOf<JoinMode?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
        Text("${Campaign.VENUE}\nTiming: ${Campaign.TIMING}", style = MaterialTheme.typography.bodyMedium)
        if (profile.phone.isNotBlank()) {
            Text("We have your phone (${profile.phone}) on file. Confirm how you will join the protest.", style = MaterialTheme.typography.bodyMedium)
        } else {
            OutlinedTextField(phone, { phone = it }, label = { Text("WhatsApp number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(city, { city = it }, label = { Text("City (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("HOW WILL YOU JOIN?", style = MaterialTheme.typography.labelLarge)
        JoinMode.entries.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == option, onClick = { mode = option })
                Text(option.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        CjpButton("Register", onClick = { onSubmit(profile, phone, city, mode) }, enabled = !state.submitting, modifier = Modifier.fillMaxWidth())
    }
}
