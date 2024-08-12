package com.example.retinacapture.Manual

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
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
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
import com.example.retinacapture.Quick.FullscreenImageActivity
import com.example.retinacapture.R

class CameraManualDiagnosis : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gpuImage: GPUImage
    private lateinit var imageCapture: ImageCapture
    private lateinit var previewView: PreviewView
    private lateinit var progressBar: ProgressBar
    private lateinit var takePhotoButton: Button
    private lateinit var cameraControl: CameraControl
    private lateinit var imagePreview: ImageView

    private var bluetoothGatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var captureInProgress = false
    private var capturedImagePath: String? = null

    companion object {
        private const val TAG = "CameraManualDiagnosis"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_manual_diagnosis)

        val deviceAddress = intent.getStringExtra(LedControlActivity.EXTRA_DEVICE_ADDRESS)
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
        Log.d(TAG, "Device address:- $deviceAddress")
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
        imagePreview = findViewById(R.id.imagePreview)

        takePhotoButton.setOnClickListener {
            if (!captureInProgress) {
                takePhotoButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                takeSinglePhoto()
            }
        }

        imagePreview.setOnClickListener {
            capturedImagePath?.let { path ->
                openFullscreenImage(path)
            }
        }

        setupImageFilters()
        setupModeSwitch()
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
        Log.d(TAG, "Starting camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
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
                    Log.d(TAG, "Camera started successfully-")
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed-", exc)
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Getting camera provider failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeSinglePhoto() {
        captureInProgress = true
        Log.d(TAG, "Taking single photo")

        runOnUiThread {
            takePhoto { imagePath ->
                Log.d(TAG, "Image captured: $imagePath")
                capturedImagePath = imagePath
                runOnUiThread {
                    takePhotoButton.isEnabled = true
                    progressBar.visibility = View.GONE
                    updateImagePreview(imagePath)
                }
            } // Capture image
            // Blink animation
            previewView.alpha = 0.5f
            previewView.animate().alpha(1.0f).setDuration(100).start()
        }
    }

    private fun takePhoto(onImageSaved: (String) -> Unit) {
        Log.d(TAG, "Taking photo")
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
        Log.d(TAG, "Cropping bitmap to circular shape")
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
        Log.d(TAG, "Saving bitmap to file: ${file.absolutePath}")
        val outputStream = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private fun updateImagePreview(imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val circularBitmap = cropToCircularBitmap(bitmap)
        imagePreview.setImageBitmap(circularBitmap)
        imagePreview.visibility = View.VISIBLE
    }

    private fun openFullscreenImage(imagePath: String) {
        val intent = Intent(this, FullscreenImageActivity::class.java).apply {
            putExtra("imagePath", imagePath)
        }
        startActivity(intent)
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
        Log.d(TAG, "Setting up image filters")
        val contrast: SeekBar = findViewById(R.id.seekBarContrast)
        val brightness: SeekBar = findViewById(R.id.seekBarBrightness)
        val saturation: SeekBar = findViewById(R.id.seekBarSaturation)
        val whiteBalance: SeekBar = findViewById(R.id.seekBarWhiteBalance)
        val grayScale: SeekBar = findViewById(R.id.seekBarGrayScale)

        contrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gpuImage.setFilter(GPUImageContrastFilter(progress / 100.0f))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gpuImage.setFilter(GPUImageBrightnessFilter((progress - 50) / 50.0f))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gpuImage.setFilter(GPUImageSaturationFilter(progress / 50.0f))
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

    private fun setupModeSwitch() {
        val switchMode: Switch = findViewById(R.id.switchMode)
        val cardFilters: View = findViewById(R.id.cardFilters)
        val cardRGBW: View = findViewById(R.id.cardRGBW)

        switchMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cardFilters.visibility = View.GONE
                cardRGBW.visibility = View.VISIBLE
            } else {
                cardFilters.visibility = View.VISIBLE
                cardRGBW.visibility = View.GONE
            }
        }

        // RGBW Control Elements
        val buttonON: Button = findViewById(R.id.buttonON)
        val buttonOFF: Button = findViewById(R.id.buttonOFF)
        val seekBarRed: SeekBar = findViewById(R.id.seekBarRed)
        val seekBarGreen: SeekBar = findViewById(R.id.seekBarGreen)
        val seekBarBlue: SeekBar = findViewById(R.id.seekBarBlue)
        val seekBarWhite: SeekBar = findViewById(R.id.seekBarWhite)

        buttonON.setOnClickListener {
            sendLEDCommand(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x06.toByte(), 0x01.toByte(), 0x01.toByte()))
        }

        buttonOFF.setOnClickListener {
            sendLEDCommand(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte()))
        }

        seekBarRed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRGBW()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarGreen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRGBW()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRGBW()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarWhite.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRGBW()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateRGBW() {
        val red = findViewById<SeekBar>(R.id.seekBarRed).progress
        val green = findViewById<SeekBar>(R.id.seekBarGreen).progress
        val blue = findViewById<SeekBar>(R.id.seekBarBlue).progress
        val white = findViewById<SeekBar>(R.id.seekBarWhite).progress

        val command = byteArrayOf(
            0x69.toByte(), 0x96.toByte(), 0x05.toByte(), 0x02.toByte(),
            red.toByte(), green.toByte(), blue.toByte(), white.toByte()
        )
        sendLEDCommand(command)
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

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.-")
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.-")
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
                            Log.d(TAG, "LED characteristic discovered-")
                            break
                        }
                    }
                }
            }
        }
    }
}
