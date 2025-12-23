package com.example.cardealer2.screens.customer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerDetails(
    navController: NavController,
    customerId: String
) {
    CustomerFormScreen(
        mode = CustomerFormMode.Edit(customerId = customerId),
        navController = navController
    )
}