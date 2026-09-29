package com.selkicx.manualbooth.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "selkicx_settings"
private const val KEY_QR_SHARING_ENABLED = "qr_sharing_enabled"

/**
 * Small settings store for Admin toggles (spec section 42: QR Sharing
 * ON/OFF). Backed by plain SharedPreferences rather than a Room table or
 * DataStore - a single boolean flag doesn't warrant either.
 */
class AppSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _qrSharingEnabled = MutableStateFlow(prefs.getBoolean(KEY_QR_SHARING_ENABLED, false))
    val qrSharingEnabled: StateFlow<Boolean> = _qrSharingEnabled.asStateFlow()

    fun setQrSharingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QR_SHARING_ENABLED, enabled).apply()
        _qrSharingEnabled.value = enabled
    }
}
