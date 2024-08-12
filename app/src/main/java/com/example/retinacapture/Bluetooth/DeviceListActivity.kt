package com.example.retinacapture.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.retinacapture.LedControl.LedControlActivity
import com.example.retinacapture.R

class DeviceList : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var deviceListAdapter: ArrayAdapter<String>? = null
    private val deviceList: MutableList<String> = ArrayList()
    private val devicesMap: MutableMap<String, BluetoothDevice> = HashMap()
    private lateinit var diagnosisType: String
    private val TAG = "DeviceList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        diagnosisType = intent.getStringExtra("diagnosis_type") ?: "quick"
        Log.d(TAG, "Diagnosisk type: $diagnosisType")

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        deviceListAdapter = ArrayAdapter(this, R.layout.list_item, deviceList)
        val listView = findViewById<ListView>(R.id.listView)
        listView.adapter = deviceListAdapter

        val scanButton = findViewById<Button>(R.id.button)
        scanButton.setOnClickListener { v: View? ->
            if (hasPermissions()) {
                startScan()
            } else {
                requestPermissions()
            }
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val deviceInfo = deviceList[position]
            val deviceAddress = deviceInfo.substring(deviceInfo.lastIndexOf("\n") + 1)
            val device = devicesMap[deviceAddress]
            if (device != null) {
                val intent = Intent(this, LedControlActivity::class.java).apply {
                    putExtra(LedControlActivity.EXTRA_DEVICE_ADDRESS, deviceAddress)
                    putExtra("diagnosis_type", diagnosisType)
                }
                Log.d(TAG, "Device selected: $deviceAddress, starting LedControlActivity")
                startActivity(intent)
            } else {
                Log.e(TAG, "Device not found: $deviceAddress")
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        ), REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (hasPermissions()) {
                startScan()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScan() {
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            bluetoothAdapter!!.startDiscovery()
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            startScan()
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val deviceName = device.name ?: "Unknown Device"
                    val deviceAddress = device.address
                    devicesMap[deviceAddress] = device
                    deviceList.add("$deviceName\n$deviceAddress")
                    deviceListAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_CODE_PERMISSIONS = 2
    }
}
