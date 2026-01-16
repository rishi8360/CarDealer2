package com.example.cardealer2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.SettingsViewModel
import com.example.cardealer2.utils.TranslatedText
import com.example.cardealer2.utils.TranslationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val company by viewModel.company.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    // Track back button click to prevent multiple clicks
    var backButtonClicked by remember { mutableStateOf(false) }
    
    // Form fields
    var companyName by remember { mutableStateOf(company.name) }
    var phone by remember { mutableStateOf(company.phone) }
    var nameOfOwner by remember { mutableStateOf(company.nameOfOwner) }
    var phoneNumber by remember { mutableStateOf(company.phoneNumber) }
    var email by remember { mutableStateOf(company.email) }
    var gstin by remember { mutableStateOf(company.gstin) }
    
    // Update text fields when company data changes
    LaunchedEffect(company) {
        companyName = company.name
        phone = company.phone
        nameOfOwner = company.nameOfOwner
        phoneNumber = company.phoneNumber
        email = company.email
        gstin = company.gstin
    }
    
    // Auto-clear success message after 3 seconds
    LaunchedEffect(saveMessage) {
        if (saveMessage != null && !saveMessage!!.startsWith("Failed")) {
            delay(3000)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TranslatedText(
                        englishText = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!backButtonClicked) {
                                backButtonClicked = true
                                navController.popBackStack()
                            }
                        },
                        enabled = !backButtonClicked
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Company Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TranslatedText(
                        englishText = "Company Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Company Name
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Company Name *") },
                        enabled = !isSaving && !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Phone") },
                        enabled = !isSaving && !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Name of Owner
                    OutlinedTextField(
                        value = nameOfOwner,
                        onValueChange = { nameOfOwner = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Name of Owner") },
                        enabled = !isSaving && !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Phone Number (Owner)
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Owner Phone Number") },
                        enabled = !isSaving && !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Email") },
                        enabled = !isSaving && !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // GSTIN
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("GSTIN") },
                        enabled = !isSaving && !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Error message
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Save message
                    if (saveMessage != null) {
                        Text(
                            text = saveMessage!!,
                            color = if (saveMessage!!.startsWith("Failed")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Save Button
                    Button(
                        onClick = {
                            viewModel.updateCompanyData(
                                context,
                                com.example.cardealer2.data.Company(
                                    name = companyName.trim(),
                                    phone = phone.trim(),
                                    nameOfOwner = nameOfOwner.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    email = email.trim(),
                                    gstin = gstin.trim()
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving && !isLoading && companyName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TranslatedText(
                            englishText = if (isSaving) "Saving..." else "Save",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Language Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TranslatedText(
                        englishText = "Language Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TranslatedText(
                                englishText = "Language",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            TranslatedText(
                                englishText = if (isPunjabiEnabled) "Punjabi (ਪੰਜਾਬੀ)" else "English",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isPunjabiEnabled) "ਪੰ" else "EN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Switch(
                                checked = isPunjabiEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        TranslationManager.setPunjabiEnabled(context, enabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TranslatedText(
                        englishText = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TranslatedText(
                        englishText = "The company name will be displayed on the splash screen when the app starts. All other information is stored for your records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

