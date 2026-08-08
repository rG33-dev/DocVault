package com.example.docvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.HistoryLog

/**
 * The Room database for the application.
 *
 * It stores metadata for documents and activity logs.
 * File content itself is stored separately in encrypted files.
 */
@Database(
    entities = [Document::class, HistoryLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** Accessor for document-related database operations. */
    abstract val documentDao: DocumentDao
    /** Accessor for history-related database operations. */
    abstract val historyDao: HistoryDao

    companion object {
        /** The name of the database file. */
        const val DATABASE_NAME = "docvault_db"
    }
}
