package com.cockroachcommunity.app.feature.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.data.complaints.Complaint
import com.cockroachcommunity.app.data.complaints.ComplaintsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class ComplaintSort(val label: String) { LATEST("Latest"), SUPPORTED("Most supported"), URGENT("Most urgent") }

data class ComplaintFilters(
    val query: String = "",
    val state: String? = null,
    val city: String? = null,
    val category: String? = null,
    val status: String? = null,
    val urgency: String? = null,
    val sort: ComplaintSort = ComplaintSort.LATEST
)

data class ComplaintListState(
    val loading: Boolean = true,
    val items: List<Complaint> = emptyList(),
    val states: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val filters: ComplaintFilters = ComplaintFilters()
)

private fun urgencyRank(u: String) = when (u) { "emergency" -> 4; "high" -> 3; "medium" -> 2; else -> 1 }

@HiltViewModel
class ComplaintListViewModel @Inject constructor(repository: ComplaintsRepository) : ViewModel() {

    private val filters = MutableStateFlow(ComplaintFilters())

    val state: StateFlow<ComplaintListState> = combine(repository.observeFeed(), filters) { all, f ->
        val states = all.map { it.state }.filter { it.isNotBlank() }.distinct().sorted()
        val cities = all.filter { f.state == null || it.state == f.state }.map { it.city }.filter { it.isNotBlank() }.distinct().sorted()
        val q = f.query.trim().lowercase()
        val filtered = all.filter { c ->
            (q.isEmpty() || "${c.title} ${c.description}".lowercase().contains(q)) &&
                (f.state == null || c.state == f.state) &&
                (f.city == null || c.city == f.city) &&
                (f.category == null || c.issueType == f.category) &&
                (f.status == null || c.status == f.status) &&
                (f.urgency == null || c.urgency == f.urgency)
        }
        val sorted = when (f.sort) {
            ComplaintSort.LATEST -> filtered.sortedByDescending { it.createdAt }
            ComplaintSort.SUPPORTED -> filtered.sortedByDescending { it.supportCount }
            ComplaintSort.URGENT -> filtered.sortedWith(compareByDescending<Complaint> { urgencyRank(it.urgency) }.thenByDescending { it.createdAt })
        }
        ComplaintListState(false, sorted, states, cities, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComplaintListState())

    fun update(block: (ComplaintFilters) -> ComplaintFilters) = filters.update(block)
}
