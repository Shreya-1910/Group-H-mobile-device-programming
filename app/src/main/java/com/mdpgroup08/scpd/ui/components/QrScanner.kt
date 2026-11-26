package com.mdpgroup08.scpd.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mdpgroup08.scpd.data.qr.QrFrameDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Camera preview that looks for a QR code (R3-S2).
 * Calls [onScanned] once with the QR text, or [onError] if the camera can't start.
 * The caller must already have the CAMERA permission.
 */
@Composable
fun QrScanner(
    onScanned: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnScanned by rememberUpdatedState(onScanned)
    val latestOnError by rememberUpdatedState(onError)
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val analysisThread = Executors.newSingleThreadExecutor()
        val mainThread = ContextCompat.getMainExecutor(context)
        val found = AtomicBoolean(false)
        val disposed = AtomicBoolean(false)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            if (disposed.get()) return@addListener
            try {
                val p = providerFuture.get()
                provider = p
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisThread) { image ->
                    val text = readQr(image)
                    if (text != null && found.compareAndSet(false, true)) {
                        mainThread.execute { if (!disposed.get()) latestOnScanned(text) }
                    }
                }

                val camera = if (p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, camera, preview, analysis)
            } catch (e: Exception) {
                Log.w("SCPD-CheckIn", "Camera couldn't start", e)
                latestOnError("The camera couldn't start. Type the 6-digit code instead.")
            }
        }, mainThread)

        onDispose {
            disposed.set(true)
            provider?.unbindAll()
            analysisThread.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Reads the brightness plane of one frame and looks for a QR code in it. Always closes the frame. */
private fun readQr(image: ImageProxy): String? = try {
    val plane = image.planes[0]
    val rowStride = plane.rowStride
    val bytes = ByteArray(rowStride * image.height)
    val buffer = plane.buffer
    buffer.rewind()
    buffer.get(bytes, 0, minOf(buffer.remaining(), bytes.size))
    QrFrameDecoder.decode(bytes, rowStride, image.width, image.height)
} catch (e: Exception) {
    null
} finally {
    image.close()
}
