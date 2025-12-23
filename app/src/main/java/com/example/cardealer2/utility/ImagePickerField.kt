package com.example.cardealer2.utility


import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.TorchState
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImagePickerField(
    label: String,
    images: List<Uri>,
    onImagesChanged: (List<Uri>) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddPhotoDialogBox by remember { mutableStateOf(false) }

    val controller = remember {
        LifecycleCameraController(context.applicationContext).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE
            )
        }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<String?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)


    // Use Photo Picker for Android 13+ (API 33+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onImagesChanged(images + it)
        }
    }

    // Fallback for older Android versions
    val legacyImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onImagesChanged(images + it)
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = "${images.size} image(s) selected",
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            isError = errorMessage != null,
            trailingIcon = {
                Row(){
                    IconButton(onClick = {
                        // Use Photo Picker for Android 13+ (handles Android 14+ Selected Photos Access)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            legacyImagePickerLauncher.launch("image/*")
                        }
                    }) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        // Use Photo Picker for Android 13+ (handles Android 14+ Selected Photos Access)
                        cameraPermissionState.launchPermissionRequest()
                        showAddPhotoDialogBox = true
                    }) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Add Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        if (cameraPermissionState.status.isGranted && showAddPhotoDialogBox) {
            AddPhotoDialog(
                controller,
                onDismiss = { showAddPhotoDialogBox = false },
                onImageCaptured = {
                    onImagesChanged(images+it)
                    showAddPhotoDialogBox = false
                })
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Selected Images (${images.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(images) { imageUrl ->
                    ImagePreviewItem(
                        imageUrl = imageUrl.toString(),
                        onDeleteClick = {
                            imageToDelete = imageUrl.toString()
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && imageToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                imageToDelete = null
            },
            icon = {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Image?") },
            text = { Text("Are you sure you want to remove this image?") },
            confirmButton = {
                Button(
                    onClick = {
                        imageToDelete?.let { url ->
                            // ✅ Convert String back to Uri for comparison
                            val uriToDelete = url.toUri()
                            onImagesChanged(images.filter { it != uriToDelete })
                        }
                        showDeleteDialog = false
                        imageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    imageToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
private fun ImagePreviewItem(
    imageUrl: String,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Selected Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clickable(onClick = onDeleteClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                modifier = Modifier.padding(4.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun AddPhotoDialog(
    controller: LifecycleCameraController,
    onDismiss: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.controller = controller
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar with Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Bottom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error Message
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Capture Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            errorMessage = null
                            takePhoto(
                                controller = controller,
                                context = context,
                                onPhotoTaken = { uri ->
                                    onImageCaptured(uri)
                                    onDismiss()
                                },
                                onError = { exception ->
                                    isCapturing = false
                                    errorMessage = "Failed to capture photo"
                                    Log.e("AddPhotoDialog", "Photo capture error", exception)
                                }
                            )
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (isCapturing) Color.Gray else Color.White)
                    )

                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Capture Label
                Text(
                    text = if (isCapturing) "Capturing..." else "Tap to Capture",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }


        }
    }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            controller.unbind()
        }
    }
}

fun takePhoto(
    controller: LifecycleCameraController,
    context: Context,
    onPhotoTaken: (Uri) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        // Create a temporary file to store the photo
        val photoFile = File(
            context.externalCacheDir,
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        // Use the built-in takePicture() from LifecycleCameraController
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    try {
                        // Convert ImageProxy to Bitmap
                        val bitmap = image.toBitmap() // extension function from camera-core-ktx
                        val matrix = Matrix().apply {
                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                        }
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrix,
                            true
                        )

                        // Save bitmap to file
                        photoFile.outputStream().use { out ->
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }

                        val savedUri = Uri.fromFile(photoFile)
                        onPhotoTaken(savedUri)

                    } catch (e: Exception) {
                        onError(e)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    onError(exception)
                }
            }
        )
    } catch (e: Exception) {
        onError(e)
    }
}

