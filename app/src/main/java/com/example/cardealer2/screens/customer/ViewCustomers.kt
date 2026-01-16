package com.example.cardealer2.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.cardealer2.data.Customer
import com.example.cardealer2.ViewModel.ViewCustomersViewModel
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewCustomerScreen(
    navController: NavController,
    viewModel: ViewCustomersViewModel= viewModel()
) {
    val customers by viewModel.customers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }

    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Customer List", isPunjabiEnabled),
                navController = navController
            )
        }
    ) { paddingValues ->

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize().padding(
                        paddingValues
                    )
                    .padding(horizontal = 20.dp)
            ) {

				var searchQuery by rememberSaveable { mutableStateOf("") }
				Spacer(modifier = Modifier.height(12.dp))
				OutlinedTextField(
					value = searchQuery,
					onValueChange = { searchQuery = it },
					modifier = Modifier.fillMaxWidth(),
					leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
					trailingIcon = {
						if (searchQuery.isNotEmpty()) {
							IconButton(onClick = { searchQuery = "" }) {
								Icon(Icons.Default.Close, contentDescription = TranslationManager.translate("Clear search", isPunjabiEnabled))
							}
						}
					},
					singleLine = true,
					label = { TranslatedText("Search by name or phone") }
				)

				val filteredCustomers = remember(customers, searchQuery) {
					val q = searchQuery.trim()
					if (q.isEmpty()) customers else customers.filter { c ->
						c.name.contains(q, ignoreCase = true) || c.phone.contains(q, ignoreCase = true)
					}
				}

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TranslatedText(
                                englishText = error ?: "Error loading customers",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    customers.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TranslatedText(
                                englishText = "No customers available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

				else -> {
					Spacer(modifier = Modifier.height(24.dp))
					if (filteredCustomers.isEmpty() && searchQuery.isNotBlank()) {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							TranslatedText(
								englishText = "No results found",
								style = MaterialTheme.typography.bodyLarge,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					} else {
						LazyColumn(
							modifier = Modifier.fillMaxSize(),
							verticalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(bottom = 16.dp)
						) {
							items(filteredCustomers) { customer ->
								CustomerCard(customer = customer) {
									navController.navigate("customer_detail/${customer.customerId}")
								}
							}
						}
					}
				}
                }
            }
        }
    }

}

@Composable
fun CustomerCard(
    customer: Customer,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Customer Photo
            if (customer.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = customer.photoUrl.first(),
                    contentDescription = customer.name,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Customer Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val owesCustomer = customer.amount < 0
                val amountColor = if (owesCustomer)
                    Color(0xFFFF6B6B) // owe money
                else
                    Color(0xFF51C272) // to be received

                val displayAmount = kotlin.math.abs(customer.amount)

                val amountText = if (owesCustomer) {
                    "${TranslationManager.translate("You Owe ₹", isPunjabiEnabled)}$displayAmount"
                } else {
                    "${TranslationManager.translate("To Receive ₹", isPunjabiEnabled)}$displayAmount"
                }
                
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


