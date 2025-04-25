package com.example.blindsight.presentation

import android.graphics.Bitmap
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.blindsight.data.MidasModel
import com.example.blindsight.data.MobilenetDetector
import org.tensorflow.lite.task.vision.detector.Detection

class ImageAnalyzer(
    private val detector: MobilenetDetector,
    private val midasDetector: MidasModel,
    private val onBitmapGenerated: (Bitmap) -> Unit,
    private val onDetected: (List<Detection>) -> Unit,
    private val textToSpeech: TextToSpeech?
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0
    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 30 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees
            var bitmap = image.toBitmap()
            bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

            val detections  = detector.classify(bitmap, rotationDegrees)

            val depthBitmap = midasDetector.getDepthMap(bitmap)

            val squareSize = 64
            val numSquaresPerRow = 256 / squareSize
            val matrix = Array(numSquaresPerRow) { FloatArray(numSquaresPerRow) }

            for (row in 0 until numSquaresPerRow) {
                for (col in 0 until numSquaresPerRow) {
                    var sum = 0f  // Use Float instead of Int for depth values
                    for (i in 0 until squareSize) {
                        for (j in 0 until squareSize) {
                            val pixel = depthBitmap.getPixel(col * squareSize + j, row * squareSize + i)
                            val grayscale = Color.red(pixel)  // Extract grayscale depth
                            sum += grayscale
                        }
                    }
                    val average = sum / (squareSize * squareSize)
                    matrix[row][col] = average
                }
            }

//            Log.d("ImageProcessing", "Average Depth Matrix:")
//            for (row in matrix) {
//                Log.d("ImageProcessing", row.joinToString("  ") { "%.2f".format(it) })
//            }

            if (detections.isNotEmpty()) {
                val scaleFactorX = 256f / 300f
                val scaleFactorY = 256f / 300f

                val xMin = (detections[0].boundingBox.left * scaleFactorX).toInt().coerceIn(0, 255)
                val yMin = (detections[0].boundingBox.top * scaleFactorY).toInt().coerceIn(0, 255)
                val xMax = (detections[0].boundingBox.right * scaleFactorX).toInt().coerceIn(0, 255)
                val yMax = (detections[0].boundingBox.bottom * scaleFactorY).toInt().coerceIn(0, 255)
                val label = detections[0].categories.firstOrNull()?.label ?: "Unknown"
                val avgDepth = getAverageDepthForBoundingBox(xMin, yMin, xMax, yMax, squareSize, matrix)
//                Log.d("ObjectDepth", "Average Depth of Object: $avgDepth")
                val depthCategory = getDepthCategory(avgDepth)
                val direction = getObjectDirection(xMin, xMax)
//                Log.d("ObjectDirection", "Object Position: $direction")
//                Log.d("ObjectInfo", "Object at $direction with Depth Level: $depthCategory")
                val resultText = "$label $direction $depthCategory"
                textToSpeech?.speak(resultText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            onBitmapGenerated(depthBitmap)
            onDetected(detections)
        }
        frameSkipCounter++

        image.close()
    }
    fun getAverageDepthForBoundingBox(
        xMin: Int, yMin: Int, xMax: Int, yMax: Int,
        blockSize: Int, depthMatrix: Array<FloatArray>
    ): Float {
        val numBlocks = depthMatrix.size  // Number of blocks in row/column (4x4)
        val imageSize = numBlocks * blockSize  // Total image size (256 pixels)

        // Convert bounding box coordinates to depth matrix indices
        val startRow = (yMin * numBlocks / imageSize).coerceIn(0, numBlocks - 1)
        val endRow = (yMax * numBlocks / imageSize).coerceIn(0, numBlocks - 1)
        val startCol = (xMin * numBlocks / imageSize).coerceIn(0, numBlocks - 1)
        val endCol = (xMax * numBlocks / imageSize).coerceIn(0, numBlocks - 1)

        // Sum depth values of the selected blocks
        var sum = 0f
        var count = 0
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                sum += depthMatrix[row][col]
                count++
            }
        }

        return if (count > 0) sum / count else 0f
    }

    fun getObjectDirection(xMin: Int, xMax: Int, imageWidth: Int = 256): String {
        val leftEnd = imageWidth / 3  // 85
        val rightStart = 2 * (imageWidth / 3) // 170

        return when {
            xMax <= leftEnd -> "Left"
            xMin >= rightStart -> "Right"
            xMin >= leftEnd && xMax <= rightStart -> "Center"
            xMin >= leftEnd -> "Center-Right"
            xMax <= rightStart -> "Center-Left"
            xMin <= leftEnd && xMax >= rightStart -> "All"
            else -> "Unknown"
        }
    }

    fun getDepthCategory(depth: Float): String {
        return when {
            depth > 130 -> "High"
            depth in 90.0..130.0 -> "Medium"
            else -> "Low"
        }
    }
}