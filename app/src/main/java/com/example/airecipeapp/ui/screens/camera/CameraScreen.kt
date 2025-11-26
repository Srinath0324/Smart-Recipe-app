package com.example.airecipeapp.ui.screens.camera

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.airecipeapp.utils.ImageUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionScreen(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    // Handle navigation on success
    LaunchedEffect(uiState) {
        if (uiState is CameraUiState.Success) {
            val scanId = (uiState as CameraUiState.Success).scanResult.id
            onNavigateToEditor(scanId)
            viewModel.resetState()
        }
    }
    
    // Reset state when leaving camera screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }
    
    if (cameraPermissionState.status.isGranted) {
        CameraScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack
        )
    } else {
        PermissionDeniedScreen(
            onRequestPermission = {
                cameraPermissionState.launchPermissionRequest()
            },
            onNavigateBack = onNavigateBack
        )
    }
}

@Composable
fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This app needs camera access to scan grocery lists",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}

@Composable
fun InstructionDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "How to Get Best Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grocery list format image
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        id = com.example.airecipeapp.R.drawable.grocery_list_format
                    ),
                    contentDescription = "Grocery list format example",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Instructions
                Text(
                    text = "For best scanning accuracy:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InstructionItem(
                    icon = Icons.Default.FormatListBulleted,
                    text = "Use the format shown above:\nItem Name - Quantity + Unit"
                )
                
                InstructionItem(
                    icon = Icons.Default.Edit,
                    text = "Write in large, clear handwriting"
                )
                
                InstructionItem(
                    icon = Icons.Default.WbSunny,
                    text = "Ensure good lighting"
                )
                
                InstructionItem(
                    icon = Icons.Default.CenterFocusStrong,
                    text = "Keep the camera steady and focused"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got It!")
            }
        }
    )
}

@Composable
fun InstructionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    
    // Preferences manager for tracking first-time instructions
    val preferencesManager = remember { com.example.airecipeapp.utils.PreferencesManager(context) }
    var showInstructions by remember { 
        mutableStateOf(!preferencesManager.hasCameraInstructionsBeenShown()) 
    }
    
    // Show instruction dialog on first visit
    if (showInstructions) {
        InstructionDialog(
            onDismiss = {
                preferencesManager.markCameraInstructionsShown()
                showInstructions = false
            }
        )
    }
    
    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = ImageUtils.loadBitmapFromUri(context, it)
                bitmap?.let { bmp ->
                    val rotatedBitmap = ImageUtils.rotateBitmapIfNeeded(bmp, it, context)
                    viewModel.processImage(rotatedBitmap)
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading overlay
        if (uiState is CameraUiState.Processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Processing image...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // Error message
        if (uiState is CameraUiState.Error) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.resetState() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text((uiState as CameraUiState.Error).message)
            }
        }
        
        // Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Capture button
                Button(
                    onClick = {
                        val executor = ContextCompat.getMainExecutor(context)
                        val photoFile = File(
                            context.cacheDir,
                            "grocery_${System.currentTimeMillis()}.jpg"
                        )
                        
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        
                        imageCapture?.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch {
                                        val uri = Uri.fromFile(photoFile)
                                        val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
                                        bitmap?.let {
                                            viewModel.processImage(it)
                                        }
                                    }
                                }
                                
                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                }
                            }
                        )
                    },
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Back button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
