package com.example.landmark.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
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
        // Initialize TFLite Interpreter
        val interpreterOptions = Interpreter.Options().apply {
            // Add the GPU Delegate if supported.
            // See -> https://www.tensorflow.org/lite/performance/gpu#android
            if ( CompatibilityList().isDelegateSupportedOnThisDevice ) {
                addDelegate( GpuDelegate( CompatibilityList().bestOptionsForThisDevice ))
            }
            else {
                // Number of threads for computation
                setNumThreads( NUM_THREADS )
            }
        }
        interpreter = Interpreter(FileUtil.loadMappedFile( context, modelFileName ) , interpreterOptions )
    }

    fun getDepthMap( inputImage : Bitmap ): Bitmap {
        return run( inputImage )
    }

    private fun run( inputImage : Bitmap ): Bitmap {
        // Note: The model takes in a RGB image ( of shape ( 256 , 256 , 3 ) ) and
        // outputs a depth map of shape ( 256 , 256 , 1 )
        // Create a tensor of shape ( 1 , inputImageDim , inputImageDim , 3 ) from the given Bitmap.
        // Then perform operations on the tensor as described by `inputTensorProcessor`.
        var inputTensor = TensorImage.fromBitmap( inputImage )

        val t1 = System.currentTimeMillis()
        inputTensor = inputTensorProcessor.process( inputTensor )

        // Output tensor of shape ( 256 , 256 , 1 ) and data type float32
        var outputTensor = TensorBufferFloat.createFixedSize(
            intArrayOf( inputImageDim , inputImageDim , 1 ) , DataType.FLOAT32 )

        // Perform inference on the MiDAS model
        interpreter.run( inputTensor.buffer, outputTensor.buffer )

        // Perform operations on the output tensor as described by `outputTensorProcessor`.
        outputTensor = outputTensorProcessor.process( outputTensor )
//        Logger.logInfo( "MiDaS inference speed: ${System.currentTimeMillis() - t1}")

        // Create a Bitmap from the depth map which will be displayed on the screen.
        var res: Bitmap = byteBufferToBitmap( outputTensor.floatArray , inputImageDim )
        Log.i("midas", res.toString())
        return res
    }

    private fun byteBufferToBitmap( imageArray : FloatArray , imageDim : Int ) : Bitmap {
        val pixels = imageArray.map { it.toInt() }.toIntArray()
        val bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.RGB_565 );
        for ( i in 0 until imageDim ) {
            for ( j in 0 until imageDim ) {
                val p = pixels[ i * imageDim + j ]
                bitmap.setPixel( j , i , Color.rgb( p , p , p ))
            }
        }
        return bitmap
    }

    class MinMaxScalingOp : TensorOperator {
        override fun apply( input : TensorBuffer?): TensorBuffer {
            val values = input!!.floatArray
            // Compute min and max of the output
            val max = values.maxOrNull()!!
            val min = values.minOrNull()!!
            for ( i in values.indices ) {
                // Normalize the values and scale them by a factor of 255
                var p = ((( values[ i ] - min ) / ( max - min )) * 255).toInt()
                if ( p < 0 ) {
                    p += 255
                }
                values[ i ] = p.toFloat()
            }
            // Convert the normalized values to the TensorBuffer and load the values in it.
            val output = TensorBufferFloat.createFixedSize( input.shape , DataType.FLOAT32 )
            output.loadArray( values )
            return output
        }
    }
}