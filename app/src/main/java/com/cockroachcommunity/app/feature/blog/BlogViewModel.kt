package com.cockroachcommunity.app.feature.blog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.data.articles.Article
import com.cockroachcommunity.app.data.articles.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BlogState {
    data object Loading : BlogState
    data class List(val articles: kotlin.collections.List<Article>) : BlogState
}

@HiltViewModel
class BlogViewModel @Inject constructor(private val repository: ArticlesRepository) : ViewModel() {
    private val _state = MutableStateFlow<BlogState>(BlogState.Loading)
    val state: StateFlow<BlogState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = BlogState.List(repository.list()) }
    }
}

sealed interface ArticleState {
    data object Loading : ArticleState
    data object NotFound : ArticleState
    data class Loaded(val article: Article) : ArticleState
}

@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow<ArticleState>(ArticleState.Loading)
    val state: StateFlow<ArticleState> = _state.asStateFlow()

    init {
        val id: String = checkNotNull(savedStateHandle["id"])
        viewModelScope.launch {
            val article = repository.get(id)
            _state.value = if (article == null) ArticleState.NotFound else ArticleState.Loaded(article)
        }
    }
}
