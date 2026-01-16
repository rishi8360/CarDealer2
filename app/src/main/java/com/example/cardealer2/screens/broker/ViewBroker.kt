package com.example.cardealer2.screens.broker

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.data.Broker
import com.example.cardealer2.ViewModel.ViewBrokersViewModel
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBrokerScreen(
    navController: NavController,
    viewModel: ViewBrokersViewModel = viewModel()
) {
    val brokers by viewModel.brokers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)

    LaunchedEffect(Unit) {
        viewModel.loadBrokers()
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Broker List", isPunjabiEnabled),
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
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = TranslationManager.translate("Clear search", isPunjabiEnabled)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    label = { TranslatedText("Search by name or phone") }
                )

                val filteredBrokers = remember(brokers, searchQuery) {
                    val q = searchQuery.trim()
                    if (q.isEmpty()) brokers else brokers.filter { b ->
                        b.name.contains(q, ignoreCase = true) || b.phoneNumber.contains(q, ignoreCase = true)
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
                                englishText = error ?: "Error loading brokers",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    brokers.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TranslatedText(
                                englishText = "No brokers available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        Spacer(modifier = Modifier.height(24.dp))
                        if (filteredBrokers.isEmpty() && searchQuery.isNotBlank()) {
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
                                items(filteredBrokers) { broker ->
                                    BrokerCard(broker = broker) {
                                        navController.navigate("broker_detail/${broker.brokerId}")
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
fun BrokerCard(
    broker: Broker,
    onClick: (() -> Unit)? = null
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Broker Icon
            val context = LocalContext.current
            val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
                .collectAsState(initial = false)
            
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = TranslationManager.translate("Profile", isPunjabiEnabled),
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Broker Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = broker.name,
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
                        text = broker.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
