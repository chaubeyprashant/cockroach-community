package com.cockroachcommunity.app.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.CockroachCommunityTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Dimens.TouchTarget),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Dimens.Space24, vertical = Dimens.Space12)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.IconSmall),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space8))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Dimens.TouchTarget),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Dimens.Space24, vertical = Dimens.Space12)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.IconSmall),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space8))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
fun ButtonsPreview() {
    CockroachCommunityTheme {
        androidx.compose.foundation.layout.Column {
            PrimaryButton(text = "Primary Button", onClick = {})
            Spacer(modifier = Modifier.height(Dimens.Space8))
            PrimaryButton(text = "Loading", isLoading = true, onClick = {})
            Spacer(modifier = Modifier.height(Dimens.Space8))
            SecondaryButton(text = "Secondary Button", onClick = {})
        }
    }
}
