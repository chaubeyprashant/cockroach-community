package com.cockroachcommunity.app.feature.blog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cockroachcommunity.app.core.designsystem.components.*
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.Red
import com.cockroachcommunity.app.data.articles.Article

@Composable
fun BlogScreen(onOpen: (String) -> Unit, viewModel: BlogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        BlogState.Loading -> LoadingBox()
        is BlogState.List -> LazyColumn(
            contentPadding = PaddingValues(Dimens.Space16),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
        ) {
            item {
                SectionTitle("Party Dispatches")
                Spacer(Modifier.height(Dimens.Space8))
                Text(
                    "News, manifesto explainers, and protest updates from the Cockroach Janta Party.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            items(s.articles, key = { it.id }) { article -> ArticleCard(article, onClick = { onOpen(article.id) }) }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column {
            if (article.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
            Column(Modifier.padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                Text(formatDate(article.publishedAt).uppercase(), style = MaterialTheme.typography.labelMedium, color = Red)
                Text(article.title, style = MaterialTheme.typography.titleLarge)
                Text(article.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                if (article.authorName.isNotBlank()) Text("By ${article.authorName}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ArticleDetailScreen(viewModel: ArticleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        ArticleState.Loading -> LoadingBox()
        ArticleState.NotFound -> EmptyState("Article not found", "This dispatch is no longer available.")
        is ArticleState.Loaded -> {
            val a = s.article
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (a.imageUrl.isNotBlank()) {
                    AsyncImage(a.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(220.dp))
                }
                Column(Modifier.padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                    Text(formatDate(a.publishedAt).uppercase(), style = MaterialTheme.typography.labelLarge, color = Red)
                    Text(a.title, style = MaterialTheme.typography.headlineLarge)
                    if (a.authorName.isNotBlank()) Text("By ${a.authorName}", style = MaterialTheme.typography.titleSmall)
                    MiniMarkdown(a.content)
                }
            }
        }
    }
}
