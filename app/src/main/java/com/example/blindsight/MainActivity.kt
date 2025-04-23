package com.example.blindsight

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.blindsight.data.MidasModel
import com.example.blindsight.data.MobilenetDetector
import com.example.blindsight.presentation.CameraPreview
import com.example.blindsight.presentation.ImageAnalyzer
import com.example.blindsight.ui.theme.LandmarkTheme
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*

class MainActivity : ComponentActivity() {
    private var textToSpeech: TextToSpeech? = null
    private var stepsFound = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
        if(!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        setContent {
            LandmarkTheme {
                var bitmapState by remember {
                    mutableStateOf<Bitmap?>(null)
                }
                var detections by remember {
                    mutableStateOf(emptyList<Detection>())
                }
                val analyzer = remember {
                    ImageAnalyzer(
                        detector = MobilenetDetector(
                            context = applicationContext
                        ),
                        midasDetector = MidasModel(
                            context = applicationContext
                        ),
                        onBitmapGenerated = { newBitmap ->
                            bitmapState = newBitmap
                        },
                        onDetected = { results ->
//                            detections = results
//                            if(detections.isNotEmpty()){
//                                val label = detections[0].categories.firstOrNull()?.label ?: "Unknown"
//                                textToSpeech?.speak(
//                                    label + " " + stepsFound,
//                                    TextToSpeech.QUEUE_ADD,
//                                    null,
//                                    null
//                                )
//                            }
                        },
                        textToSpeech = textToSpeech
                    )
                }
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(applicationContext),
                            analyzer
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CameraPreview(controller, Modifier.fillMaxSize())

                    bitmapState?.let { bitmap ->
                        val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels
                        val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels

                        val matrix = Matrix()

//                        matrix.postRotate(90f)

                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrix,
                            true
                        )

//                        val scaledBitmap = Bitmap.createScaledBitmap(
//                            rotatedBitmap,
//                            screenWidth,
//                            screenHeight,
//                            true
//                        )

                        Image(
                            bitmap = rotatedBitmap.asImageBitmap(),
                            contentDescription = "Captured Frame",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val inputWidth = 300f
                        val inputHeight = 300f
                        val scaleX = canvasWidth / inputWidth
                        val scaleY = canvasHeight / inputHeight
                        detections.forEach { detection ->
                            val boundingBox = detection.boundingBox
                            val left = boundingBox.left * scaleX
                            val top = boundingBox.top * scaleY
                            val right = boundingBox.right * scaleX
                            val bottom = boundingBox.bottom * scaleY
                            drawRect(
                                color = Color.Red,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(
                                    width = right - left,
                                    height = bottom - top
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 4f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
    }
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

}
