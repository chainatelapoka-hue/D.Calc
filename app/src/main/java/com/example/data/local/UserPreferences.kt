package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.NumberFormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeColor(val displayName: String, val banglaName: String, val hexCode: Long) {
    MIDNIGHT_NAVY("Midnight Navy", "মিডনাইট নেভি (ডিফল্ট)", 0xFF38BDF8),
    OBSIDIAN_AMOLED("Obsidian AMOLED", "অ্যামোলেড ব্ল্যাক", 0xFF60A5FA),
    DEEP_EMERALD("Deep Emerald", "এমারেল্ড গ্রীন", 0xFF10B981),
    ROYAL_AMETHYST("Royal Amethyst", "রয়্যাল পার্পল", 0xFFA855F7),
    GOLDEN_AMBER("Golden Amber", "গোল্ডেন ওয়েলথ", 0xFFF59E0B),
    CRIMSON_RUBY("Crimson Ruby", "রুবী রেড", 0xFFF43F5E)
}

data class UserSettings(
    val formatStyle: NumberFormatStyle = NumberFormatStyle.SOUTH_ASIAN,
    val decimalPrecision: Int = 4,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val customRmbToBdt: Double = 17.50,
    val customUsdToBdt: Double = 122.50,
    val defaultFromCurrency: String = "USD",
    val defaultToCurrency: String = "BDT",
    val themeColor: String = AppThemeColor.MIDNIGHT_NAVY.name
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nexora_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        val styleStr = prefs.getString("format_style", NumberFormatStyle.SOUTH_ASIAN.name) ?: NumberFormatStyle.SOUTH_ASIAN.name
        val style = try {
            NumberFormatStyle.valueOf(styleStr)
        } catch (_: Exception) {
            NumberFormatStyle.SOUTH_ASIAN
        }
        val themeStr = prefs.getString("theme_color", AppThemeColor.MIDNIGHT_NAVY.name) ?: AppThemeColor.MIDNIGHT_NAVY.name
        return UserSettings(
            formatStyle = style,
            decimalPrecision = prefs.getInt("decimal_precision", 4),
            hapticEnabled = prefs.getBoolean("haptic_enabled", true),
            soundEnabled = prefs.getBoolean("sound_enabled", false),
            customRmbToBdt = prefs.getFloat("custom_rmb_bdt", 17.50f).toDouble(),
            customUsdToBdt = prefs.getFloat("custom_usd_bdt", 122.50f).toDouble(),
            defaultFromCurrency = prefs.getString("from_curr", "USD") ?: "USD",
            defaultToCurrency = prefs.getString("to_curr", "BDT") ?: "BDT",
            themeColor = themeStr
        )
    }

    fun setFormatStyle(style: NumberFormatStyle) {
        prefs.edit().putString("format_style", style.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(formatStyle = style)
    }

    fun setDecimalPrecision(precision: Int) {
        prefs.edit().putInt("decimal_precision", precision).apply()
        _settingsFlow.value = _settingsFlow.value.copy(decimalPrecision = precision)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(hapticEnabled = enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(soundEnabled = enabled)
    }

    fun setCustomRmbToBdt(rate: Double) {
        prefs.edit().putFloat("custom_rmb_bdt", rate.toFloat()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(customRmbToBdt = rate)
    }

    fun setCustomUsdToBdt(rate: Double) {
        prefs.edit().putFloat("custom_usd_bdt", rate.toFloat()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(customUsdToBdt = rate)
    }

    fun setDefaultCurrencies(from: String, to: String) {
        prefs.edit().putString("from_curr", from).putString("to_curr", to).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultFromCurrency = from, defaultToCurrency = to)
    }

    fun setThemeColor(theme: AppThemeColor) {
        prefs.edit().putString("theme_color", theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(themeColor = theme.name)
    }
}
