package com.humbleSolutions.cardealer2.screens.customer

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    navController: NavController,
) {
    CustomerFormScreen(
        mode = CustomerFormMode.Add,
        navController = navController
    )
}