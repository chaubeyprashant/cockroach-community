package com.cockroachcommunity.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cockroachcommunity.app.core.common.SocialPersonaManager
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.OnlineEmerald
import kotlin.math.abs

@Composable
fun CockroachCommunityAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.AvatarMedium,
    isOnline: Boolean? = null,
    badgeEmoji: String? = null
) {
    // Generate clean 1-2 character initials
    val initials = rememberInitials(name)
    val colorIndex = abs(name.hashCode()) % AvatarGradients.size
    val gradientColors = AvatarGradients[colorIndex]

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = if (size <= Dimens.AvatarSmall) {
                    MaterialTheme.typography.labelSmall
                } else if (size <= Dimens.AvatarMedium) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                }
            )
        }

        // Optional badge emoji (e.g. ⚡, 🎧, 🚀)
        if (!badgeEmoji.isNullOrBlank()) {
            val emojiBadgeSize = if (size <= Dimens.AvatarSmall) 14.dp else 18.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(emojiBadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeEmoji,
                    fontSize = if (size <= Dimens.AvatarSmall) 8.sp else 10.sp
                )
            }
        }

        // Live status indicator dot
        if (isOnline != null) {
            val dotSize = if (size <= Dimens.AvatarSmall) 8.dp else 12.dp
            val statusColor = if (isOnline) OnlineEmerald else Color(0xFF94A3B8)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun rememberInitials(name: String): String {
    val clean = SocialPersonaManager.formatPeerDisplayName(name).trim()
    if (clean.isEmpty()) return "?"
    val parts = clean.split(" ", "-", "_", "·").filter { it.isNotBlank() && it != "·" }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        clean.length >= 2 -> clean.take(2).uppercase()
        else -> clean.uppercase()
    }
}

// Curated modern dual-tone gradients for peer avatars
private val AvatarGradients = listOf(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), // Indigo -> Violet
    listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)), // Cyan -> Sky
    listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), // Pink -> Rose
    listOf(Color(0xFF10B981), Color(0xFF059669)), // Emerald -> Teal
    listOf(Color(0xFFF59E0B), Color(0xFFEA580C)), // Amber -> Orange
    listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)), // Violet -> Fuchsia
    listOf(Color(0xFF0284C7), Color(0xFF4F46E5)), // Blue -> Indigo
    listOf(Color(0xFF14B8A6), Color(0xFF0EA5E9))  // Teal -> Sky
)
