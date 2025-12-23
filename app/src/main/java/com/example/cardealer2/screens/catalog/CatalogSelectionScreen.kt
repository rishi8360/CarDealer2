package com.example.cardealer2.screens.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cardealer2.ViewModel.CatalogSelectionViewModel
import com.example.cardealer2.ViewModel.HomeScreenViewModel
import com.example.cardealer2.data.Brand
import com.example.cardealer2.data.VehicleSummary
import com.example.cardealer2.utils.PdfGenerator
import com.example.cardealer2.utility.smartPopBack
import kotlinx.coroutines.launch
import java.net.URLEncoder
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

    val brands by homeScreenViewModel.brands.collectAsState()
    val catalogState by catalogViewModel.state.collectAsState()

    var currentView by remember { mutableStateOf<CatalogView>(CatalogView.BrandSelection) }
    var selectedBrandForVehicles by remember { mutableStateOf<Brand?>(null) }

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
                    is CatalogView.BrandSelection -> "Select Brands"
                    is CatalogView.VehicleSelection -> "Select Vehicles"
                },
                subtitle = when (currentView) {
                    is CatalogView.BrandSelection -> "$brandsSelected brand(s) • $totalSelected vehicle(s)"
                    is CatalogView.VehicleSelection -> "${catalogViewModel.getSelectedVehiclesForBrand(selectedBrandForVehicles?.brandId ?: "").size} vehicle(s) selected"
                },
                onBackClick = {
                    when (currentView) {
                        is CatalogView.BrandSelection -> navController.smartPopBack()
                        is CatalogView.VehicleSelection -> {
                            currentView = CatalogView.BrandSelection
                            selectedBrandForVehicles = null
                        }
                    }
                },
                actions = {

                    if (currentView is CatalogView.BrandSelection && totalSelected > 0) {
                        TextButton(
                            onClick = { catalogViewModel.clearAllSelections() }
                        ) {
                            Text("Clear All")
                        }

                    }
                    TextButton(
                        onClick = { catalogViewModel.selectAllBrands() }
                    ) {
                        Text("Select All")
                    }
                }
            )
        },
        floatingActionButton = {
            if (totalSelected > 0 && !catalogState.isGeneratingCatalog) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val htmlResult = catalogViewModel.generateCatalogHtml()
                            htmlResult.fold(
                                onSuccess = { htmlContent ->
                                    // Keep isGeneratingCatalog true while generating PDF
                                    val pdfResult = PdfGenerator.generatePdfFromHtml(
                                        context = context,
                                        htmlContent = htmlContent,
                                        fileName = "catalog_${System.currentTimeMillis()}"
                                    )
                                    pdfResult.fold(
                                        onSuccess = { file ->
                                            // Set generation complete before navigation
                                            catalogViewModel.setCatalogGenerationComplete()
                                            val encodedPath = URLEncoder.encode(file.absolutePath, "UTF-8")
                                            navController.navigate("pdf_viewer/$encodedPath")
                                        },
                                        onFailure = { exception ->
                                            catalogViewModel.setCatalogGenerationError("Failed to generate PDF: ${exception.message}")
                                        }
                                    )
                                },
                                onFailure = { exception ->
                                    // Error already set in state, isGeneratingCatalog already set to false
                                }
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate PDF ($totalSelected)")
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
                                    Text("Loading brands...")
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
                                        Text(
                                            text = "No brands with vehicles",
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
                            val vehicles = brand.vehicle
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
                                        Text(
                                            text = "No vehicles available",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "This brand has no vehicles in stock",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (catalogState.isGeneratingCatalog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Generating Catalog") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please wait...")
                }
            },
            confirmButton = {}
        )
    }

    catalogState.catalogGenerationError?.let { error ->
        AlertDialog(
            onDismissRequest = { catalogViewModel.clearCatalogGenerationError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { catalogViewModel.clearCatalogGenerationError() }) {
                    Text("OK")
                }
            }
        )
    }
}

sealed class CatalogView {
    object BrandSelection : CatalogView()
    object VehicleSelection : CatalogView()
}

@Composable
fun SelectableBrandCard(
    brand: Brand,
    isSelected: Boolean,
    onClick: () -> Unit,
    onViewVehicles: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                if (brand.logo.isNotEmpty()) {
                    AsyncImage(
                        model = brand.logo,
                        contentDescription = brand.brandId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brand info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = brand.brandId.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${brand.vehicle.size} models",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View button - full width at bottom
            FilledTonalButton(
                onClick = {
                    onViewVehicles()
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "View Vehicles",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
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
                    val icon = if (vehicle.type.equals("car", true))
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
                        Text(
                            text = "Qty: ${vehicle.quantity}",
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
                    contentDescription = "Selected",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not selected",
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
                    contentDescription = "Back",
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