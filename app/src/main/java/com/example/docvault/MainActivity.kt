package com.example.docvault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.docvault.domain.security.BiometricAuthenticator
import com.example.docvault.ui.components.LoadingScreen
import com.example.docvault.ui.detail.DocumentDetailScreen
import com.example.docvault.ui.history.HistoryScreen
import com.example.docvault.ui.theme.DocVaultTheme
import com.example.docvault.ui.vault.VaultScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The main activity of the DocVault application.
 *
 * Implements a premium startup flow with biometric authentication and 
 * high-end UI design inspired by modern dashboard aesthetics.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthenticator: BiometricAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocVaultTheme {
                var authState by remember { mutableStateOf<AuthState>(AuthState.Initial) }
                val isBiometricAvailable = remember { biometricAuthenticator.isBiometricAvailable() }

                LaunchedEffect(Unit) {
                    if (isBiometricAvailable) {
                        authState = AuthState.Authenticating
                        biometricAuthenticator.authenticate(this@MainActivity) { success ->
                            authState = if (success) AuthState.Authenticated else AuthState.Unauthenticated
                        }
                    } else {
                        // For MVP, if no biometric is setup, we allow entry.
                        authState = AuthState.Authenticated
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = authState, label = "auth_transition") { state ->
                        when (state) {
                            AuthState.Initial -> LoadingScreen(message = "Starting Secure Engine...")
                            AuthState.Authenticating -> LoadingScreen(message = "Verifying Identity...")
                            AuthState.Authenticated -> DocVaultAppContent()
                            AuthState.Unauthenticated -> LockScreen(onRetry = {
                                authState = AuthState.Authenticating
                                biometricAuthenticator.authenticate(this@MainActivity) { success ->
                                    authState = if (success) AuthState.Authenticated else AuthState.Unauthenticated
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

sealed class AuthState {
    data object Initial : AuthState()
    data object Authenticating : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
}

@Composable
fun LockScreen(onRetry: () -> Unit) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.evault),
                        contentDescription = "DocVault Logo",
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Vault Locked.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Authentication required to access your encrypted documents.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "UNLOCK NOW",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun DocVaultAppContent() {
    val navController = rememberNavController()
    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "vault",
            modifier = Modifier.padding(padding)
        ) {
            composable("vault") {
                VaultScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate("detail/$id")
                    },
                    onNavigateToHistory = {
                        navController.navigate("history")
                    }
                )
            }
            composable("history") {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "detail/{docId}",
                arguments = listOf(navArgument("docId") { type = NavType.LongType })
            ) {
                DocumentDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
