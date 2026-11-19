package com.cockroachcommunity.app.feature.complaints

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.*
import com.cockroachcommunity.app.data.complaints.*
import com.cockroachcommunity.app.feature.session.RequireMember

private const val SITE = "https://cockroachjanthaparty.com"

@Composable
fun ComplaintDetailScreen(viewModel: ComplaintDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = state.complaint

    when {
        state.loading -> LoadingBox()
        c == null -> EmptyState("Complaint not found", "It may have been removed.")
        else -> {
            var flagging by remember { mutableStateOf(false) }
            var flagReason by remember { mutableStateOf("") }
            var comment by remember { mutableStateOf("") }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    CjpChip(c.issueType)
                    CjpChip(statusLabel(c.status), color = statusColor(c.status))
                    CjpChip(c.urgency, color = urgencyColor(c.urgency))
                }
                Text(c.title, style = MaterialTheme.typography.headlineMedium)
                Text(c.referenceId, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(c.description, style = MaterialTheme.typography.bodyLarge)

                val place = listOf(c.address, c.locality, c.city, c.state, c.pincode).filter { it.isNotBlank() && it != "Not specified" && it != "000000" }.joinToString(", ")
                if (place.isNotBlank()) Text("📍 $place", style = MaterialTheme.typography.bodyMedium)
                if (c.lat != null && c.lng != null) {
                    CjpOutlinedButton("Open in Maps", onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${c.lat},${c.lng}?q=${c.lat},${c.lng}(${Uri.encode(c.title)})")))
                    })
                }
                Text("Reported by ${c.reporterName} · ${formatDate(c.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (c.media.isNotEmpty()) {
                    SectionTitle("Evidence")
                    c.media.forEach { m ->
                        if (m.isImage) {
                            AsyncImage(m.url, m.name, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                        } else {
                            CjpOutlinedButton(
                                (if (m.isVideo) "▶ Play video: " else "Open file: ") + m.name,
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.url))) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                RequireMember("Sign in with Google to support, comment on or report complaints.") { _ ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        CjpButton(
                            (if (state.supported) "Withdraw support" else "Support") + " · ${c.supportCount}",
                            onClick = { viewModel.toggleSupport(state.supported) },
                            gold = !state.supported,
                            modifier = Modifier.weight(1f)
                        )
                        CjpOutlinedButton("Report fake", onClick = { flagging = true })
                    }
                }
                CjpOutlinedButton("Share", onClick = {
                    val text = "${c.title} — ${c.referenceId}\n$SITE/complaints/${c.id}"
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share complaint"))
                }, modifier = Modifier.fillMaxWidth())

                SectionTitle("Status timeline")
                state.history.forEach { h ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                        Text("●", color = statusColor(h.status))
                        Column {
                            Text(statusLabel(h.status), style = MaterialTheme.typography.titleSmall)
                            if (h.note.isNotBlank()) Text(h.note, style = MaterialTheme.typography.bodyMedium)
                            Text(formatDate(h.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                SectionTitle("Comments (${state.comments.size})")
                state.comments.forEach { cm ->
                    Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.padding(Dimens.Space12).fillMaxWidth()) {
                            Text(cm.authorName.uppercase(), style = MaterialTheme.typography.labelLarge)
                            Text(cm.body, style = MaterialTheme.typography.bodyMedium)
                            Text(formatDate(cm.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                RequireMember("Sign in with Google to join the discussion.") { _ ->
                    OutlinedTextField(comment, { comment = it }, label = { Text("Add a comment") }, modifier = Modifier.fillMaxWidth())
                    CjpButton("Post comment", onClick = { viewModel.comment(comment); comment = "" }, enabled = comment.isNotBlank(), modifier = Modifier.fillMaxWidth())
                }
                state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            if (flagging) {
                AlertDialog(
                    onDismissRequest = { flagging = false },
                    title = { Text("REPORT FAKE COMPLAINT") },
                    text = { OutlinedTextField(flagReason, { flagReason = it }, label = { Text("Why? (4–240 characters)") }) },
                    confirmButton = {
                        TextButton(enabled = flagReason.trim().length in 4..240, onClick = { viewModel.reportFake(flagReason.trim()); flagging = false; flagReason = "" }) { Text("SEND") }
                    },
                    dismissButton = { TextButton(onClick = { flagging = false }) { Text("CANCEL") } }
                )
            }
        }
    }
}
