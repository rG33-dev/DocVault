package com.example.docvault.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docvault.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Activity History screen.
 *
 * Provides a reactive stream of history logs and functions to manage them, 
 * such as clearing the entire history.
 *
 * @property repository The [HistoryRepository] for history data access.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    /**
     * UI state containing the list of history logs and loading status.
     */
    val uiState: StateFlow<HistoryUiState> = repository.allHistory
        .map { logs -> HistoryUiState(logs = logs, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState(isLoading = true)
        )

    /**
     * Deletes all history logs from the vault.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
