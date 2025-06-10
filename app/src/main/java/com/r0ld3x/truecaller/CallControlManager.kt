package com.r0ld3x.truecaller

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

class CallControlManager(private val context: Context) {

    fun enableCallReceiver() {
        val componentName = ComponentName(context, IncomingCallReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("CallControlManager", "Receiver enabled")
    }

    fun disableCallReceiver() {
        val componentName = ComponentName(context, IncomingCallReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("CallControlManager", "Receiver disabled")
    }

    fun isCallReceiverEnabled(): Boolean {
        val componentName = ComponentName(context, IncomingCallReceiver::class.java)
        val state = context.packageManager.getComponentEnabledSetting(componentName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }
}