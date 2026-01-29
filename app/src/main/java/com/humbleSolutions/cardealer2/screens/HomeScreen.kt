package com.humbleSolutions.cardealer2.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.humbleSolutions.cardealer2.ViewModel.HomeScreenViewModel
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import com.humbleSolutions.cardealer2.utils.TranslationDictionary
import com.humbleSolutions.cardealer2.repository.CompanyRepository
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = viewModel(),
) {
    val brands by viewModel.brands.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val filter by viewModel.filteredBrands.collectAsState()
    val currentRoute by navController.currentBackStackEntryAsState()
    
    // 🔹 Drawer state from ViewModel
    val drawerStateValue by viewModel.drawerState.collectAsState()
    val canOpenDrawer by viewModel.canOpenDrawer.collectAsState()
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val drawerState = rememberDrawerState(initialValue = drawerStateValue)
    val scope = rememberCoroutineScope()
    
    // 🔹 Sync ViewModel drawer state with DrawerState
    LaunchedEffect(drawerStateValue) {
        when (drawerStateValue) {
            DrawerValue.Open -> drawerState.open()
            DrawerValue.Closed -> drawerState.close()
        }
    }
    
    // 🔹 Sync DrawerState changes back to ViewModel
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue != drawerStateValue) {
            when (drawerState.currentValue) {
                DrawerValue.Open -> viewModel.openDrawer()
                DrawerValue.Closed -> viewModel.closeDrawer()
            }
        }
    }
    
    // 🔹 Reset drawer state when returning to this screen
    LaunchedEffect(currentRoute?.id) {
        if (currentRoute?.destination?.route == "home") {
            viewModel.resetDrawerState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Use explicit Color.Black instead of scrim to prevent black screen issues
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(280.dp)
            ) {
                DrawerContent(
                    onAddCustomerClick = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("add_customer") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onViewCustomersClick = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("view_customer") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onViewBrokersClick = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("view_broker") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onPurchaseVehicleClick = {
                        scope.launch {
                            viewModel.closeDrawer()
                            navController.navigate("purchase_vehicle") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCatalogSelection = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("catalog_selection") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onEmiScheduleClick = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("emi_due") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onAllTransactionsClick = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("all_transactions") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onSettingsClick = {
                        scope.launch { 
                            viewModel.closeDrawer()
                            navController.navigate("settings") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCloseDrawer = { 
                        scope.launch { viewModel.closeDrawer() }
                    }
                )

            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(50.dp))

                // Header with Menu Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { 
                                viewModel.openDrawer()
                            },
                            modifier = Modifier
                                .shadow(4.dp, CircleShape)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = TranslationManager.translate("Menu", isPunjabiEnabled),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val company by CompanyRepository.company.collectAsState()
                        Text(
                            text = company.name.ifEmpty { "Car Dealer" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TranslatedText(
                            englishText = "Find your perfect vehicle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.size(48.dp)) // Balance the menu button
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Search Bar
                EnhancedSearchBar(
                    onSearch =  { query -> viewModel.filterBrands(query) }

                )

                Spacer(modifier = Modifier.height(24.dp))

                // Content Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TranslatedText(
                        englishText = "Popular Brands",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (filter.isNotEmpty()) {
                        val brandsText = if (isPunjabiEnabled) {
                            "${filter.size} ${TranslationDictionary.translate("brands")}"
                        } else {
                            "${filter.size} brands"
                        }
                        Text(
                            text = brandsText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Area
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TranslatedText(
                                    englishText = "Loading brands...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "⚠️", style = MaterialTheme.typography.displaySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                TranslatedText(
                                    englishText = "Something went wrong",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = error!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                    filter.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TranslatedText(
                                    englishText = "No brands found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TranslatedText(
                                    englishText = if (brands.isEmpty()) "No brands available" else "Try adjusting your search",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filter) { brand ->
                                EnhancedBrandCard(
                                    imageUrl = brand.logo,
                                    brandName = brand.brandId,
                                    totalVehicles = brand.vehicle.sumOf { it.quantity },
                                    onClick = {
                                        navController.navigate("brand_selected/${brand.brandId}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    onAddCustomerClick: () -> Unit,
    onViewCustomersClick: ()->Unit,
    onViewBrokersClick: () -> Unit,
    onPurchaseVehicleClick: () -> Unit,
    onCatalogSelection:()->Unit,
    onEmiScheduleClick: () -> Unit,
    onAllTransactionsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    val company by CompanyRepository.company.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
    ) {
        // Drawer Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = company.name.ifEmpty { "Car Dealer" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TranslatedText(
                        englishText = "Management System",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(8.dp))

        // Menu Items
        DrawerMenuItem(
            icon = Icons.Default.PersonAdd,
            title = "Add Customer",
            onClick = onAddCustomerClick,
            isPunjabiEnabled = isPunjabiEnabled
        )

        DrawerMenuItem(
            icon = Icons.Default.People,
            title = "View Customers",
            onClick = {onViewCustomersClick() },
            isPunjabiEnabled = isPunjabiEnabled
        )

        DrawerMenuItem(
            icon = Icons.Default.Business,
            title = "Brokers",
            onClick = { onViewBrokersClick() },
            isPunjabiEnabled = isPunjabiEnabled
        )

        DrawerMenuItem(
            icon = Icons.Default.ShoppingCart,
            title = "Purchase Vehicle",
            onClick = { onPurchaseVehicleClick() },
            isPunjabiEnabled = isPunjabiEnabled
        )



        DrawerMenuItem(
            icon = Icons.Default.PictureAsPdf,
            title = "Generate Catalog",
            onClick = {
                onCatalogSelection()
            },
            isPunjabiEnabled = isPunjabiEnabled
        )

        DrawerMenuItem(
            icon = Icons.Default.Payments,
            title = "EMI Schedule",
            onClick = { onEmiScheduleClick() },
            isPunjabiEnabled = isPunjabiEnabled
        )
        
        DrawerMenuItem(
            icon = Icons.Default.Receipt,
            title = "All Transactions",
            onClick = { onAllTransactionsClick() },
            isPunjabiEnabled = isPunjabiEnabled
        )
        
    /*    DrawerMenuItem(
            icon = Icons.Default.Assessment,
            title = "Reports",
            onClick = { /* Navigate to reports */ }
            )
     */


        Spacer(Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(8.dp))

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = { onSettingsClick() },
            isPunjabiEnabled = isPunjabiEnabled
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    isPunjabiEnabled: Boolean = false
) {
    val translatedTitle = remember(title, isPunjabiEnabled) {
        TranslationManager.translate(title, isPunjabiEnabled)
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = translatedTitle,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = translatedTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EnhancedSearchBar(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onSearch(it)
        },
        placeholder = {
            TranslatedText(
                englishText = "Search brands...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = TranslationManager.translate("Search", isPunjabiEnabled),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
@Composable
fun EnhancedBrandCard(
    imageUrl: String,
    brandName: String,
    totalVehicles: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = brandName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = brandName.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    val vehiclesText = if (isPunjabiEnabled) {
                        "$totalVehicles ${TranslationDictionary.translate("vehicles")}"
                    } else {
                        "$totalVehicles vehicles"
                    }
                    Text(
                        text = vehiclesText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}