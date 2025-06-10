package com.r0ld3x.truecaller

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import coil.load
import coil.transform.CircleCropTransformation
import com.r0ld3x.truecaller.databinding.OverlayNameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class IncomingCallReceiver : BroadcastReceiver() {

    private lateinit var overlayView: LinearLayout
    private lateinit var windowManager: WindowManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = getIncomingNumber(intent)
                if (incomingNumber == null) {
                    return
                }
                handleIncomingCall(context, incomingNumber)
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE || state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                releaseWakeLock()
                removeOverlay()
            }
        }
    }

    private fun getIncomingNumber(intent: Intent): String? {
        return try {
            @Suppress("DEPRECATION")
            intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        } catch (e: Exception) {
            Log.e("IncomingCallReceiver", "Error getting incoming number: ${e.message}")
            null
        }
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String?) {
        if (isNumberSaved(context, phoneNumber.toString())) {
            Log.d("CallState", "Contact is saved. $phoneNumber")
            return
        }
        Log.d("IncomingCallReceiver", "Incoming number: $phoneNumber")
        acquireWakeLock(context)

        fetchUserInfoFromApi(context, phoneNumber.toString())
    }

    private fun acquireWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "TrueCaller:IncomingCallWakeLock"
            )
            wakeLock?.acquire(30000) // 30 seconds timeout
            Log.d("WakeLock", "Wake lock acquired")
        } catch (e: Exception) {
            Log.e("WakeLock", "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("WakeLock", "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("WakeLock", "Error releasing wake lock: ${e.message}")
        }
    }

    private fun removeOverlay() {
        try {
            if (::overlayView.isInitialized && ::windowManager.isInitialized) {
                windowManager.removeView(overlayView)
                Log.d("Overlay", "Overlay removed")
            }
        } catch (e: Exception) {
            Log.e("Overlay", "Error removing overlay: ${e.message}")
        }
    }

    private fun fetchUserInfoFromApi(context: Context, number: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getUserInfo(number.toString())
                val user = response
                withContext(Dispatchers.Main) {
                    showCallerOverlay(context, user.name, user.image, user.address)
                }
            } catch (e: Exception) {
                Log.e("CallService", "API Error: ${e.message}")
                Log.e("CallService", "API Error: ${e.toString()}")
                withContext(Dispatchers.Main) {
                    showCallerOverlay(context, "API DEAD", "", "")
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCallerOverlay(
        context: Context,
        callerName: String,
        callerPhoto: String,
        callerLocation: String
    ) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(context)
        val binding = OverlayNameBinding.inflate(inflater)
        overlayView = binding.root

        binding.nameText.text = callerName.ifBlank { "Unknown Caller" }
        binding.locationText.text = callerLocation.ifBlank { "Unknown Location" }
        if (callerPhoto.isNotBlank()) {
            binding.profileImage.load(callerPhoto) {
                crossfade(true)
                placeholder(R.drawable.user_person_icon)
                error(R.drawable.user_person_icon)
                transformations(CircleCropTransformation())
            }
        } else {
            binding.profileImage.setImageResource(R.drawable.user_person_icon)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        try {
            windowManager.addView(overlayView, params)
            Log.d("Overlay", "Overlay shown successfully")
        } catch (e: Exception) {
            Log.e("Overlay", "Failed to show overlay: ${e.message}")
        }

        var initialX = 0f
        var initialY = 0f
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var hasMovedSignificantly = false

        overlayView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x.toFloat()
                    initialY = params.y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    hasMovedSignificantly = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val currentX = event.rawX
                    val currentY = event.rawY
                    val deltaX = currentX - initialTouchX
                    val deltaY = currentY - initialTouchY

                    // Check if user has moved significantly (more than touch slop)
                    if (!hasMovedSignificantly && (abs(deltaX) > 20 || abs(deltaY) > 20)) {
                        hasMovedSignificantly = true
                        isDragging = true
                    }

                    if (isDragging) {
                        // Update overlay position smoothly
                        params.x = (initialX + deltaX).toInt()
                        params.y = (initialY + deltaY).toInt()

                        try {
                            windowManager.updateViewLayout(overlayView, params)
                        } catch (e: Exception) {
                            Log.e("Overlay", "Error updating overlay position: ${e.message}")
                        }

                        val dismissThreshold = 150f // Reduced threshold for better UX

                        // Horizontal swipe to dismiss
                        if (abs(deltaX) > dismissThreshold && abs(deltaX) > abs(deltaY)) {
                            try {
                                windowManager.removeView(overlayView)
                                releaseWakeLock()
                                Log.d("Overlay", "Overlay removed by horizontal swipe")
                                return@setOnTouchListener true
                            } catch (e: Exception) {
                                Log.e("Overlay", "Error removing overlay on swipe: ${e.message}")
                            }
                        }

                        // Vertical swipe to dismiss
                        if (abs(deltaY) > dismissThreshold && abs(deltaY) > abs(deltaX)) {
                            try {
                                windowManager.removeView(overlayView)
                                releaseWakeLock()
                                Log.d("Overlay", "Overlay removed by vertical swipe")
                                return@setOnTouchListener true
                            } catch (e: Exception) {
                                Log.e("Overlay", "Error removing overlay on swipe: ${e.message}")
                            }
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val finalDeltaX = event.rawX - initialTouchX
                        val finalDeltaY = event.rawY - initialTouchY

                        // If user didn't swipe far enough, animate back to center
                        if (abs(finalDeltaX) < 150 && abs(finalDeltaY) < 150) {
                            // Animate back to original position
                            val handler = Handler(Looper.getMainLooper())
                            val startX = params.x
                            val startY = params.y
                            val targetX = 0 // Center horizontally
                            val targetY = 100 // Original Y position
                            val animationDuration = 200L
                            val startTime = System.currentTimeMillis()

                            val animateBack = object : Runnable {
                                override fun run() {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val progress =
                                        (elapsed.toFloat() / animationDuration).coerceAtMost(1f)

                                    // Smooth interpolation
                                    val interpolatedProgress =
                                        1f - (1f - progress) * (1f - progress)

                                    params.x =
                                        (startX + (targetX - startX) * interpolatedProgress).toInt()
                                    params.y =
                                        (startY + (targetY - startY) * interpolatedProgress).toInt()

                                    try {
                                        windowManager.updateViewLayout(overlayView, params)
                                    } catch (e: Exception) {
                                        Log.e(
                                            "Overlay",
                                            "Error animating overlay back: ${e.message}"
                                        )
                                        return
                                    }

                                    if (progress < 1f) {
                                        handler.postDelayed(this, 16) // ~60fps
                                    }
                                }
                            }
                            handler.post(animateBack)
                        }
                    }

                    isDragging = false
                    hasMovedSignificantly = false
                    true
                }

                else -> false
            }
        }

        binding.closeButton.setOnClickListener {
            try {
                windowManager.removeView(overlayView)
                releaseWakeLock()
            } catch (e: Exception) {
                Log.e("Overlay", "Error removing overlay on click: ${e.message}")
            }
        }

        // Auto-remove overlay after 15 seconds (increased from 10)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                windowManager.removeView(overlayView)
                releaseWakeLock()
            } catch (e: Exception) {
                Log.e("Overlay", "Error auto-removing overlay: ${e.message}")
            }
        }, 15_000)
    }

    fun isNumberSaved(context: Context, phoneNumber: String): Boolean {
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )

        cursor?.use {
            return it.count > 0
        }

        return false
    }


    

}
