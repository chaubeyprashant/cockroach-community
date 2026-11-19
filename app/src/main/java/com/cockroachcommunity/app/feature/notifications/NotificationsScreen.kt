package com.cockroachcommunity.app.feature.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.Gold
import com.cockroachcommunity.app.data.notifications.AppNotification

@Composable
fun NotificationsScreen(
    onOpen: (AppNotification) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val items by viewModel.notifications.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(Dimens.Space16),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Notifications")
                if (unread > 0) CjpOutlinedButton("Mark all read", onClick = viewModel::markAllRead)
            }
        }
        if (items.isEmpty()) {
            item { EmptyState("All quiet", "Complaint updates will show up here.") }
        }
        items(items, key = { it.id }) { n ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (n.read) MaterialTheme.colorScheme.background else Gold.copy(alpha = 0.25f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.markRead(n.id)
                    onOpen(n)
                }
            ) {
                Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
                    Text(n.title.uppercase(), style = MaterialTheme.typography.titleSmall)
                    if (n.body.isNotBlank()) Text(n.body, style = MaterialTheme.typography.bodyMedium)
                    Text(formatDate(n.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
