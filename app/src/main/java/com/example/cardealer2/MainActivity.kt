package com.example.cardealer2

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
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
import com.example.cardealer2.screens.catalog.CatalogSelectionScreen
import com.example.cardealer2.screens.catalog.PdfViewerScreen
import com.example.cardealer2.screens.PurchaseVehicleScreen
import com.example.cardealer2.screens.SellVehicleScreen
import com.example.cardealer2.screens.EmiDueScreen
import com.example.cardealer2.screens.transactions.AllTransactionsScreen
import com.example.cardealer2.screens.transactions.TransactionDetailScreen
import com.example.cardealer2.screens.broker.ViewBrokerScreen
import com.example.cardealer2.screens.broker.BrokerDetailsScreen
import com.example.cardealer2.screens.broker.EditBrokerDetails
import com.example.cardealer2.ViewModel.ViewBrokersViewModel
import com.example.cardealer2.ViewModel.SettingsViewModel
import com.example.cardealer2.screens.SplashScreen
import com.example.cardealer2.screens.SettingsScreen
import com.example.cardealer2.ui.theme.CarDealer2Theme
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Apply your app theme here
            CarDealer2Theme { // <-- Replace with your actual theme name

                    AppNav()

            }
        }
    }
}


@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash Screen
        composable("splash") {
            val settingsViewModel: SettingsViewModel = viewModel()
            SplashScreen(
                navController = navController,
                viewModel = settingsViewModel
            )
        }
        
        composable("home") {
            val homeViewModel: HomeScreenViewModel = viewModel()
            HomeScreen(navController = navController, viewModel = homeViewModel)
        }

        composable(
            route = "brand_selected/{brandId}",
            arguments = listOf(navArgument("brandId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brandId = backStackEntry.arguments?.getString("brandId") ?: ""

            // ✅ Get the same ViewModel scoped to "home" when available; safely fall back otherwise
            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("home") }.getOrNull()
            }
            val homeViewModel: HomeScreenViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[HomeScreenViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }


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

            // Create a ViewModel scoped to this screen
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

            // ✅ Try to scope to the brand vehicles graph when available; safely fall back otherwise
            val parentEntry = remember(backStackEntry) {
                runCatching {
                    navController.getBackStackEntry("brand_Vehicle/{productId}/{brandId}")
                }.getOrNull()
            }
            val brandVehiclesViewModel: BrandVehicleViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[BrandVehicleViewModel::class.java]
            } else {
                // Fallback to a local instance to avoid crashes/blank screens when deep linked
                viewModel(backStackEntry)
            }
            val viewModel: VehicleDetailViewModel = viewModel(backStackEntry)

            VehicleDetailScreen(
                chassisNumber = chassisNumber,
                navController = navController,
                viewModel =viewModel,
                brandVehiclesViewModel = brandVehiclesViewModel
            )
        }


        composable(
            "edit_vehicle/{chassisNumber}",
            arguments = listOf(navArgument("chassisNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val chassisNumber = backStackEntry.arguments?.getString("chassisNumber") ?: ""

            // Try to scope to vehicle_detail when available; fallback to local scope otherwise
            val parentEntry = runCatching {
                navController.getBackStackEntry("vehicle_detail/$chassisNumber")
            }.getOrNull()
            val detailVm: VehicleDetailViewModel = if (parentEntry != null) {
                viewModel(parentEntry)
            } else {
                viewModel(backStackEntry)
            }

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

            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("home") }.getOrNull()
            }
            val homeScreenViewModel: HomeScreenViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[HomeScreenViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }

            AddBrandVehicleScreen(brandId = brandId, navController = navController,homeScreenViewModel=homeScreenViewModel,)

        }
        composable("add_customer") {backStackEntry->
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

            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("view_customer") }.getOrNull()
            }
            val customersVm: ViewCustomersViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[ViewCustomersViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }

            CustomerDetailsScreen(
                navController = navController,
                customerId = customerId,
                viewModel = customersVm
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

        composable("catalog_selection") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("home") }.getOrNull()
            }
            val homeScreenViewModel: HomeScreenViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[HomeScreenViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }

            CatalogSelectionScreen(
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
                val decoded = URLDecoder.decode(pdfPath, "UTF-8")
                // Extract catalog ID from query parameter if present
                val parts = decoded.split("?catalogId=")
                if (parts.size == 2) {
                    parts[0].replace("%2F", "/").replace("%3A", ":")
                } else {
                    decoded.replace("%2F", "/").replace("%3A", ":")
                }
            } catch (e: Exception) {
                pdfPath
            }
            
            // Extract catalog ID
            val catalogId = try {
                val decoded = URLDecoder.decode(pdfPath, "UTF-8")
                val parts = decoded.split("?catalogId=")
                if (parts.size == 2) {
                    URLDecoder.decode(parts[1], "UTF-8")
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            PdfViewerScreen(
                pdfFilePath = decodedPath,
                navController = navController,
                catalogId = catalogId
            )
        }

        // Purchase Vehicle Screen
        composable("purchase_vehicle") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("home") }.getOrNull()
            }
            val homeScreenViewModel: HomeScreenViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[HomeScreenViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }
            
            PurchaseVehicleScreen(
                navController = navController,
                homeScreenViewModel = homeScreenViewModel
            )
        }

        // Sell Vehicle Screen
        composable(
            route = "sell_vehicle/{chassisNumber}",
            arguments = listOf(navArgument("chassisNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val chassisNumber = backStackEntry.arguments?.getString("chassisNumber") ?: ""
            
            SellVehicleScreen(
                chassisNumber = chassisNumber,
                navController = navController
            )
        }

        // View Broker Screen
        composable("view_broker") {
            ViewBrokerScreen(
                navController = navController
            )
        }

        // Broker Detail Screen
        composable(
            route = "broker_detail/{brokerId}",
            arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brokerId = backStackEntry.arguments?.getString("brokerId") ?: ""
            
            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("view_broker") }.getOrNull()
            }
            val brokersVm: ViewBrokersViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[ViewBrokersViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }
            
            BrokerDetailsScreen(
                navController = navController,
                brokerId = brokerId,
                viewModel = brokersVm
            )
        }

        // Edit Broker Screen
        composable(
            route = "edit_broker/{brokerId}",
            arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brokerId = backStackEntry.arguments?.getString("brokerId") ?: ""
            
            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry("broker_detail/$brokerId") }.getOrNull()
            }
            val brokersVm: ViewBrokersViewModel = if (parentEntry != null) {
                ViewModelProvider(parentEntry)[ViewBrokersViewModel::class.java]
            } else {
                viewModel(backStackEntry)
            }
            
            EditBrokerDetails(
                navController = navController,
                brokerId = brokerId,
                viewModel = brokersVm
            )
        }

        // EMI Due Screen
        composable("emi_due") {
            EmiDueScreen(
                navController = navController
            )
        }

        // All Transactions Screen
        composable("all_transactions") {
            AllTransactionsScreen(
                navController = navController
            )
        }

        // Transaction Detail Screen
        composable(
            route = "transaction_detail/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            TransactionDetailScreen(
                navController = navController,
                transactionId = transactionId
            )
        }

        // Settings Screen
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                navController = navController,
                viewModel = settingsViewModel
            )
        }

    }
}

