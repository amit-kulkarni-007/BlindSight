package com.example.blindsight.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import androidx.core.graphics.get
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat

class MidasModel(
    private val context: Context
) {
    private val modelFileName = "midas.tflite"
    private lateinit var interpreter : Interpreter
    private val NUM_THREADS = 4
    private val inputImageDim = 256
    private val mean = floatArrayOf( 123.675f ,  116.28f ,  103.53f )
    private val std = floatArrayOf( 58.395f , 57.12f ,  57.375f )

    private val inputTensorProcessor = ImageProcessor.Builder()
        .add( ResizeOp( inputImageDim , inputImageDim , ResizeOp.ResizeMethod.BILINEAR ) )
        .add( NormalizeOp( mean , std ) )
        .build()

    private val outputTensorProcessor = TensorProcessor.Builder()
        .add( MinMaxScalingOp() )
        .build()

    init {
        val interpreterOptions = Interpreter.Options().apply {
            if ( CompatibilityList().isDelegateSupportedOnThisDevice ) {
                addDelegate( GpuDelegate( CompatibilityList().bestOptionsForThisDevice ))
            }
            else {
                setNumThreads( NUM_THREADS )
            }
        }
        interpreter = Interpreter(FileUtil.loadMappedFile( context, modelFileName ) , interpreterOptions )
    }

    fun getDepthMap( inputImage : Bitmap ): Bitmap {
        return run( inputImage )
    }

    private fun run( inputImage : Bitmap ): Bitmap {
        val mat = Matrix()
        mat.postRotate(90f)
        val rotatedInputImage = Bitmap.createBitmap(inputImage, 0, 0, inputImage.width, inputImage.height, mat, true)

        var inputTensor = TensorImage.fromBitmap( rotatedInputImage )

        val t1 = System.currentTimeMillis()
        inputTensor = inputTensorProcessor.process( inputTensor )

        var outputTensor = TensorBufferFloat.createFixedSize(
            intArrayOf( inputImageDim , inputImageDim , 1 ) , DataType.FLOAT32 )

        interpreter.run( inputTensor.buffer, outputTensor.buffer )

        outputTensor = outputTensorProcessor.process( outputTensor )

        val bitmap = byteBufferToBitmap(outputTensor.floatArray, inputImageDim)

        return bitmap
    }

    private fun byteBufferToBitmap(imageArray: FloatArray, imageDim: Int): Bitmap {
        val max = imageArray.maxOrNull()!!
        val min = imageArray.minOrNull()!!

        val bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.ARGB_8888)
        for (i in 0 until imageDim) {
            for (j in 0 until imageDim) {
                val index = i * imageDim + j
                val depthValue = ((imageArray[index] - min) / (max - min) * 255).toInt() // Normalize
                val grayscaleColor = Color.rgb(depthValue, depthValue, depthValue)
                bitmap.setPixel(j, i, grayscaleColor)
            }
        }
        return bitmap
    }


    class MinMaxScalingOp : TensorOperator {
        override fun apply( input : TensorBuffer?): TensorBuffer {
            val values = input!!.floatArray
            val max = values.maxOrNull()!!
            val min = values.minOrNull()!!
            for ( i in values.indices ) {
                var p = ((( values[ i ] - min ) / ( max - min )) * 255).toInt()
                if ( p < 0 ) {
                    p += 255
                }
                values[ i ] = p.toFloat()
            }
            val output = TensorBufferFloat.createFixedSize( input.shape , DataType.FLOAT32 )
            output.loadArray( values )
            return output
        }
    }
}