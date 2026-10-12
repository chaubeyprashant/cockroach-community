package com.cockroachcommunity.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.database.entity.TransferEntity
import com.cockroachcommunity.app.core.network.repository.FileTransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransfersViewModel @Inject constructor(
    private val repository: FileTransferRepository
) : ViewModel() {

    val activeTransfers: StateFlow<List<TransferEntity>> = repository.getActiveTransfers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
