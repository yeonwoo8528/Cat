package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import android.graphics.Color
import android.util.Log

class Classifier(private var context: Context, private val modelName: String) {
    private lateinit var model: Model
    private lateinit var inputImage: TensorImage
    private lateinit var outputBuffer: TensorBuffer
    private var modelInputChannel = 0
    private var modelInputWidth = 0
    private var modelInputHeight = 0
    private val labels = mutableListOf<String>()

    fun init() {
        model = Model.createModel(context, modelName)
        initModelShape()
        labels.addAll(FileUtil.loadLabels(context, LABEL_FILE))
    }

    private fun initModelShape() {
        val inputTensor = model.getInputTensor(0)
        val inputShape = inputTensor.shape()
        modelInputChannel = inputShape[0]
        modelInputWidth = inputShape[1]
        modelInputHeight = inputShape[2]

        inputImage = TensorImage(inputTensor.dataType())

        val outputTensor = model.getOutputTensor(0)
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
    }

    fun classify(image: Bitmap): Pair<String, Float> {
        inputImage = loadImage(image)
        val inputs = arrayOf<Any>(inputImage.buffer)
        val outputs = mutableMapOf<Int, Any>()
        outputs[0] = outputBuffer.buffer.rewind()
        model.run(inputs, outputs)
        val output = TensorLabel(labels, outputBuffer).mapWithFloatValue
        return argmax(output)
    }

    private fun convertRGBtoBGR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val convertedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = bitmap.getPixel(x, y)
                val red = Color.red(pixelColor)
                val green = Color.green(pixelColor)
                val blue = Color.blue(pixelColor)

                val bgrPixelColor = Color.rgb(blue, green, red)

                convertedBitmap.setPixel(x, y, bgrPixelColor)
            }
        }

        return convertedBitmap
    }

    private fun loadImage(bitmap: Bitmap): TensorImage {
        val argbBitmap = convertBitmapToARGB8888(bitmap)

        val bgrBitmap = convertRGBtoBGR(argbBitmap)
        inputImage.load(bgrBitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputWidth, modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        return imageProcessor.process(inputImage)
    }

    private fun convertBitmapToARGB8888(bitmap: Bitmap) = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    private fun argmax(map: Map<String, Float>) =
        map.entries.maxByOrNull { it.value }?.let {
            it.key to it.value
        } ?: ("" to 0f)

    fun finish() {
        if (::model.isInitialized) model.close()
    }

    companion object {
        const val LABEL_FILE = "YN.txt"
    }
}