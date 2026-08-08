package com.example.docvault.ui.history

import com.example.docvault.domain.model.HistoryLog

/**
 * UI State for the Activity History screen.
 *
 * @property logs The list of all activity logs, sorted chronologically.
 * @property isLoading Whether the history is currently being loaded from the database.
 */
data class HistoryUiState(
    val logs: List<HistoryLog> = emptyList(),
    val isLoading: Boolean = false
)
