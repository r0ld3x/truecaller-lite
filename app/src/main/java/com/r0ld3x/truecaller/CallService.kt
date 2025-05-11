package com.r0ld3x.truecaller

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.r0ld3x.truecaller.databinding.OverlayNameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallService : Service() {

    companion object {
        var isRunning = false
    }

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var overlayView: LinearLayout
    private lateinit var windowManager: WindowManager
    private val listener = object : PhoneStateListener() {
        @SuppressLint("ServiceCast")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d("CallState", "📞 Incoming call from $phoneNumber")
                    if (isNumberSaved(applicationContext, phoneNumber.toString())) {
                        Log.d("CallState", "Contact is saved. $phoneNumber")
                        return
                    }
                    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                    val wakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "truecaller:OverlayWakeLock"
                    )

                    Log.d("WakeLock", "Trying to acquire wake lock. isHeld=${wakeLock.isHeld}")
                    wakeLock.acquire(10_000L)
                    Log.d("WakeLock", "Wake lock acquired. isHeld=${wakeLock.isHeld}")

                    fetchUserInfoFromApi(phoneNumber.toString())
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d("CallState", "📲 Call answered or dialing")
                }

                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d("CallState", "📴 Call ended or idle")
                    try {
                        windowManager.removeView(overlayView)
                    } catch (e: Exception) {
                        Log.e("Overlay", "Failed to remove overlay: ${e.message}")
                    }
                }
            }

        }

    }

    private val serviceScope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        windowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "calls_channel")
            .setContentTitle("Call Monitoring Active")
            .setContentText("Waiting for incoming calls...")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }


    private fun fetchUserInfoFromApi(number: String) {
        serviceScope.launch {
            try {
                val response = RetrofitClient.apiService.getUserInfo(number.toString())
                val user = response
                withContext(Dispatchers.Main) {
                    showCallerOverlay(applicationContext, user.name, user.image, user.address)
                }
            } catch (e: Exception) {
                Log.e("CallService", "API Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    showCallerOverlay(applicationContext, "API DEAD", "", "")
                }
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun showCallerOverlay(context: Context, callerName: String, callerPhoto: String, callerLocation: String) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!powerManager.isInteractive) {
            Log.d("Overlay", "Delaying overlay because screen is off")
            Handler(Looper.getMainLooper()).postDelayed({
                showCallerOverlay(context, callerName,callerPhoto, callerLocation)
            }, 1000)
            return
        }


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
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        try {
            windowManager.addView(overlayView, params)
            Log.d("Overlay", "Overlay shown successfully")
        } catch (e: Exception) {
            Log.e("Overlay", "Failed to show overlay: ${e.message}")
        }

        var initialX = 0
        var initialY = 0
        var lastAction = 0


        overlayView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX.toInt()
                    initialY = event.rawY.toInt()
                    lastAction = MotionEvent.ACTION_DOWN
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX.toInt() - initialX
                    val deltaY = event.rawY.toInt() - initialY
                    params.x += deltaX
                    params.y += deltaY
                    windowManager.updateViewLayout(overlayView, params)
                    initialX = event.rawX.toInt()
                    initialY = event.rawY.toInt()
                    lastAction = MotionEvent.ACTION_MOVE
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    lastAction = MotionEvent.ACTION_UP
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }


        binding.closeButton.setOnClickListener {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                Log.e("Overlay", "Error removing overlay on click: ${e.message}")
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                Log.e("Overlay", "Error auto-removing overlay: ${e.message}")
            }
        }, 10_000)
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

    override fun onDestroy() {
        Log.d("Overlay", "onDestroy is called")
        super.onDestroy()
        isRunning = false
    }

}