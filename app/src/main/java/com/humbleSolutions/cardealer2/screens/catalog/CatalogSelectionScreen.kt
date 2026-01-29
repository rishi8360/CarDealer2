package com.humbleSolutions.cardealer2.screens.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.humbleSolutions.cardealer2.ViewModel.CatalogSelectionViewModel
import com.humbleSolutions.cardealer2.ViewModel.FilterState
import com.humbleSolutions.cardealer2.ViewModel.HomeScreenViewModel
import com.humbleSolutions.cardealer2.ViewModel.ProductWithPrice
import com.humbleSolutions.cardealer2.data.Brand
import com.humbleSolutions.cardealer2.data.Product
import com.humbleSolutions.cardealer2.data.VehicleSummary
import com.humbleSolutions.cardealer2.utility.smartPopBack
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSelectionScreen(
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel
) {
    val catalogViewModel: CatalogSelectionViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)

    val brands by homeScreenViewModel.brands.collectAsState()
    val catalogState by catalogViewModel.state.collectAsState()

    var currentView by remember { mutableStateOf<CatalogView>(CatalogView.BrandSelection) }
    var selectedBrandForVehicles by remember { mutableStateOf<Brand?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Load brands when screen appears
    LaunchedEffect(Unit) {
        homeScreenViewModel.loadBrands()
    }

    LaunchedEffect(brands) {
        if (brands.isNotEmpty()) {
            catalogViewModel.loadBrands(brands)
        }
    }

    val totalSelected by catalogViewModel.totalSelectedCount.collectAsState(initial = 0)
    val brandsSelected = catalogViewModel.getSelectedBrandsCount()

    Scaffold(
        topBar = {
            CatalogTopAppBar(
                title = when (currentView) {
                    is CatalogView.BrandSelection -> TranslationManager.translate("Select Brands", isPunjabiEnabled)
                    is CatalogView.VehicleSelection -> TranslationManager.translate("Select Models", isPunjabiEnabled)
                    is CatalogView.IndividualVehicleSelection -> TranslationManager.translate("Select Vehicles", isPunjabiEnabled)
                    is CatalogView.PriceEditing -> TranslationManager.translate("Edit Selling Prices", isPunjabiEnabled)
                    is CatalogView.RecipientNameEntry -> TranslationManager.translate("Recipient Details", isPunjabiEnabled)
                },
                subtitle = when (currentView) {
                    is CatalogView.BrandSelection -> {
                        val brandText = TranslationManager.translate("brand(s)", isPunjabiEnabled)
                        val vehicleText = TranslationManager.translate("vehicle(s)", isPunjabiEnabled)
                        "$brandsSelected $brandText • $totalSelected $vehicleText"
                    }
                    is CatalogView.VehicleSelection -> {
                        val selectedText = TranslationManager.translate("selected", isPunjabiEnabled)
                        val vehicleText = TranslationManager.translate("model(s)", isPunjabiEnabled)
                        "${catalogViewModel.getSelectedVehiclesForBrand(selectedBrandForVehicles?.brandId ?: "").size} $vehicleText $selectedText"
                    }
                    is CatalogView.IndividualVehicleSelection -> {
                        val selectedText = TranslationManager.translate("selected", isPunjabiEnabled)
                        val filteredVehicles = catalogViewModel.getFilteredIndividualVehicles()
                        val totalCount = catalogState.individualVehiclesForSelection.size
                        val filteredCount = filteredVehicles.size
                        val selectedCount = catalogState.selectedIndividualVehicles.size
                        if (catalogState.filterState.hasActiveFilters()) {
                            "$selectedCount $selectedText • $filteredCount ${TranslationManager.translate("shown", isPunjabiEnabled)} / $totalCount ${TranslationManager.translate("total", isPunjabiEnabled)}"
                        } else {
                            "$selectedCount $selectedText / $totalCount ${TranslationManager.translate("total", isPunjabiEnabled)}"
                        }
                    }
                    is CatalogView.PriceEditing -> {
                        val productText = TranslationManager.translate("product(s)", isPunjabiEnabled)
                        "${catalogState.productsForPriceEditing.size} $productText"
                    }
                    is CatalogView.RecipientNameEntry -> TranslationManager.translate("Enter recipient name for catalog", isPunjabiEnabled)
                },
                onBackClick = {
                    when (currentView) {
                        is CatalogView.BrandSelection -> navController.smartPopBack()
                        is CatalogView.VehicleSelection -> {
                            currentView = CatalogView.BrandSelection
                            selectedBrandForVehicles = null
                        }
                        is CatalogView.IndividualVehicleSelection -> {
                            currentView = CatalogView.BrandSelection
                            selectedBrandForVehicles = null
                        }
                        is CatalogView.PriceEditing -> {
                            currentView = CatalogView.IndividualVehicleSelection
                        }
                        is CatalogView.RecipientNameEntry -> {
                            currentView = CatalogView.PriceEditing
                        }
                    }
                },
                actions = {
                    if (currentView is CatalogView.IndividualVehicleSelection) {
                        // Filter icon button
                        IconButton(
                            onClick = { showFilterSheet = true }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (catalogState.filterState.hasActiveFilters()) {
                                        Badge {
                                            Text(
                                                text = catalogState.filterState.getActiveFilterCount().toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = TranslationManager.translate("Filter", isPunjabiEnabled)
                                )
                            }
                        }
                    }
                    
                    // Clear All button - shown when there are selections
                    when (currentView) {
                        is CatalogView.BrandSelection -> {
                            if (totalSelected > 0) {
                                TextButton(
                                    onClick = { catalogViewModel.clearAllSelections() }
                                ) {
                                    TranslatedText("Clear All")
                                }
                            }
                        }
                        is CatalogView.VehicleSelection -> {
                            val selectedCount = catalogViewModel.getSelectedVehiclesForBrand(selectedBrandForVehicles?.brandId ?: "").size
                            if (selectedCount > 0) {
                                TextButton(
                                    onClick = {
                                        selectedBrandForVehicles?.brandId?.let { brandId ->
                                            catalogViewModel.clearVehiclesForBrand(brandId)
                                        }
                                    }
                                ) {
                                    TranslatedText("Clear All")
                                }
                            }
                        }
                        is CatalogView.IndividualVehicleSelection -> {
                            if (catalogState.selectedIndividualVehicles.isNotEmpty()) {
                                TextButton(
                                    onClick = { catalogViewModel.clearIndividualVehicleSelections() }
                                ) {
                                    TranslatedText("Clear All")
                                }
                            }
                        }
                        is CatalogView.PriceEditing -> {
                            // No clear all for price editing
                        }
                        is CatalogView.RecipientNameEntry -> {
                            // No clear all for recipient entry
                        }
                    }
                    
                    // Select All button - different behavior for each screen
                    when (currentView) {
                        is CatalogView.BrandSelection -> {
                            TextButton(
                                onClick = { catalogViewModel.selectAllBrands() }
                            ) {
                                TranslatedText("Select All")
                            }
                        }
                        is CatalogView.VehicleSelection -> {
                            selectedBrandForVehicles?.let { brand ->
                                TextButton(
                                    onClick = { catalogViewModel.selectAllVehiclesForBrand(brand.brandId) }
                                ) {
                                    TranslatedText("Select All")
                                }
                            }
                        }
                        is CatalogView.IndividualVehicleSelection -> {
                            TextButton(
                                onClick = { catalogViewModel.selectAllFilteredIndividualVehicles() }
                            ) {
                                TranslatedText("Select All")
                            }
                        }
                        is CatalogView.PriceEditing -> {
                            TextButton(
                                onClick = { catalogViewModel.resetAllPricesToOriginal() }
                            ) {
                                TranslatedText("Reset All Prices")
                            }
                        }
                        is CatalogView.RecipientNameEntry -> {
                            // No select all for recipient entry
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            when (currentView) {
                is CatalogView.BrandSelection, is CatalogView.VehicleSelection -> {
                    if (totalSelected > 0 && !catalogState.isGeneratingCatalog) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    catalogViewModel.loadIndividualVehiclesForSelection()
                                    currentView = CatalogView.IndividualVehicleSelection
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val nextStepText = "${TranslationManager.translate("Next Step", isPunjabiEnabled)} ($totalSelected)"
                            Text(nextStepText)
                        }
                    }
                }
                is CatalogView.IndividualVehicleSelection -> {
                    val selectedCount = catalogState.selectedIndividualVehicles.size
                    if (selectedCount > 0 && !catalogState.isGeneratingCatalog) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    catalogViewModel.loadProductsForPriceEditing()
                                    currentView = CatalogView.PriceEditing
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val nextStepText = "${TranslationManager.translate("Next Step", isPunjabiEnabled)} ($selectedCount)"
                            Text(nextStepText)
                        }
                    }
                }
                is CatalogView.PriceEditing -> {
                    if (catalogState.productsForPriceEditing.isNotEmpty() && !catalogState.isGeneratingCatalog) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                currentView = CatalogView.RecipientNameEntry
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val nextStepText = "${TranslationManager.translate("Next Step", isPunjabiEnabled)} (${catalogState.productsForPriceEditing.size})"
                            Text(nextStepText)
                        }
                    }
                }
                is CatalogView.RecipientNameEntry -> {
                    if (catalogState.productsForPriceEditing.isNotEmpty() && !catalogState.isGeneratingCatalog && !catalogState.recipientName.isNullOrBlank()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    val catalogResult = catalogViewModel.createCatalogAndGetId()
                                    catalogResult.fold(
                                        onSuccess = { catalogId ->
                                            // Share the catalog link via WhatsApp
                                            shareCatalogLinkViaWhatsApp(context, catalogId)
                                            // Navigate back after sharing
                                            navController.smartPopBack()
                                        },
                                        onFailure = { exception ->
                                            // Error already set in state, dialog will be shown automatically
                                        }
                                    )
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val generateText = "${TranslationManager.translate("Share Catalog Link", isPunjabiEnabled)} (${catalogState.productsForPriceEditing.size})"
                            Text(generateText)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                when (currentView) {
                    is CatalogView.BrandSelection -> {
                        if (brands.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TranslatedText("Loading brands...")
                                }
                            }
                        } else {
                            // Filter brands BEFORE passing to items()
                            val brandsWithVehicles = brands.filter { it.vehicle.isNotEmpty() }

                            if (brandsWithVehicles.isEmpty()) {
                                // Show empty state if all brands have no vehicles
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.CarCrash,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TranslatedText(
                                            englishText = "No brands with vehicles",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(brandsWithVehicles) { brand ->
                                        SelectableBrandCard(
                                            brand = brand,
                                            isSelected = catalogState.selectedBrandIds.contains(
                                                brand.brandId
                                            ),
                                            onClick = {
                                                catalogViewModel.toggleBrandSelection(brand.brandId)
                                            },
                                            onViewVehicles = {
                                                selectedBrandForVehicles = brand
                                                currentView = CatalogView.VehicleSelection
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is CatalogView.VehicleSelection -> {
                        selectedBrandForVehicles?.let { brand ->
                            // Filter to only show unsold vehicles
                            val vehicles = catalogViewModel.getUnsoldVehiclesForBrand(brand.brandId)
                            val selectedVehicleIds = catalogState.selectedVehicles[brand.brandId] ?: emptySet()

                            if (vehicles.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(vehicles) { vehicle ->
                                        SelectableVehicleCard(
                                            vehicle = vehicle,
                                            isSelected = selectedVehicleIds.contains(vehicle.productId),
                                            onClick = {
                                                catalogViewModel.toggleVehicleSelection(
                                                    brand.brandId,
                                                    vehicle.productId
                                                )
                                            }
                                        )
                                    }
                                }
                            } else {
                                // Empty state when no vehicles
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TranslatedText(
                                            englishText = "No vehicles available",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TranslatedText(
                                            englishText = "This brand has no vehicles in stock",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is CatalogView.IndividualVehicleSelection -> {
                        val filteredVehicles = catalogViewModel.getFilteredIndividualVehicles()
                        val filterState = catalogState.filterState
                        val selectedChassisNumbers = catalogState.selectedIndividualVehicles
                        
                        if (catalogState.isLoadingIndividualVehicles) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TranslatedText("Loading vehicles...")
                                }
                            }
                        } else if (catalogState.individualVehiclesForSelection.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TranslatedText(
                                        englishText = "No vehicles available",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Active Filter Chips
                                if (filterState.hasActiveFilters()) {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        if (!filterState.selectedColor.isNullOrBlank()) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(selectedColor = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Color", isPunjabiEnabled)}: ${filterState.selectedColor}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (filterState.minPrice != null) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(minPrice = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Min", isPunjabiEnabled)}: ₹${filterState.minPrice}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (filterState.maxPrice != null) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(maxPrice = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Max", isPunjabiEnabled)}: ₹${filterState.maxPrice}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (!filterState.chassisNumber.isNullOrBlank()) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(chassisNumber = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Chassis", isPunjabiEnabled)}: ${filterState.chassisNumber}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (!filterState.condition.isNullOrBlank()) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(condition = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Condition", isPunjabiEnabled)}: ${filterState.condition}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (filterState.minKms != null) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(minKms = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Min KM", isPunjabiEnabled)}: ${filterState.minKms}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (filterState.maxKms != null) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(maxKms = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Max KM", isPunjabiEnabled)}: ${filterState.maxKms}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (filterState.minYear != null) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(minYear = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Min Year", isPunjabiEnabled)}: ${filterState.minYear}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                        if (filterState.maxYear != null) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = {
                                                        catalogViewModel.setFilterState(
                                                            filterState.copy(maxYear = null)
                                                        )
                                                    },
                                                    label = { 
                                                        Text("${TranslationManager.translate("Max Year", isPunjabiEnabled)}: ${filterState.maxYear}") 
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                if (filteredVehicles.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(filteredVehicles) { product ->
                                            IndividualVehicleSelectionCard(
                                                product = product,
                                                isSelected = selectedChassisNumbers.contains(product.chassisNumber),
                                                onClick = {
                                                    catalogViewModel.toggleIndividualVehicleSelection(product.chassisNumber)
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Empty state when filters don't match
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.FilterList,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            TranslatedText(
                                                englishText = "No vehicles match filters",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TranslatedText(
                                                englishText = "Try adjusting your filters",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is CatalogView.PriceEditing -> {
                        
                        if (catalogState.isLoadingProducts) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TranslatedText("Loading products...")
                                }
                            }
                        } else if (catalogState.productsForPriceEditing.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TranslatedText(
                                        englishText = "No products to edit",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(catalogState.productsForPriceEditing.size) { index ->
                                    val productWithPrice = catalogState.productsForPriceEditing[index]
                                    PriceEditingCard(
                                        productWithPrice = productWithPrice,
                                        onPriceChange = { newPrice ->
                                            catalogViewModel.updateProductSellingPrice(
                                                productWithPrice.productRef,
                                                newPrice
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is CatalogView.RecipientNameEntry -> {
                        RecipientNameEntryScreen(
                            recipientName = catalogState.recipientName ?: "",
                            onRecipientNameChange = { name ->
                                catalogViewModel.setRecipientName(name)
                            }
                        )
                    }
                }
            }
        }
    }

    if (catalogState.isGeneratingCatalog) {
        AlertDialog(
            onDismissRequest = { },
            title = { TranslatedText("Generating Catalog") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    TranslatedText("Please wait...")
                }
            },
            confirmButton = {}
        )
    }

    catalogState.catalogGenerationError?.let { error ->
        AlertDialog(
            onDismissRequest = { catalogViewModel.clearCatalogGenerationError() },
            title = { TranslatedText("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { catalogViewModel.clearCatalogGenerationError() }) {
                    TranslatedText("OK")
                }
            }
        )
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FilterBottomSheet(
                filterState = catalogState.filterState,
                onFilterStateChange = { newState ->
                    catalogViewModel.setFilterState(newState)
                },
                onDismiss = { showFilterSheet = false },
                onApply = { showFilterSheet = false },
                onClear = {
                    catalogViewModel.clearFilters()
                },
                individualVehicles = catalogState.individualVehiclesForSelection
            )
        }
    }
    
}

sealed class CatalogView {
    object BrandSelection : CatalogView()
    object VehicleSelection : CatalogView()
    object IndividualVehicleSelection : CatalogView()
    object PriceEditing : CatalogView()
    object RecipientNameEntry : CatalogView()
}
@Composable
fun SelectableBrandCard(
    brand: Brand,
    isSelected: Boolean,
    onClick: () -> Unit,
    onViewVehicles: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo container with fixed aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (brand.logo.isNotEmpty()) {
                            AsyncImage(
                                model = brand.logo,
                                contentDescription = brand.brandId,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Placeholder icon when no logo
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Selected indicator badge
                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = TranslationManager.translate("Selected", isPunjabiEnabled),
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand name
            Text(
                text = brand.brandId.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Models count badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${brand.vehicle.size} ${TranslationManager.translate("models", isPunjabiEnabled)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View vehicles button
            Button(
                onClick = {
                    onViewVehicles()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    ,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatedText(
                        "Models",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
@Composable
fun SelectableVehicleCard(
    vehicle: VehicleSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle image
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                if (vehicle.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = vehicle.imageUrl,
                        contentDescription = vehicle.productId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val icon = if (vehicle.type.lowercase() == "car")
                        Icons.Default.DirectionsCar
                    else
                        Icons.Default.TwoWheeler

                    Icon(
                        icon,
                        contentDescription = vehicle.type,
                        modifier = Modifier.padding(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Vehicle info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = vehicle.productId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = vehicle.type.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    ) {
                        val qtyText = "${TranslationManager.translate("Qty:", isPunjabiEnabled)} ${vehicle.quantity}"
                        Text(
                            text = qtyText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = TranslationManager.translate("Selected", isPunjabiEnabled),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.RadioButtonUnchecked,
                    contentDescription = TranslationManager.translate("Not selected", isPunjabiEnabled),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTopAppBar(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    TopAppBar(
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = TranslationManager.translate("Back", isPunjabiEnabled),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            if (actions != null) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun IndividualVehicleSelectionCard(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle image
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                if (product.images.isNotEmpty()) {
                    AsyncImage(
                        model = product.images.first(),
                        contentDescription = product.productId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val icon = if (product.type.lowercase() == "car")
                        Icons.Default.DirectionsCar
                    else
                        Icons.Default.TwoWheeler

                    Icon(
                        icon,
                        contentDescription = product.type,
                        modifier = Modifier.padding(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Vehicle info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (product.chassisNumber.isNotEmpty()) {
                    Text(
                        text = "${TranslationManager.translate("Chassis:", isPunjabiEnabled)} ${product.chassisNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = product.type.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (product.colour.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = product.colour,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product.year > 0) {
                        Text(
                            text = "${TranslationManager.translate("Year:", isPunjabiEnabled)} ${product.year}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (product.kms > 0) {
                        Text(
                            text = "${TranslationManager.translate("KM:", isPunjabiEnabled)} ${product.kms}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = TranslationManager.translate("Selected", isPunjabiEnabled),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.RadioButtonUnchecked,
                    contentDescription = TranslationManager.translate("Not selected", isPunjabiEnabled),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun PriceEditingCard(
    productWithPrice: ProductWithPrice,
    onPriceChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var priceText by remember { mutableStateOf(productWithPrice.sellingPrice.toString()) }
    val product = productWithPrice.product

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image on the left
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                if (product.images.isNotEmpty()) {
                    AsyncImage(
                        model = product.images.first(),
                        contentDescription = product.productId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val icon = if (product.type.lowercase() == "car")
                        Icons.Default.DirectionsCar
                    else
                        Icons.Default.TwoWheeler

                    Icon(
                        icon,
                        contentDescription = product.type,
                        modifier = Modifier.padding(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product details and price input on the right
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = product.type.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (product.chassisNumber.isNotEmpty()) {
                        val chassisText = "${TranslationManager.translate("Chassis:", isPunjabiEnabled)} ${product.chassisNumber}"
                        Text(
                            text = chassisText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selling price input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { newValue ->
                        // Only allow numeric input
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            priceText = newValue
                            val price = newValue.toIntOrNull() ?: 0
                            if (price >= 0) {
                                onPriceChange(price)
                            }
                        }
                    },
                    label = { TranslatedText("Selling Price") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Show original price for reference
                val originalText = "${TranslationManager.translate("Original:", isPunjabiEnabled)} ₹${product.sellingPrice}"
                Text(
                    text = originalText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    onFilterStateChange: (FilterState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    individualVehicles: List<com.humbleSolutions.cardealer2.data.Product> = emptyList()
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    // Calculate max values from data or use defaults
    val maxPriceValue = remember(individualVehicles) {
        if (individualVehicles.isNotEmpty()) {
            individualVehicles.maxOfOrNull { it.sellingPrice }?.coerceAtLeast(1000000) ?: 50000000
        } else {
            50000000 // 50M default
        }
    }

    val maxKmsValue = remember(individualVehicles) {
        if (individualVehicles.isNotEmpty()) {
            individualVehicles.maxOfOrNull { it.kms }?.coerceAtLeast(100000) ?: 500000
        } else {
            500000 // 500K default
        }
    }

    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val maxYearValue = currentYear + 1
    val minYearValue = 1990

    var selectedColor by remember { mutableStateOf(filterState.selectedColor ?: "") }
    var minPrice by remember { mutableFloatStateOf(filterState.minPrice?.toFloat() ?: 0f) }
    var maxPrice by remember { mutableFloatStateOf(filterState.maxPrice?.toFloat() ?: maxPriceValue.toFloat()) }
    var chassisText by remember { mutableStateOf(filterState.chassisNumber ?: "") }
    var selectedCondition by remember { mutableStateOf(filterState.condition ?: "") }
    var minKms by remember { mutableFloatStateOf(filterState.minKms?.toFloat() ?: 0f) }
    var maxKms by remember { mutableFloatStateOf(filterState.maxKms?.toFloat() ?: maxKmsValue.toFloat()) }
    var minYear by remember { mutableFloatStateOf(filterState.minYear?.toFloat() ?: minYearValue.toFloat()) }
    var maxYear by remember { mutableFloatStateOf(filterState.maxYear?.toFloat() ?: maxYearValue.toFloat()) }

    // Load colors
    var colorList by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        scope.launch {
            val result = com.humbleSolutions.cardealer2.repository.VehicleRepository.getColours()
            result.onSuccess { colors ->
                colorList = colors
            }
        }
    }

    // Sync local state when filterState prop changes
    LaunchedEffect(filterState, maxPriceValue, maxKmsValue, maxYearValue) {
        selectedColor = filterState.selectedColor ?: ""
        minPrice = filterState.minPrice?.toFloat() ?: 0f
        maxPrice = filterState.maxPrice?.toFloat() ?: maxPriceValue.toFloat()
        chassisText = filterState.chassisNumber ?: ""
        selectedCondition = filterState.condition ?: ""
        minKms = filterState.minKms?.toFloat() ?: 0f
        maxKms = filterState.maxKms?.toFloat() ?: maxKmsValue.toFloat()
        minYear = filterState.minYear?.toFloat() ?: minYearValue.toFloat()
        maxYear = filterState.maxYear?.toFloat() ?: maxYearValue.toFloat()
    }

    val conditionItems = remember(isPunjabiEnabled) {
        listOf(
            TranslationManager.translate("Excellent", isPunjabiEnabled),
            TranslationManager.translate("Good", isPunjabiEnabled),
            TranslationManager.translate("Fair", isPunjabiEnabled),
            TranslationManager.translate("Poor", isPunjabiEnabled)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with improved styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TranslatedText(
                englishText = "Filter Vehicles",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Color Filter Dropdown with improved design
            var colorExpanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = colorExpanded,
                    onExpandedChange = { colorExpanded = !colorExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedColor.ifEmpty { TranslationManager.translate("Select Color", isPunjabiEnabled) },
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            TranslatedText(
                                "Color",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    ExposedDropdownMenu(
                        expanded = colorExpanded,
                        onDismissRequest = { colorExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    TranslationManager.translate("None", isPunjabiEnabled),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedColor = ""
                                colorExpanded = false
                            }
                        )
                        colorList.forEach { color ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        color,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    selectedColor = if (selectedColor == color) "" else color
                                    colorExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Price Range with Modern Slider Design
            ModernRangeSlider(
                title = "Price Range",
                minValue = minPrice,
                maxValue = maxPrice,
                onMinValueChange = { minPrice = it.coerceAtMost(maxPrice) },
                onMaxValueChange = { maxPrice = it.coerceAtLeast(minPrice) },
                valueRange = 0f..maxPriceValue.toFloat(),
                formatValue = { "₹${String.format("%,d", it.toInt())}" },
                isPunjabiEnabled = isPunjabiEnabled
            )

            // KM Range with Modern Slider Design
            ModernRangeSlider(
                title = "KM Range",
                minValue = minKms,
                maxValue = maxKms,
                onMinValueChange = { minKms = it.coerceAtMost(maxKms) },
                onMaxValueChange = { maxKms = it.coerceAtLeast(minKms) },
                valueRange = 0f..maxKmsValue.toFloat(),
                formatValue = { "${String.format("%,d", it.toInt())} KM" },
                isPunjabiEnabled = isPunjabiEnabled
            )

            // Year Range with Modern Slider Design
            ModernRangeSlider(
                title = "Year Range",
                minValue = minYear,
                maxValue = maxYear,
                onMinValueChange = { minYear = it.coerceAtMost(maxYear) },
                onMaxValueChange = { maxYear = it.coerceAtLeast(minYear) },
                valueRange = minYearValue.toFloat()..maxYearValue.toFloat(),
                formatValue = { it.toInt().toString() },
                isPunjabiEnabled = isPunjabiEnabled,
                steps = (maxYearValue - minYearValue - 1).coerceAtLeast(0)
            )

            // Chassis Number with improved design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                OutlinedTextField(
                    value = chassisText,
                    onValueChange = { chassisText = it },
                    label = {
                        TranslatedText(
                            "Chassis Number",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    placeholder = {
                        TranslatedText(
                            "Search by chassis number",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Numbers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            // Condition Dropdown with improved design
            var expanded by remember { mutableStateOf(false) }
            val displayCondition = remember(selectedCondition, isPunjabiEnabled) {
                if (selectedCondition.isBlank()) {
                    TranslationManager.translate("Select Condition", isPunjabiEnabled)
                } else {
                    when(selectedCondition) {
                        "Excellent" -> TranslationManager.translate("Excellent", isPunjabiEnabled)
                        "Good" -> TranslationManager.translate("Good", isPunjabiEnabled)
                        "Fair" -> TranslationManager.translate("Fair", isPunjabiEnabled)
                        "Poor" -> TranslationManager.translate("Poor", isPunjabiEnabled)
                        else -> selectedCondition
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    OutlinedTextField(
                        value = displayCondition,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            TranslatedText(
                                "Condition",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        conditionItems.forEach { condition ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        condition,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    // Map back to English for storage
                                    val englishCondition = when(condition) {
                                        TranslationManager.translate("Excellent", isPunjabiEnabled) -> "Excellent"
                                        TranslationManager.translate("Good", isPunjabiEnabled) -> "Good"
                                        TranslationManager.translate("Fair", isPunjabiEnabled) -> "Fair"
                                        TranslationManager.translate("Poor", isPunjabiEnabled) -> "Poor"
                                        else -> condition
                                    }
                                    selectedCondition = if (selectedCondition == englishCondition) "" else englishCondition
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons with improved design
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    selectedColor = ""
                    minPrice = 0f
                    maxPrice = maxPriceValue.toFloat()
                    chassisText = ""
                    selectedCondition = ""
                    minKms = 0f
                    maxKms = maxKmsValue.toFloat()
                    minYear = minYearValue.toFloat()
                    maxYear = maxYearValue.toFloat()
                    onClear()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                TranslatedText(
                    "Clear All",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    val newFilterState = FilterState(
                        selectedColor = selectedColor.takeIf { it.isNotBlank() },
                        minPrice = if (minPrice > 0) minPrice.toInt() else null,
                        maxPrice = if (maxPrice < maxPriceValue) maxPrice.toInt() else null,
                        chassisNumber = chassisText.takeIf { it.isNotBlank() },
                        condition = selectedCondition.takeIf { it.isNotBlank() },
                        minKms = if (minKms > 0) minKms.toInt() else null,
                        maxKms = if (maxKms < maxKmsValue) maxKms.toInt() else null,
                        minYear = if (minYear > minYearValue) minYear.toInt() else null,
                        maxYear = if (maxYear < maxYearValue) maxYear.toInt() else null
                    )
                    onFilterStateChange(newFilterState)
                    onApply()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                TranslatedText(
                    "Apply Filters",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ModernRangeSlider(
    title: String,
    minValue: Float,
    maxValue: Float,
    onMinValueChange: (Float) -> Unit,
    onMaxValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String,
    isPunjabiEnabled: Boolean,
    steps: Int = 99
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            TranslatedText(
                englishText = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Value Display with enhanced styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatValue(minValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Max",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatValue(maxValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Min Slider with label
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TranslatedText(
                    englishText = "Minimum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = minValue,
                    onValueChange = onMinValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Max Slider with label
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TranslatedText(
                    englishText = "Maximum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = maxValue,
                    onValueChange = onMaxValueChange,
                    valueRange = minValue..valueRange.endInclusive,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
@Composable
fun RecipientNameEntryScreen(
    recipientName: String,
    onRecipientNameChange: (String) -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TranslatedText(
            englishText = "Enter Recipient Name",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        TranslatedText(
            englishText = "Please enter the name of the person you want to send this catalog to.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = recipientName,
            onValueChange = onRecipientNameChange,
            label = { TranslatedText("Recipient Name") },
            placeholder = { TranslatedText("Enter recipient name") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
