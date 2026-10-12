package com.cockroachcommunity.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cockroachcommunity.app.core.designsystem.theme.BrandAccent
import com.cockroachcommunity.app.core.designsystem.theme.BrandPrimary
import com.cockroachcommunity.app.core.designsystem.theme.Dimens

val SOCIAL_REACTION_EMOJIS = listOf("❤️", "🔥", "😂", "👍", "🎉", "⚡")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    text: String,
    isMine: Boolean,
    timestamp: String,
    modifier: Modifier = Modifier,
    status: String? = null, // Sent, Delivered, Read, Failed
    reaction: String? = null,
    onReact: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    val shape = if (isMine) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = modifier.padding(vertical = Dimens.Space4),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Floating animated emoji reaction bar
        AnimatedVisibility(
            visible = showReactionPicker,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SOCIAL_REACTION_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable {
                                    onReact?.invoke(emoji)
                                    showReactionPicker = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        Box {
            if (isMine) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandPrimary, BrandAccent)
                            )
                        )
                        .combinedClickable(
                            onClick = { showReactionPicker = !showReactionPicker },
                            onLongClick = { showReactionPicker = !showReactionPicker }
                        )
                        .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .combinedClickable(
                            onClick = { showReactionPicker = !showReactionPicker },
                            onLongClick = { showReactionPicker = !showReactionPicker }
                        ),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
                    )
                }
            }

            // Attached reaction emoji badge
            if (!reaction.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .align(if (isMine) Alignment.BottomStart else Alignment.BottomEnd)
                        .offset(x = if (isMine) (-4).dp else 4.dp, y = 10.dp)
                ) {
                    Text(
                        text = reaction,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.padding(
                top = if (reaction.isNullOrBlank()) Dimens.Space4 else 12.dp, 
                start = Dimens.Space4, 
                end = Dimens.Space4
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            if (isMine && status != null) {
                Spacer(modifier = Modifier.width(Dimens.Space4))
                if (status == "Failed" && onRetry != null) {
                    Text(
                        text = "• Failed (Tap to Retry)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onRetry() }
                    )
                } else {
                    Text(
                        text = "• $status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
