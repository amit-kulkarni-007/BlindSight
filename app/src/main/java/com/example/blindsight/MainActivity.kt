package com.example.blindsight

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blindsight.data.MidasModel
import com.example.blindsight.data.MobilenetDetector
import com.example.blindsight.presentation.CameraPreview
import com.example.blindsight.presentation.ImageAnalyzer
import com.example.blindsight.receiver.BatteryLevelReceiver
import com.example.blindsight.presentation.ContactScreen
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
        val intentFilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(BatteryLevelReceiver(), intentFilter)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0)
        }

        setContent {
            LandmarkTheme {
                val navController = rememberNavController() // Create a NavController

                NavHost(navController = navController, startDestination = "camera") {
                    composable("camera") {
                        CameraScreen(navController, textToSpeech) // Pass NavController to Camera screen
                    }
                    composable("contacts") {
                        ContactScreen(navController) // Pass NavController to Contacts screen
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
    }

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}


@Composable
fun CameraScreen(navController: NavController, textToSpeech: TextToSpeech?) {
    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }
    var detections by remember { mutableStateOf(emptyList<Detection>()) }
    val context = LocalContext.current // Hoist context retrieval

    val analyzer = remember {
        ImageAnalyzer(
            detector = MobilenetDetector(
                context = context.applicationContext
            ),
            midasDetector = MidasModel(
                context = context.applicationContext
            ),
            onBitmapGenerated = { newBitmap -> bitmapState = newBitmap },
            onDetected = { results -> detections = results },
            textToSpeech = textToSpeech
        )
    }

    val controller = remember {
        LifecycleCameraController(context.applicationContext).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context.applicationContext),
                analyzer
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(controller, Modifier.fillMaxSize())
        bitmapState?.let { bitmap ->
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels
            val matrix = Matrix()
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            Image(
                bitmap = rotatedBitmap.asImageBitmap(),
                contentDescription = "Captured Frame",
                modifier = Modifier.fillMaxWidth()
            )
        }

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
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
//                drawRect(
//                    color = Color.Red,
//                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(
//                        width = right - left,
//                        height = bottom - top
//                    ),
//                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
//                )
            }
        }

        Button(
            onClick = { navController.navigate("contacts") }, // Navigate to contacts
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Manage Contacts")
        }
    }
}