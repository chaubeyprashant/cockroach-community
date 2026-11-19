package com.cockroachcommunity.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.Gold

private val HardBorder = 2.dp

@Composable
fun CjpCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(HardBorder, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(Dimens.Space16), content = content)
    }
}

@Composable
fun CjpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gold: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Dimens.TouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = if (gold) ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1108))
        else ButtonDefaults.buttonColors(),
        border = BorderStroke(HardBorder, MaterialTheme.colorScheme.outline)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CjpOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Dimens.TouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(HardBorder, MaterialTheme.colorScheme.outline)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text.uppercase(), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Dimens.Space4))
        Box(
            Modifier
                .width(48.dp)
                .height(4.dp)
                .background(Gold)
        )
    }
}

@Composable
fun CjpChip(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text.uppercase(),
            modifier = Modifier.padding(horizontal = Dimens.Space8, vertical = Dimens.Space2),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.Space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.Space8))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(Dimens.Space32), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
    }
}
