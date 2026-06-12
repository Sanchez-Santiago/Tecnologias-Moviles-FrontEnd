package com.undef.superahorrosanchezpucci.data.remote

import android.content.Context

object AuthSessionStore {
    private const val PREFS = "auth_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val USER_ID = "user_id"

    private var appContext: Context? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var userId: String? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = prefs.getString(ACCESS_TOKEN, null)
        refreshToken = prefs.getString(REFRESH_TOKEN, null)
        userId = prefs.getString(USER_ID, null)
    }

    fun save(context: Context, accessToken: String?, refreshToken: String?, userId: String? = null) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.userId = userId
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .putString(USER_ID, userId)
            .apply()
    }

    fun save(accessToken: String?, refreshToken: String?, userId: String? = null) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.userId = userId
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(ACCESS_TOKEN, accessToken)
            ?.putString(REFRESH_TOKEN, refreshToken)
            ?.putString(USER_ID, userId)
            ?.apply()
    }

    fun clear(context: Context) {
        accessToken = null
        refreshToken = null
        userId = null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
