package com.example.docvault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.docvault.domain.model.HistoryLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [HistoryLog] entity.
 *
 * Provides methods to record and retrieve activity logs from the database.
 */
@Dao
interface HistoryDao {
    /**
     * Retrieves all history logs, ordered by timestamp (newest first).
     *
     * @return A [Flow] of history log lists.
     */
    @Query("SELECT * FROM history_logs ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryLog>>

    /**
     * Inserts a new history log entry.
     *
     * @param log The history log to record.
     */
    @Insert
    suspend fun insertLog(log: HistoryLog)

    /**
     * Clears all history log entries from the database.
     */
    @Query("DELETE FROM history_logs")
    suspend fun clearHistory()
}
