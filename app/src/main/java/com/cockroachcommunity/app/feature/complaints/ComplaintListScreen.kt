package com.cockroachcommunity.app.feature.complaints

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.*
import com.cockroachcommunity.app.data.complaints.*

@Composable
fun ComplaintListScreen(
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    viewModel: ComplaintListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val f = state.filters

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = Dimens.Space16, end = Dimens.Space16, top = Dimens.Space16, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
        ) {
            item {
                SectionTitle("Public Complaints")
                Spacer(Modifier.height(Dimens.Space12))
                OutlinedTextField(
                    value = f.query,
                    onValueChange = { v -> viewModel.update { it.copy(query = v) } },
                    label = { Text("Search complaints") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Dimens.Space8))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                    FilterDropdown("State", f.state, state.states) { v -> viewModel.update { it.copy(state = v, city = null) } }
                    FilterDropdown("City", f.city, state.cities) { v -> viewModel.update { it.copy(city = v) } }
                    FilterDropdown("Category", f.category, IssueTypes) { v -> viewModel.update { it.copy(category = v) } }
                    FilterDropdown("Status", f.status, ComplaintStatuses, labeler = ::statusLabel) { v -> viewModel.update { it.copy(status = v) } }
                    FilterDropdown("Urgency", f.urgency, UrgencyLevels, labeler = { it.uppercase() }) { v -> viewModel.update { it.copy(urgency = v) } }
                }
                Spacer(Modifier.height(Dimens.Space8))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                    ComplaintSort.entries.forEach { sort ->
                        FilterChip(
                            selected = f.sort == sort,
                            onClick = { viewModel.update { it.copy(sort = sort) } },
                            label = { Text(sort.label.uppercase(), style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
            when {
                state.loading -> item { LoadingBox() }
                state.items.isEmpty() -> item { EmptyState("No complaints found", "Try clearing a filter, or be the first to file one.") }
                else -> items(state.items, key = { it.id }) { c -> ComplaintCard(c, onClick = { onOpen(c.id) }) }
            }
        }
        FloatingActionButton(
            onClick = onNew,
            containerColor = Gold,
            contentColor = Ink,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.Space16)
        ) {
            Text("FILE COMPLAINT", Modifier.padding(horizontal = Dimens.Space16), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selected: String?,
    options: List<String>,
    labeler: (String) -> String = { it },
    onSelect: (String?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text((selected?.let(labeler) ?: label).uppercase(), style = MaterialTheme.typography.labelMedium) },
            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline)
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("All") }, onClick = { onSelect(null); open = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(labeler(option)) }, onClick = { onSelect(option); open = false })
            }
        }
    }
}

@Composable
fun urgencyColor(urgency: String): Color = when (urgency) {
    "emergency" -> Red
    "high" -> Orange
    "medium" -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun statusColor(status: String): Color = when (status) {
    "resolved", "verified" -> Green
    "escalated", "under_review" -> Orange
    "rejected" -> Red
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun ComplaintCard(c: Complaint, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CjpChip(c.issueType)
                CjpChip(statusLabel(c.status), color = statusColor(c.status))
                CjpChip(c.urgency, color = urgencyColor(c.urgency))
            }
            Text(c.title, style = MaterialTheme.typography.titleLarge)
            Text(c.description.take(180) + if (c.description.length > 180) "…" else "", style = MaterialTheme.typography.bodyMedium)
            val place = listOf(c.locality, c.city, c.state).filter { it.isNotBlank() && it != "Not specified" }.joinToString(", ")
            if (place.isNotBlank()) Text("📍 $place", style = MaterialTheme.typography.bodySmall)
            Text(
                "${c.supportCount} supports · ${c.commentCount} comments · ${c.referenceId}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
