package com.r0ld3x.truecaller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.load
import com.r0ld3x.truecaller.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1001
    }
    private val serviceScope = CoroutineScope(Job() + Dispatchers.IO)
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        val channel = NotificationChannel(
            "calls_channel",
            "Call Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        binding.btnStartService.setOnClickListener {
            startCallMonitorService()
            Toast.makeText(this, "Service is started.", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                updateServiceStatus()
            }, 500)
        }
        binding.btnStopService.setOnClickListener {
            val stopIntent = Intent(this, CallService::class.java)
            stopService(stopIntent)
            Toast.makeText(this, "Service is stopped.", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                updateServiceStatus()
            }, 500)
        }

        binding.submitBtn.setOnClickListener {
            searchMobileNumber()
        }
        binding.mobileNo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchMobileNumber()
                true  // consume the action
            } else {
                false // pass on to other listeners
            }
        }

    }

    private fun requestPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
            ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
            ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.READ_CALL_LOG,
                    android.Manifest.permission.READ_CONTACTS
                ),
                PERMISSIONS_REQUEST_CODE
            )
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        }
    }

    private fun startCallMonitorService() {
        val serviceIntent = Intent(this, CallService::class.java)
        startService(serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allPermissionGranted = true


            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionGranted = false
                    break
                }
            }

            if (!allPermissionGranted) {
                Toast.makeText(
                    this,
                    "Permissions are required to monitor calls.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        if (CallService.isRunning) {
            binding.currentState.text = "✅ Service Running"
        } else {
            binding.currentState.text = "❌ Service Not Running"
        }
    }

    private fun searchMobileNumber(){
        val mobNo = binding.mobileNo.text
    if (mobNo.isEmpty()){
            Toast.makeText(applicationContext, "Enter the number.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(applicationContext, "Wait..", Toast.LENGTH_SHORT).show()
        serviceScope.launch {
            try {
                val response = RetrofitClient.apiService.getUserInfo(mobNo.toString())
                val user = response
                withContext(Dispatchers.Main) {
                    binding.postLayout.visibility = VISIBLE
                    binding.profileName.text = user.name
                    binding.address.text = user.address
                    binding.profileImage.load(user.image)
                }
            } catch (e: Exception) {
                Log.e("CallService", "API Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.postLayout.visibility = VISIBLE
                    binding.profileName.text = "Not Found"
                }
            }
        }
    }




}