package com.example.blindsight.presentation

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.blindsight.data.MidasModel
import com.example.blindsight.data.MobilenetDetector
import org.tensorflow.lite.task.vision.detector.Detection

class ImageAnalyzer(
    private val detector: MobilenetDetector,
    private val midasDetector: MidasModel,
    private val onBitmapGenerated: (Bitmap) -> Unit,
    private val onDetected: (List<Detection>) -> Unit
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0
//    480 * 640
    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 20 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees
            var bitmap = image
                .toBitmap()
            bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

            var detections  = detector.classify(bitmap, rotationDegrees)

            val dmap = midasDetector.getDepthMap(bitmap)
            onBitmapGenerated(dmap)
            onDetected(detections)
        }
        frameSkipCounter++

        image.close()
    }
}