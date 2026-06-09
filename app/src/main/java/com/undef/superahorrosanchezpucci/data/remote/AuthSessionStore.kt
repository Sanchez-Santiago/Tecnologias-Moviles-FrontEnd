package com.undef.superahorrosanchezpucci.data.remote

import android.content.Context

object AuthSessionStore {
    private const val PREFS = "auth_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"

    var accessToken: String? = null
    var refreshToken: String? = null

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = prefs.getString(ACCESS_TOKEN, null)
        refreshToken = prefs.getString(REFRESH_TOKEN, null)
    }

    fun save(context: Context, accessToken: String?, refreshToken: String?) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun clear(context: Context) {
        accessToken = null
        refreshToken = null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
