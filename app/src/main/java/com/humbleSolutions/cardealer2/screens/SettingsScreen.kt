package com.humbleSolutions.cardealer2.screens

import androidx.compose.foundation.clickable
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

import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.humbleSolutions.cardealer2.ViewModel.SettingsViewModel
import com.humbleSolutions.cardealer2.utils.TranslatedText
import com.humbleSolutions.cardealer2.utils.TranslationManager
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
                                com.humbleSolutions.cardealer2.data.Company(
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
                    
                    TranslatedText(
                        englishText = "Select your preferred language for the app interface.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // Radio Button Options
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isPunjabiEnabled) {
                                        scope.launch {
                                            TranslationManager.setPunjabiEnabled(context, false)
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = !isPunjabiEnabled,
                                onClick = {
                                    if (isPunjabiEnabled) {
                                        scope.launch {
                                            TranslationManager.setPunjabiEnabled(context, false)
                                        }
                                    }
                                }
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "EN",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TranslatedText(
                                    englishText = "English",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isPunjabiEnabled) {
                                        scope.launch {
                                            TranslationManager.setPunjabiEnabled(context, true)
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isPunjabiEnabled,
                                onClick = {
                                    if (!isPunjabiEnabled) {
                                        scope.launch {
                                            TranslationManager.setPunjabiEnabled(context, true)
                                        }
                                    }
                                }
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "ਪੰ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TranslatedText(
                                    englishText = "Punjabi",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                            }
                        }
                    }
                }
            }
            
            // Price Password Section
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
                        englishText = "Price Visibility Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    var newPassword by remember { mutableStateOf("") }
                    var confirmPassword by remember { mutableStateOf("") }
                    var showPassword by remember { mutableStateOf(false) }
                    var showCurrentPasswordDialog by remember { mutableStateOf(false) }
                    var currentPasswordInput by remember { mutableStateOf("") }
                    var currentPasswordError by remember { mutableStateOf<String?>(null) }
                    
                    val isPasswordSaving by viewModel.isPasswordSaving.collectAsState()
                    val passwordSaveMessage by viewModel.passwordSaveMessage.collectAsState()
                    val storedPassword by viewModel.password.collectAsState()
                    
                    // Load password when screen opens
                    LaunchedEffect(Unit) {
                        viewModel.fetchPassword(context)
                    }
                    
                    // Auto-clear message
                    LaunchedEffect(passwordSaveMessage) {
                        if (passwordSaveMessage != null && !passwordSaveMessage!!.startsWith("Failed")) {
                            delay(3000)
                            viewModel.clearPasswordMessage()
                        }
                    }
                    
                    TranslatedText(
                        englishText = "Set a password to protect vehicle prices. Users will need to enter this password to view purchase and selling prices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("New Password") },
                        enabled = !isPasswordSaving,
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Confirm Password") },
                        enabled = !isPasswordSaving,
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (passwordSaveMessage != null) {
                        Text(
                            text = passwordSaveMessage!!,
                            color = if (passwordSaveMessage!!.startsWith("Failed")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Button(
                        onClick = {
                            // If password exists, show dialog to verify current password
                            if (storedPassword.isNotEmpty()) {
                                showCurrentPasswordDialog = true
                                currentPasswordInput = ""
                                currentPasswordError = null
                            } else {
                                // No password exists, update directly
                                viewModel.updatePassword(context, newPassword, confirmPassword)
                                newPassword = ""
                                confirmPassword = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPasswordSaving && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPasswordSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TranslatedText(
                            englishText = if (isPasswordSaving) "Updating..." else "Update Password",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Current Password Verification Dialog
                    if (showCurrentPasswordDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showCurrentPasswordDialog = false
                                currentPasswordInput = ""
                                currentPasswordError = null
                            },
                            title = {
                                TranslatedText(
                                    englishText = "Verify Current Password",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    TranslatedText(
                                        englishText = "Please enter your current password to continue.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    OutlinedTextField(
                                        value = currentPasswordInput,
                                        onValueChange = {
                                            currentPasswordInput = it
                                            currentPasswordError = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { TranslatedText("Current Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        isError = currentPasswordError != null,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (currentPasswordError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentPasswordError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            if (currentPasswordInput.isBlank()) {
                                                currentPasswordError = "Password cannot be empty"
                                                return@launch
                                            }
                                            
                                            val isValid = viewModel.verifyCurrentPasswordAsync(context, currentPasswordInput)
                                            if (isValid) {
                                                // Password is correct, proceed with update
                                                showCurrentPasswordDialog = false
                                                viewModel.updatePassword(context, newPassword, confirmPassword, currentPasswordInput)
                                                newPassword = ""
                                                confirmPassword = ""
                                                currentPasswordInput = ""
                                                currentPasswordError = null
                                            } else {
                                                currentPasswordError = "Incorrect password"
                                            }
                                        }
                                    }
                                ) {
                                    TranslatedText("Verify")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showCurrentPasswordDialog = false
                                        currentPasswordInput = ""
                                        currentPasswordError = null
                                    }
                                ) {
                                    TranslatedText("Cancel")
                                }
                            }
                        )
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

