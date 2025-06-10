package com.r0ld3x.truecaller

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharePref(context: Context){
    val prefs: SharedPreferences? = context.getSharedPreferences("miui_perms", Context.MODE_PRIVATE)

    fun hasUserSeenMiuiPrompt(): Boolean {
        if(prefs == null){
            return false
        }
        return prefs.getBoolean("miui_prompt_shown", false)
    }

    fun markMiuiPromptAsShown() {
        prefs?.edit() { putBoolean("miui_prompt_shown", true) }
    }
}