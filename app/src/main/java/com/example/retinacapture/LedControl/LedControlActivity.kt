package com.example.retinacapture.LedControl

import android.bluetooth.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.retinacapture.Bluetooth.DeviceList
import com.example.retinacapture.Manual.CameraManualDiagnosis
import com.example.retinacapture.Quick.CameraQuickDiagnosis
import com.example.retinacapture.R
import java.util.*

class LedControlActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        val TAG = LedControlActivity::class.java.simpleName
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var redValue: Byte = 0x00
    private var greenValue: Byte = 0x00
    private var blueValue: Byte = 0x00
    private lateinit var diagnosisType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_led_control)

        diagnosisType = intent.getStringExtra("diagnosis_type") ?: "quick"
        Log.d(TAG, "Diagnosisd type: $diagnosisType")

        val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
        Log.d(TAG, "Device address: $deviceAddress")

        device?.let {
            bluetoothGatt = it.connectGatt(this, false, gattCallback)
        }

        val seekBarRed: SeekBar = findViewById(R.id.seekBarR)
        seekBarRed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                redValue = progress.toByte()
                updateColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekBarGreen: SeekBar = findViewById(R.id.seekBarG)
        seekBarGreen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                greenValue = progress.toByte()
                updateColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekBarBlue: SeekBar = findViewById(R.id.seekBarB)
        seekBarBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blueValue = progress.toByte()
                updateColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekBarWhite: SeekBar = findViewById(R.id.seekBarW)
        seekBarWhite.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                redValue = progress.toByte()
                greenValue = progress.toByte()
                blueValue = progress.toByte()
                updateColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val btnOn: Button = findViewById(R.id.button2)
        val btnOff: Button = findViewById(R.id.button3)
        val btnDisconnect: Button = findViewById(R.id.button4)
        val btnNext: Button = findViewById(R.id.buttonNext)
        btnOn.setOnClickListener {
            writeCharacteristic(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x06.toByte(), 0x01.toByte(), 0x01.toByte()))
        }

        btnOff.setOnClickListener {
            writeCharacteristic(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte()))
        }

        btnDisconnect.setOnClickListener {
            bluetoothGatt?.disconnect()
            val intent = Intent(this, DeviceList::class.java)
            startActivity(intent)
            finish()
        }

        btnNext.setOnClickListener {
            writeCharacteristic(byteArrayOf(0x69.toByte(), 0x96.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte())) // Turn off LED
            val nextIntent = when (diagnosisType) {
                "quick" -> Intent(this, CameraQuickDiagnosis::class.java)
                "manual" -> Intent(this, CameraManualDiagnosis::class.java)
                else -> Intent(this, CameraQuickDiagnosis::class.java)
            }
            Log.d(TAG, "Next button clicked, starting activity: ${nextIntent.component?.className}")
            nextIntent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
            startActivity(nextIntent)
        }
    }

    private fun updateColor() {
        val colorCommand = byteArrayOf(
            0x69.toByte(), 0x96.toByte(), 0x05.toByte(), 0x02.toByte(),
            redValue, greenValue, blueValue, 0x7F.toByte()
        )
        writeCharacteristic(colorCommand)
    }

    private fun writeCharacteristic(value: ByteArray) {
        ledCharacteristic?.let { characteristic ->
            characteristic.value = value
            val result = bluetoothGatt?.writeCharacteristic(characteristic)
            if (result == false) {
                Log.e(TAG, "Failed to write characteristic")
            } else {
                Log.d(TAG, "Characteristic write initiated")
            }
        } ?: run {
            Log.e(TAG, "Characteristic is not available")
            runOnUiThread {
                Toast.makeText(this, "Characteristic is not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.")
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.")
                runOnUiThread {
                    Toast.makeText(this@LedControlActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
                val intent = Intent(this@LedControlActivity, DeviceList::class.java)
                startActivity(intent)
                finish()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service in gatt.services) {
                    Log.d(TAG, "Service UUID: ${service.uuid}")
                    for (characteristic in service.characteristics) {
                        Log.d(TAG, "Characteristic UUID: ${characteristic.uuid}")
                        if (characteristic.uuid == UUID.fromString("0000ee02-0000-1000-8000-00805f9b34fb")) {
                            ledCharacteristic = characteristic
                            Log.d(TAG, "LED Characteristic found and set")
                            runOnUiThread {
                                Toast.makeText(this@LedControlActivity, "LED Characteristic found and set", Toast.LENGTH_SHORT).show()
                            }
                            break
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@LedControlActivity, "Service discovery failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}
