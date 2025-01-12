package com.example.landmark.presentation

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.scale
import com.example.landmark.data.MidasModel
import com.example.landmark.data.MobilenetDetector
import com.example.landmark.domain.Classification
import com.example.landmark.domain.LandmarkClassifier
import org.tensorflow.lite.task.vision.detector.Detection

class LandmarkImageAnalyzer(
    private val classifier: LandmarkClassifier,
    private val detector: MobilenetDetector,
    private val midasDetector: MidasModel,
    private val onResults: (List<Classification>) -> Unit,
    private val onBitmapGenerated: (Bitmap) -> Unit,
    private val onDetected: (List<Detection>) -> Unit
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0
//    480 * 640
    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 30 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees
//            val bitmap = image
//                .toBitmap()
//                .centerCrop(480, 480)
//                .scale(300, 300)
            var bitmap = image
                .toBitmap()
            bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
//            val results = classifier.classify(bitmap, rotationDegrees)

            var detections  = detector.classify(bitmap, rotationDegrees)

            val dmap = midasDetector.getDepthMap(bitmap)
//            onResults(results)
            onBitmapGenerated(dmap)
            onDetected(detections)
        }
        frameSkipCounter++

        image.close()
    }
}