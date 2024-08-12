package com.example.retinacapture.Quick

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.retinacapture.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageResultsActivity : AppCompatActivity() {

    private lateinit var imageViews: List<ImageView>
    private lateinit var resultImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private val imageResults = mutableListOf<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_results)

        imageViews = listOf(
            findViewById(R.id.imageView1),
            findViewById(R.id.imageView2),
            findViewById(R.id.imageView3),
            findViewById(R.id.imageView4)
        )

        resultImageView = findViewById(R.id.resultImageView)
        progressBar = findViewById(R.id.progressBar)

        val imagePaths = listOf(
            intent.getStringExtra("image1"),
            intent.getStringExtra("image2"),
            intent.getStringExtra("image3"),
            intent.getStringExtra("image4")
        )

        for ((index, imagePath) in imagePaths.withIndex()) {
            if (imagePath != null) {
                Log.d("ImageResultsActivity", "Image $index received path: $imagePath")
                val bitmap = BitmapFactory.decodeFile(imagePath)
                val rotatedBitmap = rotateImageIfRequired(bitmap)
                imageResults.add(rotatedBitmap)
                imageViews[index].setImageBitmap(rotatedBitmap)
                imageViews[index].setOnClickListener {
                    openFullscreenImage(imagePath)
                }
            } else {
                Log.e("ImageResultsActivity", "Image $index is null")
            }
        }

        findViewById<Button>(R.id.buttonRG).setOnClickListener {
            processImage("R-G")
        }
        findViewById<Button>(R.id.buttonRB).setOnClickListener {
            processImage("R-B")
        }
        findViewById<Button>(R.id.buttonBG).setOnClickListener {
            processImage("B-G")
        }

        resultImageView.setOnClickListener {
            openFullscreenImage(intent.getStringExtra("resultImagePath") ?: "")
        }
    }

    private fun openFullscreenImage(imagePath: String) {
        Log.d("ImageResultsActivity", "Opening fullscreen image with path: $imagePath")
        val intent = Intent(this, FullscreenImageActivity::class.java).apply {
            putExtra("imagePath", imagePath)
        }
        startActivity(intent)
    }

    private fun processImage(operation: String) {
        if (imageResults.size < 4) {
            Toast.makeText(this, "Images are not fully loaded", Toast.LENGTH_SHORT).show()
            return
        }

        // Показуємо прогрес бар і приховуємо результат
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            resultImageView.visibility = View.GONE
        }

        Thread {
            try {
                val redChannel = extractChannel(imageResults[0], 0)
                val greenChannel = extractChannel(imageResults[1], 1)
                val blueChannel = extractChannel(imageResults[2], 2)

                val resultBitmap = when (operation) {
                    "R-G" -> subtractMatrices(redChannel, greenChannel)
                    "R-B" -> subtractMatrices(redChannel, blueChannel)
                    "B-G" -> subtractMatrices(blueChannel, greenChannel)
                    else -> {
                        runOnUiThread {
                            Toast.makeText(this, "Unknown operation", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                }

                runOnUiThread {
                    // Після завершення обчислення приховуємо прогрес бар і показуємо результат
                    progressBar.visibility = View.GONE
                    resultImageView.visibility = View.VISIBLE

                    // Встановлення результату обчислення в resultImageView
                    resultImageView.setImageBitmap(resultBitmap)
                    intent.putExtra("resultImagePath", saveBitmap(resultBitmap))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    resultImageView.visibility = View.VISIBLE
                    Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ImageResultsActivity", "Error processing image", e)
                }
            }
        }.start()
    }

    private fun rotateImageIfRequired(img: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    private fun extractChannel(bitmap: Bitmap, channel: Int): Array<Array<Int>> {
        val width = bitmap.width
        val height = bitmap.height
        val channelMatrix = Array(height) { Array(width) { 0 } }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val value = when (channel) {
                    0 -> (pixel shr 16) and 0xFF // Red
                    1 -> (pixel shr 8) and 0xFF  // Green
                    2 -> pixel and 0xFF          // Blue
                    else -> throw IllegalArgumentException("Invalid channel: $channel")
                }
                channelMatrix[y][x] = value
            }
        }

        return channelMatrix
    }

    private fun subtractMatrices(matrix1: Array<Array<Int>>, matrix2: Array<Array<Int>>): Bitmap {
        val height = matrix1.size
        val width = matrix1[0].size

        // Перевірка на відповідність розмірів матриць
        if (matrix2.size != height || matrix2[0].size != width) {
            throw IllegalArgumentException("Matrix dimensions do not match")
        }

        val resultMatrix = Array(height) { Array(width) { 0 } }

        for (y in 0 until height) {
            for (x in 0 until width) {
                resultMatrix[y][x] = (matrix1[y][x] - matrix2[y][x]).coerceIn(0, 255)
            }
        }

        return matrixToBitmap(resultMatrix)
    }

    private fun matrixToBitmap(matrix: Array<Array<Int>>): Bitmap {
        val height = matrix.size
        val width = matrix[0].size

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = matrix[y][x]
                pixels[y * width + x] = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
            }
        }

        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    private fun saveBitmap(bitmap: Bitmap): String {
        val directory = File(getExternalFilesDir(null), "images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "result_image.jpg")
        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
        } catch (e: IOException) {
            Log.e("ImageResultsActivity", "Failed to save bitmap", e)
        }
        return file.absolutePath
    }
}
