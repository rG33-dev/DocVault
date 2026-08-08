package com.example.docvault.data.repository

import com.example.docvault.data.local.HistoryDao
import com.example.docvault.domain.model.HistoryLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing application activity history.
 *
 * Provides access to the stream of all history logs and methods to add or clear them.
 *
 * @property historyDao The DAO for history-related database operations.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    /**
     * A [Flow] of all history logs, ordered by timestamp.
     */
    val allHistory: Flow<List<HistoryLog>> = historyDao.getAllHistory()

    /**
     * Adds a new entry to the activity history.
     *
     * @param log The log entry to add.
     */
    suspend fun addLog(log: HistoryLog) {
        historyDao.insertLog(log)
    }

    /**
     * Clears all entries from the activity history.
     */
    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
