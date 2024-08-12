package com.example.retinacapture.Quick

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.retinacapture.R
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class FullscreenImageActivity : AppCompatActivity() {

    private lateinit var fullScreenImageView: ShapeableImageView
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        fullScreenImageView = findViewById(R.id.fullScreenImageView)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        imagePath = intent.getStringExtra("imagePath")

        imagePath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            if (bitmap != null) {
                val rotatedBitmap = rotateImageIfRequired(bitmap)
                fullScreenImageView.setImageBitmap(rotatedBitmap)
                Log.d("FullscreenImageActivity", "Image loaded and rotated")
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                Log.e("FullscreenImageActivity", "Failed to load image from path: $it")
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            imagePath?.let {
                saveImageToGallery(it)
            }
        }
    }

    private fun saveImageToGallery(imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val rotatedBitmap = rotateImageIfRequired(bitmap)
        val savedImagePath: String

        val imageFileName = "JPEG_" + System.currentTimeMillis() + ".jpg"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "RC_AutoDiagnosis"
        )
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.absolutePath
            try {
                val fOut = FileOutputStream(imageFile)
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
                runOnUiThread {
                    saveButton.text = "Saved"
                    saveButton.isEnabled = false
                    saveButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                    saveButton.setTextColor(resources.getColor(android.R.color.white))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show()
                return
            }

            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri: Uri = Uri.fromFile(imageFile)
            mediaScanIntent.data = contentUri
            this.sendBroadcast(mediaScanIntent)
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rotateImageIfRequired(img: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }
}
