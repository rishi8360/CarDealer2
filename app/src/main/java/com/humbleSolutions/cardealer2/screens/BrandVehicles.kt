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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.humbleSolutions.cardealer2.ViewModel.BrandVehicleViewModel
import com.humbleSolutions.cardealer2.data.Product
import com.humbleSolutions.cardealer2.utility.ConsistentTopAppBar
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandVehicles(
    productId: String,
    brandName: String,
    navController: NavController,
    viewModel: BrandVehicleViewModel
) {
    val productList by viewModel.product.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(productId, brandName) {
        viewModel.loadProductByModelNameBrandName(productId, brandName)
    }

    var searchText by remember { mutableStateOf("") }
    var soldStatusFilter by remember { mutableStateOf("not_sold_products") } // Default to not sold
    var backButtonClicked by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var filteredFeatures by remember { mutableStateOf<List<Product>>(emptyList()) }

    // Update filtered features when productList or searchText changes
    LaunchedEffect(productList, searchText, soldStatusFilter) {
        val nonNullProducts = productList.filterNotNull()
        val query = searchText.trim().lowercase()

        val tempFiltered = nonNullProducts.filter { product ->
            val matchesSoldStatus = when (soldStatusFilter) {
                "all_products" -> true
                "sold_products" -> product.sold
                "not_sold_products" -> !product.sold
                else -> true // Should not happen
            }

            val name = product.productId.trim().lowercase()
            val color = product.colour.trim().lowercase()
            val chassis = product.chassisNumber.trim().lowercase()
            val condition = product.condition.trim().lowercase()
            val lastService = product.lastService.trim().lowercase()
            val price = product.price.toString()
            val year = product.year.toString()
            val kms = product.kms.toString()
            val owners = product.previousOwners.toString()
            val type = product.type.trim().lowercase()
            val brand = product.brandId.trim().lowercase()

            matchesSoldStatus && (query.isBlank() ||
                    name.contains(query) ||
                    color.contains(query) ||
                    chassis.contains(query) ||
                    condition.contains(query) ||
                    lastService.contains(query) ||
                    price.contains(query) ||
                    year.contains(query) ||
                    kms.contains(query) ||
                    owners.contains(query) ||
                    type.contains(query) ||
                    brand.contains(query))
        }
        filteredFeatures = tempFiltered
    }

    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = if (isLoading) 
                    TranslationManager.translate("Loading...", isPunjabiEnabled) 
                else 
                    "$brandName $productId",
                subtitle = if (productList.isNotEmpty()) {
                    "${filteredFeatures.size} ${TranslationManager.translate("vehicles available", isPunjabiEnabled)}"
                } else null,
                navController = navController,
                onBackClick = {
                    if (!backButtonClicked) {
                        backButtonClicked = true
                        navController.popBackStack()
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
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
                                englishText = "Loading vehicles...",
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
                                englishText = "Something went wrong",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    // Always show controls so the user can search/edit even if list is empty
                    EnhancedControlsSection(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        onFilterClick = { scope.launch { sheetState.show() } },
                        onResetClick = {
                            searchText = ""
                            soldStatusFilter = "not_sold_products" // Reset sold status filter
                            filteredFeatures = productList
                                .filterNotNull()
                                .filter { !it.sold }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        // Show no-match state when there are products but none match search
                        productList.filterNotNull().isNotEmpty() && filteredFeatures.isEmpty() && searchText.isNotBlank() -> {
                            EnhancedEmptyState(
                                title = TranslationManager.translate("No Matching Vehicles", isPunjabiEnabled),
                                message = TranslationManager.translate("Try adjusting your search criteria.", isPunjabiEnabled),
                                icon = Icons.Default.Search
                            )
                        }
                        // Show no vehicles state when there are no products at all
                        productList.filterNotNull().isEmpty() -> {
                            EnhancedEmptyState(
                                title = TranslationManager.translate("No Vehicles Available", isPunjabiEnabled),
                                message = TranslationManager.translate("There are no vehicles available for this brand and model.", isPunjabiEnabled),
                                icon = Icons.Default.DirectionsCar
                            )
                        }
                        else -> {
                            // Show list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredFeatures) { feature ->
                                    EnhancedVehicleCard(feature) {
                                        navController.navigate("vehicle_detail/${feature.chassisNumber}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Enhanced Bottom Sheet
    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide() } },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            SimpleFilterBottomSheet(
                onDismiss = { scope.launch { sheetState.hide() } },
                onSortOptionSelected = { sort ->
                    if (sort.endsWith("_products")) {
                        soldStatusFilter = sort
                    } else {
                        val currentProducts = productList.filterNotNull()
                        val query = searchText.trim().lowercase()
                        val searchFiltered = if (query.isBlank()) {
                            currentProducts
                        } else {
                            currentProducts.filter { product ->
                                val name = product.productId.trim().lowercase()
                                val color = product.colour.trim().lowercase()
                                val chassis = product.chassisNumber.trim().lowercase()
                                val condition = product.condition.trim().lowercase()
                                val lastService = product.lastService.trim().lowercase()
                                val price = product.price.toString()
                                val year = product.year.toString()
                                val kms = product.kms.toString()
                                val owners = product.previousOwners.toString()
                                val type = product.type.trim().lowercase()
                                val brand = product.brandId.trim().lowercase()

                                name.contains(query) ||
                                        color.contains(query) ||
                                        chassis.contains(query) ||
                                        condition.contains(query) ||
                                        lastService.contains(query) ||
                                        price.contains(query) ||
                                        year.contains(query) ||
                                        kms.contains(query) ||
                                        owners.contains(query) ||
                                        type.contains(query) ||
                                        brand.contains(query)
                            }
                        }

                        filteredFeatures = when (sort) {
                            "price_low_high" -> searchFiltered.sortedBy { it.price }
                            "price_high_low" -> searchFiltered.sortedByDescending { it.price }
                            "year_new_old" -> searchFiltered.sortedByDescending { it.year }
                            "year_old_new" -> searchFiltered.sortedBy { it.year }
                            "condition" -> searchFiltered.sortedBy { feature ->
                                when (feature.condition.trim().lowercase()) {
                                    "very bad" -> 1
                                    "bad" -> 2
                                    "ok" -> 3
                                    "good" -> 4
                                    // keep unknowns at the end
                                    else -> 5
                                }
                            }
                            else -> searchFiltered
                        }
                    }
                    scope.launch { sheetState.hide() }
                }
            )
        }
    }
}

@Composable
fun EnhancedControlsSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    Column {
        // Control Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Button
            OutlinedButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = TranslationManager.translate("Filter", isPunjabiEnabled),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TranslatedText("Filter")
            }

            // Reset Button
            OutlinedButton(
                onClick = onResetClick,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                TranslatedText("Reset")
            }
        }

        // Static Search Bar
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = {
                TranslatedText(
                    "Search vehicles...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
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
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = TranslationManager.translate("Clear search", isPunjabiEnabled),
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
}

@Composable
fun EnhancedVehicleCard(
    feature: Product,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp) // MODIFIED: Reduced height from 160.dp to 120.dp
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(), // MODIFIED: Fill the entire Box/Card height and width
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Image
            // MODIFIED: width is 1/3 of the card, fills the card's height, and changed shape for corner radius
            Surface(
                modifier = Modifier
                    .fillMaxHeight() // MODIFIED: Fill height of the card
                    .fillMaxWidth(0.33f), // MODIFIED: 1/3 of the card width
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp), // Matched card corner radius
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                if (feature.images.isNotEmpty()) {
                    AsyncImage(
                        model = feature.images[0],
                        contentDescription = feature.productId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp), // Still pad the image itself inside the surface
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder for missing image
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Vehicle",
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
                    .padding(vertical = 16.dp), // MODIFIED: Added vertical padding for better alignment
                verticalArrangement = Arrangement.Center // Centering content vertically
            ) {
                // Vehicle Name
                Text(
                    text = feature.productId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vehicle Info Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VehicleInfoChip(
                        icon = Icons.Outlined.Palette,
                        text = feature.colour,
                        modifier = Modifier.weight(1f, false)
                    )
                    VehicleInfoChip(
                        icon = Icons.Outlined.CalendarToday,
                        text = feature.year.toString(),
                        modifier = Modifier.weight(1f, false)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Condition
                Text(
                    text = "Condition: ${feature.condition}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Go",
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f)
                    .padding(end = 16.dp), // MODIFIED: Added end padding for spacing
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
@Composable
fun VehicleInfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EnhancedEmptyState(
    title: String,
    message: String,
    icon: ImageVector
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleFilterBottomSheet(
    onSortOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
            .padding(bottom = 32.dp)
    ) {
        TranslatedText(
            englishText = "Filter Options",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Filter by Sold Status
        FilterOption(TranslationManager.translate("All Products", isPunjabiEnabled)) {
            onSortOptionSelected("all_products")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterOption(TranslationManager.translate("Sold Products", isPunjabiEnabled)) {
            onSortOptionSelected("sold_products")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterOption(TranslationManager.translate("Not Sold Products", isPunjabiEnabled)) {
            onSortOptionSelected("not_sold_products")
        }


        // Sort by Price
        FilterOption(TranslationManager.translate("Sort by Price (Low to High)", isPunjabiEnabled)) {
            onSortOptionSelected("price_low_high")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterOption(TranslationManager.translate("Sort by Price (High to Low)", isPunjabiEnabled)) {
            onSortOptionSelected("price_high_low")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sort by Year
        FilterOption(TranslationManager.translate("Sort by Year (New to Old)", isPunjabiEnabled)) {
            onSortOptionSelected("year_new_old")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterOption(TranslationManager.translate("Sort by Year (Old to New)", isPunjabiEnabled)) {
            onSortOptionSelected("year_old_new")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter by Condition
        FilterOption(TranslationManager.translate("Sort by Condition", isPunjabiEnabled)) {
            onSortOptionSelected("condition")
        }
    }
}

@Composable
fun FilterOption(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}