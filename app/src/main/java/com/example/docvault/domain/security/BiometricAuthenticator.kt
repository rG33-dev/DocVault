package com.example.docvault.domain.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles biometric authentication logic for the application.
 *
 * This class provides methods to check for biometric availability and to trigger
 * the biometric authentication prompt.
 *
 * @property context The application context.
 */
@Singleton
class BiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Checks if biometric authentication is available on the device.
     *
     * @return True if biometrics can be used, false otherwise.
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Triggers the biometric authentication prompt.
     *
     * @param activity The [FragmentActivity] used to host the biometric prompt.
     * @param onResult Callback function that receives a boolean indicating success or failure.
     */
    fun authenticate(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(false)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("DocVault Lock")
            .setSubtitle("Authenticate to access your vault")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
