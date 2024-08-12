package com.example.retinacapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.retinacapture.Bluetooth.DeviceList

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_PERMISSIONS = 1
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnQuickDiagnosis: Button = findViewById(R.id.btnQuickDiagnosis)
        val btnManualDiagnosis: Button = findViewById(R.id.btnManualDiagnosis)

        btnQuickDiagnosis.setOnClickListener {
            if (hasPermissions()) {
                startDeviceListActivity("quick")
            } else {
                requestPermissions()
            }
        }

        btnManualDiagnosis.setOnClickListener {
            if (hasPermissions()) {
                startDeviceListActivity("manual")
            } else {
                requestPermissions()
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
        ), REQUEST_CODE_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (hasPermissions()) {
                startDeviceListActivity("quick")
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDeviceListActivity(diagnosisType: String) {
        Log.d(TAG, "Starting DeviceListActivity with diagnosisType: $diagnosisType")
        val intent = Intent(this, DeviceList::class.java).apply {
            putExtra("diagnosis_type", diagnosisType)
        }
        startActivity(intent)
    }
}
