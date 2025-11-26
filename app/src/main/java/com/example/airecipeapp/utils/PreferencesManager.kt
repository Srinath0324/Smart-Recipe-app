package com.example.airecipeapp.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class for managing app preferences
 */
class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_CAMERA_INSTRUCTIONS_SHOWN = "camera_instructions_shown"
    }
    
    /**
     * Check if camera instructions have been shown
     */
    fun hasCameraInstructionsBeenShown(): Boolean {
        return prefs.getBoolean(KEY_CAMERA_INSTRUCTIONS_SHOWN, false)
    }
    
    /**
     * Mark camera instructions as shown
     */
    fun markCameraInstructionsShown() {
        prefs.edit().apply {
            putBoolean(KEY_CAMERA_INSTRUCTIONS_SHOWN, true)
            apply()
        }
    }
    
    /**
     * Reset camera instructions flag (for testing)
     */
    fun resetCameraInstructions() {
        prefs.edit().apply {
            putBoolean(KEY_CAMERA_INSTRUCTIONS_SHOWN, false)
            apply()
        }
    }
}
