package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myapplication.databinding.ActivityGalleryBinding
import java.io.IOException
import java.util.*

class GalleryActivity : ComponentActivity() {
    private val binding by lazy { ActivityGalleryBinding.inflate(layoutInflater, null, false) }
    private lateinit var modelManager: ModelManager

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { url: Uri? ->
        val selectedImage = url ?: return@registerForActivityResult
        var bitmap: Bitmap? = null
        try {
            bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val src = ImageDecoder.createSource(contentResolver, selectedImage)
                ImageDecoder.decodeBitmap(src)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            }
        } catch (exception: IOException) {
            Toast.makeText(this, "Can not load image!!", Toast.LENGTH_SHORT).show()
        }

        bitmap?.let {
            val output = modelManager.classifyImage(bitmap)

            val resultStr = if (output.isNotEmpty()) {
                val resultLines = mutableListOf<String>()
                for ((disease, probability) in output) {
                    val line = String.format(
                        Locale.KOREA,
                        "%s 확률 : %.3f%%",
                        disease,
                        probability * 100
                    )
                    resultLines.add(line)
                }
                resultLines.joinToString("\n")
            } else {
                "정상"
            }

            binding.run {
                imageGallery.setImageBitmap(bitmap)
                textResult.text = resultStr
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initModelManager()
        binding.run {
            btnSelectPhoto.setOnClickListener {
                getContent.launch("image/*")
            }
        }
    }

    private fun initModelManager() {
        modelManager = ModelManager(this)
        try {
            modelManager.initModels()
        } catch (exception: IOException) {
            exception.printStackTrace()
            Toast.makeText(this, "Can not init ModelManager!!", Toast.LENGTH_SHORT).show()
        }
    }
}