package com.example.docvault.di

import android.content.Context
import androidx.room.Room
import com.example.docvault.data.local.AppDatabase
import com.example.docvault.data.local.DocumentDao
import com.example.docvault.data.local.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton instance of the [AppDatabase].
     *
     * @param context The application context.
     * @return The app database instance.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    /** Provides the [DocumentDao] from the database. */
    @Provides
    @Singleton
    fun provideDocumentDao(db: AppDatabase): DocumentDao = db.documentDao

    /** Provides the [HistoryDao] from the database. */
    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao
}
