package com.example.cardealer2.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val company by viewModel.company.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Animation state
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    // Fetch company data when splash screen loads
    LaunchedEffect(key1 = true) {
        // Fetch company data (loads from cache first, then tries Firestore)
        viewModel.fetchCompanyData(context)
        startAnimation = true
        // Wait for company data to load (or timeout after 2 seconds)
        delay(2000)
        // Navigate to home
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Company Name (shows immediately from cache if available)
                Text(
                    text = company.name.ifEmpty { "Car Dealer" },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(alphaAnim.value)
                        .padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Loading indicator (only shows if actually loading from network)
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

