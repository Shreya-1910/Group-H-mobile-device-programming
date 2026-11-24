package com.mdpgroup08.scpd.data.repository

import android.content.Context
import java.util.UUID

/**
 * Manages unique device ID generation for R6-S13 (One phone per account).
 */
class DeviceHelper(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }

    /** Retrieves or generates a unique UUID for the physical device. */
    fun getDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
}
