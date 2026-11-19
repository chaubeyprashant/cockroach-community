package com.cockroachcommunity.app.feature.placeholder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cockroachcommunity.app.core.designsystem.components.EmptyState
import com.cockroachcommunity.app.core.designsystem.components.SectionTitle
import com.cockroachcommunity.app.core.designsystem.theme.Dimens

/** Stand-in tab content until each feature phase lands. */
@Composable
fun PlaceholderScreen(title: String) {
    Column(Modifier.fillMaxSize().padding(Dimens.Space16)) {
        SectionTitle(title)
        EmptyState(title = title, message = "Coming in the next build.")
    }
}
