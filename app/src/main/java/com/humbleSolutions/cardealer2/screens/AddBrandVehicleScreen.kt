package com.humbleSolutions.cardealer2.screens


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.humbleSolutions.cardealer2.ViewModel.HomeScreenViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBrandVehicleScreen(
    brandId: String,
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel,
    ) {
    // Load brands when screen appears
    LaunchedEffect(Unit) {
        homeScreenViewModel.loadBrands()
    }
    
    VehicleFormScreen(
        mode = VehicleFormMode.Add(defaultBrandId = brandId),
        navController = navController,
        homeScreenViewModel = homeScreenViewModel
    )
}