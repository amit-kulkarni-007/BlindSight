package com.example.landmark.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import com.example.landmark.domain.Classification
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class MobilenetDetector(
    private val context: Context,
    private val maxResults: Int = 5,
    private val threshold: Float = 0.5f
) {
    private var objectDetector: ObjectDetector? = null

    private fun setupDetector() {
        val baseOptions = BaseOptions.builder()
            .setNumThreads(2)
            .build()
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .setScoreThreshold(threshold)
            .build()

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                context,
                "mobilenetssd.tflite",
                options
            )
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun classify(bitmap: Bitmap, rotation: Int): List<Detection>{
        if(objectDetector == null) {
            setupDetector()
        }

//        val imageProcessor = ImageProcessor.Builder().build()
//        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-rotation / 90))
                .build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))


//        val imageProcessingOptions = ImageProcessingOptions.builder()
//            .setOrientation(getOrientationFromRotation(rotation))
//            .build()

        val results = objectDetector?.detect(tensorImage)
        for(result in results!!) {
            Log.i("detect", result.toString())
        }
        return results
//        return results?.flatMap { classications ->
//            classications.categories.map { category ->
//                Classification(
//                    name = category.displayName,
//                    score = category.score
//                )
//            }
//        }?.distinctBy { it.name } ?: emptyList()
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when(rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }
}