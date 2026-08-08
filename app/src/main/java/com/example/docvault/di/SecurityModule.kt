package com.example.docvault.di

import android.content.Context
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides security-related dependencies.
 *
 * Specifically, it provides the [MasterKey] used for file and shared preferences encryption.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    /**
     * Provides a singleton instance of [MasterKey] using AES256_GCM encryption scheme.
     *
     * @param context The application context.
     * @return The master key instance.
     */
    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
}
