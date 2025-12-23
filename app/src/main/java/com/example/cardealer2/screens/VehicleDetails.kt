package com.example.cardealer2.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cardealer2.ViewModel.BrandVehicleViewModel
import com.example.cardealer2.ViewModel.VehicleDetailViewModel
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Broker
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.EditActionButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    chassisNumber: String,
    navController: NavController,
    viewModel: VehicleDetailViewModel ,
    brandVehiclesViewModel: BrandVehicleViewModel
) {
    val productFeature by viewModel.product.collectAsState()

    val productList by brandVehiclesViewModel.product.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Load product directly from repository
    // ViewModel will automatically observe repository StateFlow and update when product changes
    LaunchedEffect(chassisNumber) {
        viewModel.reloadProductFromRepository(chassisNumber)
    }

    // Also try to load from productList if available (for backward compatibility)
    LaunchedEffect(chassisNumber, productList) {
        if (productList.isNotEmpty()) {
            viewModel.loadProductFeatureByChassis(productList, chassisNumber)
        }
    }
    
    // Note: No need to manually reload when vehicle_updated flag is set
    // The ViewModel automatically observes repository StateFlow and will update when product changes in Firestore
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = productFeature?.productId ?: "Vehicle Details",
                subtitle = if (productFeature != null) "Vehicle Details" else null,
                navController = navController,
                actions = {
                    productFeature?.chassisNumber?.let { chassisNumber ->
                        EditActionButton(
                            onClick = {
                                navController.navigate("edit_vehicle/$chassisNumber")
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            productFeature?.chassisNumber?.let { chassisNumber ->
                FloatingActionButton(
                    onClick = {
                        navController.navigate("sell_vehicle/$chassisNumber")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachMoney,
                        contentDescription = "Sell Vehicle"
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (productFeature?.sold == true) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Content Section
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
                                Text(
                                    text = "Loading vehicle details...",
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
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "⚠️",
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .wrapContentSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Something went wrong",
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
                    productFeature != null -> {
                        VehicleDetailContent(
                            product = productFeature!!,
                            modifier = Modifier.weight(1f),
                            viewModel = viewModel
                        )
                    }

                    else -> {
                        EnhancedNotFoundState(chassisNumber)
                    }
                }
            }
        }
    }
}
    @Composable
    fun VehicleDetailContent(
        product: Product,
        modifier: Modifier = Modifier,
        viewModel: VehicleDetailViewModel
    ) {
        val context = LocalContext.current
        var showOwnerDialog by remember { mutableStateOf(false) }
        var showBrokerDialog by remember { mutableStateOf(false) }
        
        val customer by viewModel.customer.collectAsState()
        val broker by viewModel.broker.collectAsState()
        val isLoadingCustomer by viewModel.isLoadingCustomer.collectAsState()
        val isLoadingBroker by viewModel.isLoadingBroker.collectAsState()
        val customerError by viewModel.customerError.collectAsState()
        val brokerError by viewModel.brokerError.collectAsState()
        
        // Load customer data when dialog opens
        LaunchedEffect(showOwnerDialog) {
            if (showOwnerDialog && customer == null && product.ownerRef != null) {
                viewModel.loadCustomerByReference(product.ownerRef)
            }
        }
        
        // Load broker data when dialog opens
        LaunchedEffect(showBrokerDialog) {
            if (showBrokerDialog && broker == null && product.brokerOrMiddleManRef != null) {
                viewModel.loadBrokerByReference(product.brokerOrMiddleManRef)
            }
        }
        
        // Clear data when dialogs close
        LaunchedEffect(showOwnerDialog) {
            if (!showOwnerDialog) {
                viewModel.clearCustomerData()
            }
        }
        
        LaunchedEffect(showBrokerDialog) {
            if (!showBrokerDialog) {
                viewModel.clearBrokerData()
            }
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Sold Banner
            if (product.sold) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = "Sold",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SOLD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }


            if (product.images.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Vehicle Images",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 🚀 Swipable Zoomable Image Gallery
                        ZoomableImageGallery(
                            imageUrls = product.images,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Text(
                            text = "Swipe • Double tap to zoom • Drag to move",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }


            // Quick Info Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickInfoCard(
                    title = "Price",
                    value = "₹${product.price}",
                    icon = Icons.Outlined.AttachMoney,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                QuickInfoCard(
                    title = "Year",
                    value = product.year.toString(),
                    icon = Icons.Outlined.CalendarToday,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickInfoCard(
                    title = "Kilometers",
                    value = "${product.kms} km",
                    icon = Icons.Outlined.Speed,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                QuickInfoCard(
                    title = "Condition",
                    value = product.condition,
                    icon = Icons.Outlined.CheckCircle,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detailed Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Detailed Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Vehicle Details
                    EnhancedDetailRow(
                        icon = Icons.Outlined.DirectionsCar,
                        label = "Model Name",
                        value = product.productId,
                        color = MaterialTheme.colorScheme.primary
                    )

                    EnhancedDetailRow(
                        icon = Icons.Outlined.Palette,
                        label = "Color",
                        value = product.colour,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    EnhancedDetailRow(
                        icon = Icons.Outlined.Tag,
                        label = "Brand ID",
                        value = product.brandId,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    EnhancedDetailRow(
                        icon = Icons.Outlined.Numbers,
                        label = "Chassis Number",
                        value = product.chassisNumber,
                        color = MaterialTheme.colorScheme.error
                    )

                    EnhancedDetailRow(
                        icon = Icons.Outlined.Build,
                        label = "Last Service",
                        value = product.lastService,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Owner field
                    if (product.owner.isNotEmpty()) {
                        EnhancedDetailRowWithAction(
                            icon = Icons.Outlined.Person,
                            label = "Owner",
                            value = product.owner,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (product.ownerRef != null) {
                                    showOwnerDialog = true
                                }
                            },
                            enabled = product.ownerRef != null
                        )
                    }

                    // Broker or Middle Man field
                    if (product.brokerOrMiddleMan.isNotEmpty()) {
                        EnhancedDetailRowWithAction(
                            icon = Icons.Outlined.Business,
                            label = "Broker/Middle Man",
                            value = product.brokerOrMiddleMan,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                if (product.brokerOrMiddleManRef != null) {
                                    showBrokerDialog = true
                                }
                            },
                            enabled = product.brokerOrMiddleManRef != null,
                            isLast = true
                        )
                    } else {
                        // If no broker/middle man, mark last service as last
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Documents Section - NOC, RC, Insurance
            if (product.noc.isNotEmpty() || product.rc.isNotEmpty() || product.insurance.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Documents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // NOC Documents
                        if (product.noc.isNotEmpty()) {
                            Text(
                                text = "NOC (No Objection Certificate)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.noc.forEachIndexed { index, url ->
                                    PdfDocumentCard(
                                        title = "NOC Document ${index + 1}",
                                        pdfUrl = url,
                                        onClick = {
                                            openPdf(context, url)
                                        }
                                    )
                                }
                            }
                        }

                        // RC Documents
                        if (product.rc.isNotEmpty()) {
                            Text(
                                text = "RC (Registration Certificate)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.rc.forEachIndexed { index, url ->
                                    PdfDocumentCard(
                                        title = "RC Document ${index + 1}",
                                        pdfUrl = url,
                                        onClick = {
                                            openPdf(context, url)
                                        }
                                    )
                                }
                            }
                        }

                        // Insurance Documents
                        if (product.insurance.isNotEmpty()) {
                            Text(
                                text = "Insurance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.insurance.forEachIndexed { index, url ->
                                    PdfDocumentCard(
                                        title = "Insurance Document ${index + 1}",
                                        pdfUrl = url,
                                        onClick = {
                                            openPdf(context, url)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        
        // Owner (Customer) Details Dialog
        if (showOwnerDialog) {
            CustomerDetailsDialog(
                customer = customer,
                isLoading = isLoadingCustomer,
                error = customerError,
                onDismiss = { showOwnerDialog = false }
            )
        }
        
        // Broker Details Dialog
        if (showBrokerDialog) {
            BrokerDetailsDialog(
                broker = broker,
                isLoading = isLoadingBroker,
                error = brokerError,
                onDismiss = { showBrokerDialog = false }
            )
        }
    }

    private fun openPdf(context: android.content.Context, pdfUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: try to open with browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    @Composable
    fun PdfDocumentCard(
        title: String,
        pdfUrl: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Document",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Tap to open PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = "Open PDF",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    fun QuickInfoCard(
        title: String,
        value: String,
        icon: ImageVector,
        color: Color,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.2f)
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        modifier = Modifier.padding(10.dp),
                        tint = color
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = color.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun EnhancedDetailRow(
        icon: ImageVector,
        label: String,
        value: String,
        color: Color,
        isLast: Boolean = false
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.padding(8.dp),
                        tint = color
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }

    @Composable
    fun EnhancedDetailRowWithAction(
        icon: ImageVector,
        label: String,
        value: String,
        color: Color,
        onClick: () -> Unit,
        enabled: Boolean = true,
        isLast: Boolean = false
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.padding(8.dp),
                        tint = color
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (enabled) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "View Details",
                            tint = color
                        )
                    }
                }
            }

            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ZoomableImageGallery(
        imageUrls: List<String>,
        modifier: Modifier = Modifier
    ) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { imageUrls.size }
        )

        Box(modifier = modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                EnhancedZoomableImage(
                    imageUrl = imageUrls[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 🔘 Page Indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(imageUrls.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 6.dp)
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }

    @Composable
    fun EnhancedZoomableImage(
        imageUrl: String,
        modifier: Modifier = Modifier
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            // ✅ Combined Gesture Handling: Double Tap + Transform
            val gestureModifier = if (scale > 1f) {
                Modifier.pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)

                        // Only allow pan when zoomed in
                        if (newScale > 1f) {
                            val maxX = (size.width * (newScale - 1)) / 2
                            val maxY = (size.height * (newScale - 1)) / 2
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }

                        scale = newScale
                    }
                }
            } else {
                // ⚡ When zoomed out: handle double-tap only (swipe allowed)
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            coroutineScope.launch {
                                scale = 2f // double-tap zoom-in
                            }
                        }
                    )
                }
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .then(gestureModifier)
            )

            // 🔄 Reset Button when zoomed
            if (scale > 1f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Reset",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }


    @Composable
    fun CustomerDetailsDialog(
        customer: Customer?,
        isLoading: Boolean,
        error: String?,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        var selectedImageUrl by remember { mutableStateOf<String?>(null) }
        
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Loading customer details...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        error != null -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Text(
                                        text = "Error",
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
                        customer != null -> {
                            // Profile Photo Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val imageUrl = customer.photoUrl.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 3.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = imageUrl != null) {
                                                selectedImageUrl = imageUrl
                                            }
                                    ) {
                                        if (imageUrl != null) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = customer.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(60.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Contact Information Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Contact Information",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Divider()

                                    // Phone
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Phone Number",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = customer.phone,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Address
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Address",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = customer.address.ifBlank { "No address provided" },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Amount Card
                            val owesCustomer = customer.amount < 0
                            val amountText = "₹${kotlin.math.abs(customer.amount)}"
                            val amountLabel = if (owesCustomer) "Amount to Give (You Owe)" else "Amount to Receive (They Owe)"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (owesCustomer)
                                        Color(0xFFFFE5E5)
                                    else
                                        Color(0xFFE8F9E9)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = amountLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (owesCustomer)
                                                Color(0xFFB71C1C)
                                            else
                                                Color(0xFF1B5E20)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = amountText,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                                color = if (owesCustomer)
                                                Color(0xFFD32F2F)
                                            else
                                                Color(0xFF2E7D32)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                            color = if (owesCustomer)
                                            Color(0xFFD32F2F)
                                        else
                                            Color(0xFF2E7D32),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "₹",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // ID Proof Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Identity Proof",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Divider()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = customer.idProofType.ifBlank { "Not specified" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = customer.idProofNumber.ifBlank { "Not provided" },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (customer.idProofImageUrls.isNotEmpty()) {
                                        Text(
                                            text = "Documents",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            customer.idProofImageUrls.forEachIndexed { index, url ->
                                                PdfDocumentCard(
                                                    title = "ID Proof Document ${index + 1}",
                                                    pdfUrl = url,
                                                    onClick = {
                                                        openPdf(context, url)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No customer data available",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Full Screen Image Viewer Dialog
        selectedImageUrl?.let { imageUrl ->
            Dialog(onDismissRequest = { selectedImageUrl = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { selectedImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Full size image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    @Composable
    fun BrokerDetailsDialog(
        broker: Broker?,
        isLoading: Boolean,
        error: String?,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Broker Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Loading broker details...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        error != null -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Text(
                                        text = "Error",
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
                        broker != null -> {
                            // Profile Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier.size(120.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Business,
                                                contentDescription = null,
                                                modifier = Modifier.size(60.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = broker.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Contact Information Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Contact Information",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Divider()

                                    // Phone
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Phone Number",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = broker.phoneNumber,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Address
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Address",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = broker.address.ifBlank { "No address provided" },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // ID Proof Card
                            if (broker.idProof.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "Identity Proof",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Divider()

                                        Text(
                                            text = "Documents",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            broker.idProof.forEachIndexed { index, url ->
                                                PdfDocumentCard(
                                                    title = "ID Proof Document ${index + 1}",
                                                    pdfUrl = url,
                                                    onClick = {
                                                        openPdf(context, url)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Broker Bill Card
                            if (broker.brokerBill.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "Broker Bills",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Divider()

                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            broker.brokerBill.forEachIndexed { index, url ->
                                                PdfDocumentCard(
                                                    title = "Broker Bill ${index + 1}",
                                                    pdfUrl = url,
                                                    onClick = {
                                                        openPdf(context, url)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No broker data available",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EnhancedNotFoundState(chassisNumber: String) {
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
                        Icons.Default.DirectionsCar,
                        contentDescription = "Vehicle",
                        modifier = Modifier.padding(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Vehicle Not Found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vehicle with chassis number $chassisNumber could not be found in our database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
