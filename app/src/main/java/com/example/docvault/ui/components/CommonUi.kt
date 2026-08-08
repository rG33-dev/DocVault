package com.example.docvault.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A full-screen loading component with a smooth animation.
 */
@Composable
fun LoadingScreen(
    message: String = "Securing your vault..."
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
