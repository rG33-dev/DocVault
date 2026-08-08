package com.example.docvault

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for DocVault.
 *
 * Initialized with [HiltAndroidApp] to enable dependency injection throughout the app.
 */
@HiltAndroidApp
class DocVaultApp : Application()
