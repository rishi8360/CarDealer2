package com.humbleSolutions.cardealer2.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.humbleSolutions.cardealer2.ViewModel.BrandSelectedViewModel
import com.humbleSolutions.cardealer2.ViewModel.HomeScreenViewModel
import com.humbleSolutions.cardealer2.data.VehicleSummary
import com.humbleSolutions.cardealer2.utility.ConsistentTopAppBar
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandSelected(
    brandId: String,
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel,
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val viewModel: BrandSelectedViewModel = viewModel()
    val brands by homeScreenViewModel.brands.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var backButtonClicked by remember { mutableStateOf(false) }

    val tabs = remember(isPunjabiEnabled) {
        listOf(
            TranslationManager.translate("Cars", isPunjabiEnabled) to Icons.Default.DirectionsCar,
            TranslationManager.translate("Bikes", isPunjabiEnabled) to Icons.Default.TwoWheeler
        )
    }

    // Load brands when screen appears
    LaunchedEffect(Unit) {
        homeScreenViewModel.loadBrands()
    }

    // Load brand by ID once brands are loaded
    LaunchedEffect(brandId, brands) {
        if (brands.isNotEmpty()) {
            viewModel.loadBrandById(brandId, brands)
        }
    }

    val subtitle = remember(brand, isPunjabiEnabled) {
        if (brand != null) {
            val totalVehicles = brand!!.vehicle.size
            val carCount = brand!!.vehicle.count { it.type.lowercase() == "car" }
            val bikeCount = brand!!.vehicle.count { it.type.lowercase() == "bike" }
            val vehiclesText = TranslationManager.translate("vehicles", isPunjabiEnabled)
            val carsText = TranslationManager.translate("cars", isPunjabiEnabled)
            val bikesText = TranslationManager.translate("bikes", isPunjabiEnabled)
            "$totalVehicles $vehiclesText • $carCount $carsText • $bikeCount $bikesText"
        } else null
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = brandId.uppercase(Locale.ROOT),
                subtitle = subtitle,
                navController = navController,
                onBackClick = {
                    if (!backButtonClicked) {
                        backButtonClicked = true
                        navController.popBackStack()
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier
                            .shadow(4.dp, CircleShape)
                            .background(
                                if (showSearch) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (showSearch) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (showSearch) 
                                TranslationManager.translate("Close Search", isPunjabiEnabled) 
                            else 
                                TranslationManager.translate("Search", isPunjabiEnabled),
                            tint = if (showSearch) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        /*floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_vehicle/$brandId") },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = TranslationManager.translate("Add Vehicle", isPunjabiEnabled),
                    tint = Color.White
                )
            }
        }*/
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

                // Animated Search Bar
                AnimatedVisibility(
                    visible = showSearch,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = {
                            TranslatedText(
                                "Search vehicles...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = TranslationManager.translate("Search", isPunjabiEnabled),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = if (searchText.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = TranslationManager.translate("Clear", isPunjabiEnabled),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else null,
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

                if (showSearch) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Enhanced Tab Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        tabs.forEachIndexed { index, (title, icon) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedTab == index)
                                    MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = title,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selectedTab == index)
                                            Color.White
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == index)
                                            Color.White
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content Section
                when {
                    isLoading -> {
                        EnhancedLoadingState()
                    }

                    error != null -> {
                        EnhancedErrorState(error!!)
                    }

                    brand != null -> {
                        val filteredVehicles = brand!!.vehicle.filter { vehicle ->
                            val matchesTab = if (selectedTab == 0)
                                vehicle.type.lowercase() == "car"
                            else
                                vehicle.type.lowercase() == "bike"
                            val matchesSearch = searchText.isBlank() ||
                                    vehicle.productId.contains(searchText, ignoreCase = true)
                            matchesTab && matchesSearch
                        }

                        if (filteredVehicles.isEmpty()) {
                            EnhancedEmptyVehicleState(
                                selectedTab = selectedTab,
                                hasSearch = searchText.isNotEmpty()
                            )
                        } else {
                            // Results Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val availableText = "${TranslationManager.translate("Available", isPunjabiEnabled)} ${tabs[selectedTab].first}"
                                Text(
                                    text = availableText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                val foundText = "${filteredVehicles.size} ${TranslationManager.translate("found", isPunjabiEnabled)}"
                                Text(
                                    text = foundText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Enhanced Vehicle List
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredVehicles) { vehicle ->
                                    EnhancedVehicleCard(vehicle) {
                                        navController.navigate("brand_Vehicle/${vehicle.productId}/${brandId}")
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        EnhancedEmptyDataState()
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedVehicleCard(
    vehicle: VehicleSummary,
    onClickCard: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    // Decreased the fixed height from 180.dp to 120.dp for a more compact card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp) // MODIFIED: Reduced height
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClickCard)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(), // Fill the entire Box/Card
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Image with enhanced styling
            // MODIFIED: width is 1/3 of the card, fills the card's height, and removed padding
            Surface(
                modifier = Modifier
                    .fillMaxHeight() // MODIFIED: Fill height of the card
                    .fillMaxWidth(0.33f), // MODIFIED: 1/3 of the card width
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp), // Matched card corner radius
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                if (vehicle.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = vehicle.imageUrl,
                        contentDescription = vehicle.productId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp), // Still pad the image itself inside the surface
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder based on vehicle type
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

            // Vehicle Details (The rest of the content)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp), // Added vertical padding for alignment
                verticalArrangement = Arrangement.Center
            ) {
                // Vehicle Name
                Text(
                    text = vehicle.productId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))


                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    // Product ID Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Tag,
                                contentDescription = TranslationManager.translate("Type of vehicle", isPunjabiEnabled),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val typeText = "${TranslationManager.translate("Type:", isPunjabiEnabled)} ${vehicle.type}"
                            Text(
                                text = typeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Tag,
                                contentDescription = TranslationManager.translate("Number of vehicle", isPunjabiEnabled),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val numberText = "${TranslationManager.translate("Number of vehicle:", isPunjabiEnabled)} ${vehicle.quantity}"
                            Text(
                                text = numberText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                        }
                    }
                }
            }

            // Arrow Indicator
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = TranslationManager.translate("Go", isPunjabiEnabled),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f)
                    .padding(end = 16.dp), // Added end padding for spacing
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
@Composable
fun EnhancedLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            TranslatedText(
                "Loading vehicles...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EnhancedErrorState(error: String) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            TranslatedText(
                "Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EnhancedEmptyVehicleState(
    selectedTab: Int,
    hasSearch: Boolean
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val icon =
                if (selectedTab == 0) Icons.Default.DirectionsCar else Icons.Default.TwoWheeler
            val vehicleType = if (selectedTab == 0) 
                TranslationManager.translate("cars", isPunjabiEnabled) 
            else 
                TranslationManager.translate("bikes", isPunjabiEnabled)

            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Icon(
                    icon,
                    contentDescription = vehicleType,
                    modifier = Modifier.padding(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val noMatchText = if (hasSearch) {
                "${TranslationManager.translate("No matching", isPunjabiEnabled)} $vehicleType ${TranslationManager.translate("found", isPunjabiEnabled)}"
            } else {
                "${TranslationManager.translate("No", isPunjabiEnabled)} $vehicleType ${TranslationManager.translate("available", isPunjabiEnabled)}"
            }
            Text(
                text = noMatchText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            val helpText = if (hasSearch) {
                TranslationManager.translate("Try adjusting your search terms or check the other category.", isPunjabiEnabled)
            } else {
                "${TranslationManager.translate("This brand doesn't have any", isPunjabiEnabled)} $vehicleType ${TranslationManager.translate("in our inventory yet.", isPunjabiEnabled)}"
            }
            Text(
                text = helpText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EnhancedEmptyDataState() {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📭",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            TranslatedText(
                "No brand data available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            TranslatedText(
                "Unable to load brand information at this time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}