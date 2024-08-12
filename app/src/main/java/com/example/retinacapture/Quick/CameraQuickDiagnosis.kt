package com.example.retinacapture.Quick

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCallback
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.FocusMeteringAction
import com.example.retinacapture.LedControl.LedControlActivity
import com.example.retinacapture.R


class CameraQuickDiagnosis : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gpuImage: GPUImage
    private lateinit var imageCapture: ImageCapture
    private lateinit var previewView: PreviewView
    private lateinit var progressBar: ProgressBar
    private lateinit var takePhotoButton: Button
    private lateinit var cameraControl: CameraControl

    private var bluetoothGatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var captureInProgress = false

    companion object {
        private const val TAG = "CameraQuickDiagnosis"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_quick_diagnosis)

        val deviceAddress = intent.getStringExtra(LedControlActivity.EXTRA_DEVICE_ADDRESS)
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)

        previewView = findViewById(R.id.previewView)
        previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                cameraControl.startFocusAndMetering(action)
            }
            true
        }

        gpuImage = GPUImage(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        takePhotoButton = findViewById(R.id.buttonTakePhoto)
        progressBar = findViewById(R.id.progressBar)

        takePhotoButton.setOnClickListener {
            if (!captureInProgress) {
                takePhotoButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                captureImages()
            }
        }

        setupImageFilters()
    }

    override fun onResume() {
        super.onResume()
        captureInProgress = false
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageCapture
                )
                cameraControl = camera.cameraControl
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImages() {
        captureInProgress = true
        val ledCommands = listOf(
            byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x05.toByte(), 0x02.toByte(), 0x7F.toByte(), 0x00.toByte(), 0x00.toByte(), 0x7F.toByte()), // Red
            byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x05.toByte(), 0x02.toByte(), 0x00.toByte(), 0x7F.toByte(), 0x00.toByte(), 0x7F.toByte()), // Green
            byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x05.toByte(), 0x02.toByte(), 0x00.toByte(), 0x00.toByte(), 0x7F.toByte(), 0x7F.toByte()), // Blue
            byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x05.toByte(), 0x02.toByte(), 0x7F.toByte(), 0x7F.toByte(), 0x7F.toByte(), 0x7F.toByte())  // White
        )

        val handler = Handler(Looper.getMainLooper())
        val imagePaths = mutableListOf<String>()

        cameraExecutor.execute {
            for ((index, command) in ledCommands.withIndex()) {
                runOnUiThread {
                    Log.d(TAG, "Setting LED command for index $index")
                    sendLEDCommand(command) // Set color first
                    handler.postDelayed({
                        sendLEDCommand(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x06.toByte(), 0x01.toByte(), 0x01.toByte())) // Turn on LED
                    }, 500)
                }
                Thread.sleep(1000) // Delay to ensure color is set and LED is turned on
                runOnUiThread {
                    Log.d(TAG, "Capturing image for index $index")
                    takePhoto { imagePath ->
                        imagePaths.add(imagePath)
                        if (imagePaths.size == ledCommands.size) {
                            runOnUiThread {
                                takePhotoButton.isEnabled = true
                                progressBar.visibility = View.GONE
                            }
                            navigateToResultsPage(imagePaths)
                        }
                    } // Capture image
                    // Blink animation
                    previewView.alpha = 0.5f
                    previewView.animate().alpha(1.0f).setDuration(100).start()
                }
                Thread.sleep(1000) // Delay to allow capture to complete
                runOnUiThread {
                    Log.d(TAG, "Turning off LED for index $index")
                    sendLEDCommand(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte())) // Turn off LED
                }
                Thread.sleep(1000) // Delay to ensure LED is turned off
            }
        }
    }

    private fun takePhoto(onImageSaved: (String) -> Unit) {
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        takePhotoButton.isEnabled = true
                        progressBar.visibility = View.GONE
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val circularBitmap = cropToCircularBitmap(bitmap)
                    saveBitmapToFile(circularBitmap, photoFile)
                    onImageSaved(photoFile.absolutePath)
                }
            })
    }

    private fun cropToCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
        if (squaredBitmap != bitmap) {
            bitmap.recycle()
        }

        val circularBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(circularBitmap)
        val paint = Paint()
        paint.isAntiAlias = true

        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squaredBitmap, 0f, 0f, paint)

        squaredBitmap.recycle()
        return circularBitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        val outputStream = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private fun sendLEDCommand(command: ByteArray) {
        Log.d(TAG, "Sending LED command: ${command.joinToString(" ") { String.format("%02X", it) }}")
        ledCharacteristic?.let { characteristic ->
            characteristic.value = command
            val result = bluetoothGatt?.writeCharacteristic(characteristic)
            if (result == false) {
                Log.e(TAG, "Failed to write LED characteristic")
            } else {
                Log.d(TAG, "LED command write initiated")
            }
        } ?: run {
            Log.e(TAG, "LED Characteristic not available")
        }
    }

    private fun setupImageFilters() {
        val contrast: SeekBar = findViewById(R.id.seekBarContrast)
        val brightness: SeekBar = findViewById(R.id.seekBarBrightness)
        val saturation: SeekBar = findViewById(R.id.seekBarSaturation)
        val whiteBalance: SeekBar = findViewById(R.id.seekBarWhiteBalance)
        val grayScale: SeekBar = findViewById(R.id.seekBarGrayScale)

        contrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val contrastFilter = GPUImageContrastFilter(progress / 100.0f)
                gpuImage.setFilter(contrastFilter)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightnessFilter = GPUImageBrightnessFilter((progress - 50) / 50.0f)
                gpuImage.setFilter(brightnessFilter)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val saturationFilter = GPUImageSaturationFilter(progress / 50.0f)
                gpuImage.setFilter(saturationFilter)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        whiteBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Apply white balance filter logic here
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        grayScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Apply gray scale filter logic here
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    private fun navigateToResultsPage(imagePaths: List<String>) {
        val intent = Intent(this, ImageResultsActivity::class.java).apply {
            imagePaths.forEachIndexed { index, path ->
                putExtra("image${index + 1}", path)
            }
        }
        startActivity(intent)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.")
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.")
                runOnUiThread {
                    finish()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service in gatt.services) {
                    for (characteristic in service.characteristics) {
                        if (characteristic.uuid == UUID.fromString("0000ee02-0000-1000-8000-00805f9b34fb")) {
                            ledCharacteristic = characteristic
                            break
                        }
                    }
                }
            }
        }
    }
}
