package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dvor_parking_prefs", Context.MODE_PRIVATE)

    var myCarNumber: String
        get() = prefs.getString(KEY_CAR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CAR, value.trim().uppercase()).apply()

    var myPhone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE, value.trim()).apply()

    var myApt: String
        get() = prefs.getString(KEY_APT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APT, value.trim()).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value.trim()).apply()

    var isAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_ADMIN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ADMIN, value).apply()

    var hasVotedSurvey: Boolean
        get() = prefs.getBoolean(KEY_SURVEY_VOTED, false)
        set(value) = prefs.edit().putBoolean(KEY_SURVEY_VOTED, value).apply()

    var savedLayoutBackup: String
        get() = prefs.getString(KEY_SAVED_LAYOUT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_LAYOUT, value).apply()

    var currentYardAddress: String
        get() = prefs.getString(KEY_CURRENT_YARD_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CURRENT_YARD_ADDRESS, value.trim()).apply()

    var currentYardId: String
        get() = prefs.getString(KEY_CURRENT_YARD_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CURRENT_YARD_ID, value.trim()).apply()

    var alwaysAskAddressOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_ASK_ADDRESS, true)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS_ASK_ADDRESS, value).apply()

    var customServerUrl: String
        get() = prefs.getString(KEY_CUSTOM_SERVER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_SERVER, value.trim()).apply()

    fun getYardData(yardId: String): String? {
        return prefs.getString("yard_data_$yardId", null)
    }

    fun saveYardData(yardId: String, json: String) {
        prefs.edit().putString("yard_data_$yardId", json).apply()
    }

    fun removeYardData(yardId: String) {
        prefs.edit().remove("yard_data_$yardId").apply()
    }

    fun getKnownYardsJson(): String {
        return prefs.getString(KEY_KNOWN_YARDS_LIST, "") ?: ""
    }

    fun saveKnownYardsJson(json: String) {
        prefs.edit().putString(KEY_KNOWN_YARDS_LIST, json).apply()
    }

    companion object {
        private const val KEY_CAR = "my_car"
        private const val KEY_PHONE = "my_phone"
        private const val KEY_APT = "my_apt"
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_SURVEY_VOTED = "has_voted_survey"
        private const val KEY_SAVED_LAYOUT = "saved_layout_backup"
        private const val KEY_CURRENT_YARD_ADDRESS = "current_yard_address"
        private const val KEY_CURRENT_YARD_ID = "current_yard_id"
        private const val KEY_ALWAYS_ASK_ADDRESS = "always_ask_address_on_launch"
        private const val KEY_KNOWN_YARDS_LIST = "known_yards_list"
        private const val KEY_CUSTOM_SERVER = "custom_server_url"
    }
}

