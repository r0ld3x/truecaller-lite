package com.r0ld3x.truecaller

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import coil.load
import com.r0ld3x.truecaller.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
//            Manifest.permission.READ_PHONE_NUMBERS
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var callControlManager: CallControlManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            showToast("Permissions are required to monitor calls.")
        }
        checkOverlayPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callControlManager = CallControlManager(this)

        setupUiListeners()
        checkAndRequestPermissions()
        showMiuiPermissionDialogIfNeeded()
    }

    private fun setupUiListeners() {
        binding.btnStartService.setOnClickListener {
            startCallMonitorService()
        }

        binding.btnStopService.setOnClickListener {
            stopCallMonitorService()
        }

        binding.submitBtn.setOnClickListener {
            searchMobileNumber()
        }

        binding.mobileNo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchMobileNumber()
                true
            } else false
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            startActivity(intent)
        }
    }

    private fun startCallMonitorService() {
        callControlManager.enableCallReceiver()
        updateServiceStatus()
        showToast("Service started.")
    }

    private fun stopCallMonitorService() {
        callControlManager.disableCallReceiver()
        updateServiceStatus()
        showToast("Service stopped.")
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        checkOverlayPermission()
    }

    @SuppressLint("SetTextI18n")
    private fun updateServiceStatus() {
        binding.currentState.text =
            if (callControlManager.isCallReceiverEnabled()) "✅ Service Running"
            else "❌ Service Not Running"
    }

    @SuppressLint("SetTextI18n")
    private fun searchMobileNumber() {
        val number = binding.mobileNo.text.toString().filter { it.isDigit() }
        if (number.isEmpty()) {
            showToast("Enter the number.")
            return
        }

        hideKeyboard()
        val context = this
        val digitsOnly = number
        showToast("Wait...")
        lifecycleScope.launch {
            try {
                val user = RetrofitClient.getUserInfoCached(context, digitsOnly)
                if (user == null){
                    showToast("error while doing api call.")
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    binding.postLayout.visibility = android.view.View.VISIBLE
                    binding.profileName.text = user.name
                    binding.address.text = user.address
                    binding.profileImage.load(user.image)
                }
            } catch (e: Exception) {
                Log.e("CallService", "API Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.postLayout.visibility = android.view.View.VISIBLE
                    binding.profileName.text = "Not Found"
                    binding.address.text = ""
                    binding.profileImage.setImageDrawable(null)
                }
            }
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun showMiuiPermissionDialogIfNeeded() {
        if (Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) &&
            !SharePref(this).hasUserSeenMiuiPrompt()
        ) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage(
                    """
                        To display overlay on lock screen in MIUI, please enable:
                        • 'Show on Lock Screen'
                        • 'Display pop-up windows while running in background'
                    """.trimIndent()
                )
                .setPositiveButton("Open Settings") { _, _ ->
                    openMiuiOtherPermissions()
                    SharePref(this).markMiuiPromptAsShown()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun openMiuiOtherPermissions() {
        try {
            val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            }
            startActivity(miuiIntent)

            val autoStartIntent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            startActivity(autoStartIntent)

        } catch (e: Exception) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                }
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
