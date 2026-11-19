package com.cockroachcommunity.app.feature.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.CjpButton
import com.cockroachcommunity.app.core.designsystem.components.CjpCard
import com.cockroachcommunity.app.core.designsystem.components.LoadingBox
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.UserProfile

/** Shows [content] for signed-in members; guests get a sign-in prompt instead. */
@Composable
fun RequireMember(
    reason: String,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
    content: @Composable (UserProfile) -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (val s = session) {
        is Session.Member -> content(s.profile)
        Session.Loading -> LoadingBox(modifier)
        else -> CjpCard(modifier = modifier.fillMaxWidth().padding(Dimens.Space16)) {
            Text("SIGN IN REQUIRED", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.Space8))
            Text(reason, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Dimens.Space16))
            CjpButton("Sign in with Google", onClick = { viewModel.signInWithGoogle(context) }, gold = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
