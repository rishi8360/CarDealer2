package com.example.cardealer2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cardealer2.ViewModel.BrandVehicleViewModel
import com.example.cardealer2.ViewModel.HomeScreenViewModel
import com.example.cardealer2.ViewModel.VehicleDetailViewModel
import com.example.cardealer2.screens.BrandSelected
import com.example.cardealer2.screens.HomeScreen
import com.example.cardealer2.screens.BrandVehicles
import com.example.cardealer2.screens.VehicleDetailScreen
import com.example.cardealer2.screens.AddBrandVehicleScreen
import com.example.cardealer2.screens.customer.AddCustomerScreen
import com.example.cardealer2.screens.EditVehicleScreen
import com.example.cardealer2.screens.customer.ViewCustomerScreen
import com.example.cardealer2.screens.customer.CustomerDetailsScreen
import com.example.cardealer2.screens.customer.EditCustomerDetails
import com.example.cardealer2.ViewModel.ViewCustomersViewModel
import com.example.cardealer2.screens.broker.ViewBrokerScreen
import com.example.cardealer2.screens.broker.BrokerDetailsScreen
import com.example.cardealer2.screens.broker.EditBrokerDetails
import com.example.cardealer2.ViewModel.ViewBrokersViewModel
import com.example.cardealer2.screens.catalog.CatalogSelectionScreen
import com.example.cardealer2.screens.catalog.PdfViewerScreen
import com.example.cardealer2.screens.PurchaseVehicleScreen
import com.example.cardealer2.screens.SellVehicleScreen
import com.example.cardealer2.screens.PaymentsScreen
import com.example.cardealer2.screens.EmiDueScreen
import com.example.cardealer2.ui.theme.CarDealer2Theme
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Apply your app theme here
            CarDealer2Theme { // <-- Replace with your actual theme name

                    AppNav()

            }
        }
    }
}


@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val homeViewModel: HomeScreenViewModel = viewModel()
            HomeScreen(navController = navController, viewModel = homeViewModel)
        }

        composable(
            route = "brand_selected/{brandId}",
            arguments = listOf(navArgument("brandId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brandId = backStackEntry.arguments?.getString("brandId") ?: ""
            val homeViewModel: HomeScreenViewModel = viewModel()

            BrandSelected(
                brandId = brandId,
                navController = navController,
                homeScreenViewModel = homeViewModel
            )
        }


        composable(
            route = "brand_Vehicle/{productId}/{brandId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("brandId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val brandId = backStackEntry.arguments?.getString("brandId") ?: ""
            val brandVehiclesViewModel: BrandVehicleViewModel = viewModel()

            BrandVehicles(
                productId = productId,
                brandName = brandId,
                navController = navController,
                viewModel = brandVehiclesViewModel
            )
        }

        composable(
            route = "vehicle_detail/{chassisNumber}",
            arguments = listOf(
                navArgument("chassisNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chassisNumber = backStackEntry.arguments?.getString("chassisNumber") ?: ""
            val brandVehiclesViewModel: BrandVehicleViewModel = viewModel()
            val viewModel: VehicleDetailViewModel = viewModel()

            VehicleDetailScreen(
                chassisNumber = chassisNumber,
                navController = navController,
                viewModel = viewModel,
                brandVehiclesViewModel = brandVehiclesViewModel
            )
        }


        composable(
            "edit_vehicle/{chassisNumber}",
            arguments = listOf(navArgument("chassisNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val chassisNumber = backStackEntry.arguments?.getString("chassisNumber") ?: ""
            val detailVm: VehicleDetailViewModel = viewModel()

            EditVehicleScreen(
                navController = navController,
                detailViewModel = detailVm,
                chassisNumber = chassisNumber
            )
        }

        composable(
            "add_vehicle/{brandId}",
            arguments = listOf(
                navArgument("brandId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val brandId = backStackEntry.arguments?.getString("brandId") ?: ""
            val homeScreenViewModel: HomeScreenViewModel = viewModel()

            AddBrandVehicleScreen(
                brandId = brandId,
                navController = navController,
                homeScreenViewModel = homeScreenViewModel
            )
        }
        composable("add_customer") {
            AddCustomerScreen(navController = navController)
        }
        composable("view_customer") {
            ViewCustomerScreen(navController = navController)
        }
        composable(
            route = "customer_detail/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val customersVm: ViewCustomersViewModel = viewModel()

            CustomerDetailsScreen(
                navController = navController,
                customerId = customerId,
                viewModel = customersVm
            )
        }

        composable("view_broker") {
            ViewBrokerScreen(navController = navController)
        }
        composable(
            route = "broker_detail/{brokerId}",
            arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brokerId = backStackEntry.arguments?.getString("brokerId") ?: ""
            val brokersVm: ViewBrokersViewModel = viewModel()

            BrokerDetailsScreen(
                navController = navController,
                brokerId = brokerId,
                viewModel = brokersVm
            )
        }

        composable(
            route = "edit_broker/{brokerId}",
            arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brokerId = backStackEntry.arguments?.getString("brokerId") ?: ""
            val brokersVm: ViewBrokersViewModel = viewModel()

            EditBrokerDetails(
                navController = navController,
                brokerId = brokerId,
                viewModel = brokersVm
            )
        }

        composable(
            route = "edit_customer/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            EditCustomerDetails(
                navController = navController,
                customerId = customerId
            )
        }

        composable("catalog_selection") {
            val homeScreenViewModel: HomeScreenViewModel = viewModel()

            CatalogSelectionScreen(
                navController = navController,
                homeScreenViewModel = homeScreenViewModel
            )
        }

        composable("purchase_vehicle") {
            val homeScreenViewModel: HomeScreenViewModel = viewModel()

            PurchaseVehicleScreen(
                navController = navController,
                homeScreenViewModel = homeScreenViewModel
            )
        }

        composable(
            route = "pdf_viewer/{pdfPath}",
            arguments = listOf(
                navArgument("pdfPath") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val pdfPath = backStackEntry.arguments?.getString("pdfPath") ?: ""
            val decodedPath = try {
                URLDecoder.decode(pdfPath, "UTF-8").replace("%2F", "/").replace("%3A", ":")
            } catch (e: Exception) {
                pdfPath
            }

            PdfViewerScreen(
                pdfFilePath = decodedPath,
                navController = navController
            )
        }

        composable(
            route = "sell_vehicle/{chassisNumber}",
            arguments = listOf(
                navArgument("chassisNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chassisNumber = backStackEntry.arguments?.getString("chassisNumber") ?: ""
            
            SellVehicleScreen(
                chassisNumber = chassisNumber,
                navController = navController
            )
        }

        composable("payments") {
            PaymentsScreen(navController = navController)
        }

        composable("emi_due") {
            EmiDueScreen(navController = navController)
        }

    }
}

