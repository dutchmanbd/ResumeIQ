package com.dutchman.resumeiq.presentation.features.scan.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.asImageBitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dutchman.resumeiq.R
import com.dutchman.resumeiq.presentation.features.scan.ScanViewModel
import com.dutchman.resumeiq.presentation.features.scan.ScanEvent
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutchman.resumeiq.domain.util.rememberSharedBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import androidx.compose.ui.text.style.TextAlign

private enum class ScannerStep {
    Camera,
    Edit,
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ScannerScreen(
    navigator: DestinationsNavigator,
    navController: NavController,
) {
    val sharedScanViewModel: ScanViewModel = hiltViewModel(navController.rememberSharedBackStackEntry())
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    var step by remember { mutableStateOf(ScannerStep.Camera) }
    var editBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var quad by remember { mutableStateOf(QuadNorm.defaultInset()) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var capturing by remember { mutableStateOf(false) }
    var processingNext by remember { mutableStateOf(false) }
    var liveDetectedQuad by remember { mutableStateOf<QuadNorm?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(cameraPermission.status.isGranted, step) {
        val provider = ProcessCameraProvider.getInstance(context).await()
        cameraProvider = provider
        if (!cameraPermission.status.isGranted || step != ScannerStep.Camera) {
            provider.unbindAll()
            return@LaunchedEffect
        }
        provider.unbindAll()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        runCatching {
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalysis,
            )
        }
        
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val bmp = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedBmp = if (rotationDegrees != 0) {
                BitmapPerspective.rotateBitmap(bmp, rotationDegrees.toFloat())
            } else {
                bmp
            }
            if (bmp != rotatedBmp) {
                bmp.recycle()
            }
            val detected = DocumentQuadDetector.detectQuad(rotatedBmp)
            rotatedBmp.recycle()
            scope.launch(Dispatchers.Main) {
                liveDetectedQuad = detected
            }
            imageProxy.close()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(step, editBitmap) {
        if (step == ScannerStep.Edit && editBitmap == null) {
            step = ScannerStep.Camera
        }
    }

    BackHandler(enabled = step == ScannerStep.Edit) {
        editBitmap?.recycle()
        editBitmap = null
        quad = QuadNorm.defaultInset()
        step = ScannerStep.Camera
    }

    fun capturePhoto() {
        if (!cameraPermission.status.isGranted || capturing) return
        capturing = true
        val dir = File(context.cacheDir, "camera_capture").apply { mkdirs() }
        val outFile = File(dir, "md_scan_cap_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(outFile).build()
        imageCapture.takePicture(
            opts,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    scope.launch(Dispatchers.Default) {
                        val bmp = BitmapFactory.decodeFile(outFile.absolutePath)
                        if (bmp == null) {
                            withContext(Dispatchers.Main) { capturing = false }
                            return@launch
                        }
                        val detected = DocumentQuadDetector.detectQuad(bmp)
                        withContext(Dispatchers.Main) {
                            capturing = false
                            editBitmap?.recycle()
                            editBitmap = bmp
                            quad = (detected ?: QuadNorm.defaultInset()).clamped()
                            step = ScannerStep.Edit
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    scope.launch(Dispatchers.Main) {
                        capturing = false
                    }
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (step == ScannerStep.Camera) {
                            stringResource(R.string.scanner_title_camera)
                        } else {
                            stringResource(R.string.scanner_title_image)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (step) {
                                ScannerStep.Camera -> navigator.popBackStack()
                                ScannerStep.Edit -> {
                                    editBitmap?.recycle()
                                    editBitmap = null
                                    quad = QuadNorm.defaultInset()
                                    step = ScannerStep.Camera
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.scanner_cd_back),
                        )
                    }
                },
                actions = {
                    if (step == ScannerStep.Edit) {
                        IconButton(
                            enabled = !processingNext,
                            onClick = {
                                editBitmap?.let { current ->
                                    processingNext = true
                                    scope.launch(Dispatchers.Default) {
                                        val warped = BitmapPerspective.warpQuadToRectangle(current, quad)
                                        if (warped == null) {
                                            withContext(Dispatchers.Main) { processingNext = false }
                                            return@launch
                                        }
                                        withContext(Dispatchers.Main) {
                                            sharedScanViewModel.onEvent(ScanEvent.OnScannedImageReady(warped))
                                            processingNext = false
                                            editBitmap?.recycle()
                                            editBitmap = null
                                            navigator.popBackStack()
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.scanner_cd_apply),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                flashMode = if (flashMode == ImageCapture.FLASH_MODE_ON) {
                                    ImageCapture.FLASH_MODE_OFF
                                } else {
                                    ImageCapture.FLASH_MODE_ON
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON) {
                                    Icons.Default.FlashOn
                                } else {
                                    Icons.Default.FlashOff
                                },
                                contentDescription = stringResource(R.string.scanner_cd_flash),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        when (step) {
            ScannerStep.Camera -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!cameraPermission.status.isGranted) {
                        Text(
                            text = stringResource(R.string.scanner_need_camera_permission),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(28.dp),
                                ),
                        ) {
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (liveDetectedQuad != null) {
                                val overlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val q = liveDetectedQuad!!
                                    val w = size.width
                                    val h = size.height
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(q.topLeft.x * w, q.topLeft.y * h)
                                        lineTo(q.topRight.x * w, q.topRight.y * h)
                                        lineTo(q.bottomRight.x * w, q.bottomRight.y * h)
                                        lineTo(q.bottomLeft.x * w, q.bottomLeft.y * h)
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = overlayColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 8f,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                            if (capturing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(4.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(
                                    enabled = !capturing,
                                    onClick = { capturePhoto() },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)),
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            ScannerStep.Edit -> {
                val bmp = editBitmap
                if (bmp != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        ScannerCropEditor(
                            bitmap = bmp,
                            quad = quad,
                            onQuadChange = { quad = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ScannerBottomAction(
                                label = stringResource(R.string.scanner_rotate),
                                onClick = {
                                    editBitmap?.let { current ->
                                        val rotated = BitmapPerspective.rotateBitmap(current, -90f)
                                        current.recycle()
                                        editBitmap = rotated
                                        quad = QuadNorm.defaultInset()
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.RotateLeft,
                                        contentDescription = stringResource(R.string.scanner_cd_rotate_ccw),
                                    )
                                },
                            )
                            ScannerBottomAction(
                                label = stringResource(R.string.scanner_rotate),
                                onClick = {
                                    editBitmap?.let { current ->
                                        val rotated = BitmapPerspective.rotateBitmap(current, 90f)
                                        current.recycle()
                                        editBitmap = rotated
                                        quad = QuadNorm.defaultInset()
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.RotateRight,
                                        contentDescription = stringResource(R.string.scanner_cd_rotate_cw),
                                    )
                                },
                            )
                            ScannerBottomAction(
                                label = stringResource(R.string.scanner_crop),
                                enabled = !processingNext,
                                onClick = {
                                    editBitmap?.let { current ->
                                        processingNext = true
                                        scope.launch(Dispatchers.Default) {
                                            val warped = BitmapPerspective.warpQuadToRectangle(current, quad)
                                            withContext(Dispatchers.Main) {
                                                if (warped != null) {
                                                    val old = editBitmap
                                                    editBitmap = warped
                                                    if (old != warped) {
                                                        old?.recycle()
                                                    }
                                                    quad = QuadNorm.defaultInset()
                                                }
                                                processingNext = false
                                            }
                                        }
                                    }
                                },
                                selected = false,
                                icon = {
                                    Icon(
                                        Icons.Default.Crop,
                                        contentDescription = stringResource(R.string.scanner_cd_crop),
                                    )
                                },
                            )
                            ScannerBottomAction(
                                label = stringResource(R.string.scanner_next),
                                enabled = !processingNext,
                                onClick = {
                                    editBitmap?.let { current ->
                                        processingNext = true
                                        scope.launch(Dispatchers.Default) {
                                            val warped = BitmapPerspective.warpQuadToRectangle(current, quad)
                                            if (warped == null) {
                                                withContext(Dispatchers.Main) { processingNext = false }
                                                return@launch
                                            }
                                            withContext(Dispatchers.Main) {
                                                sharedScanViewModel.onEvent(ScanEvent.OnScannedImageReady(warped))
                                                processingNext = false
                                                editBitmap?.recycle()
                                                editBitmap = null
                                                navigator.popBackStack()
                                            }
                                        }
                                    }
                                },
                                icon = {
                                    if (processingNext) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = stringResource(R.string.scanner_cd_next),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerBottomAction(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .border(
                    width = 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            ) {
                icon()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
