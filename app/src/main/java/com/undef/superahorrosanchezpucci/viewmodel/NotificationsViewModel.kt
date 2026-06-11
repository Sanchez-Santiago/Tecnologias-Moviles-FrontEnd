package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.remote.dto.NotificationResponse
import kotlinx.coroutines.flow.StateFlow

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val notifications: StateFlow<List<NotificationResponse>> = store.notifications
    val unreadCount: StateFlow<Int> = store.unreadCount
    val isLoading: StateFlow<Boolean> = store.isLoading

    fun loadNotifications() = store.loadNotifications()
    fun loadUnreadCount() = store.loadUnreadCount()
    fun markNotificationRead(id: String) = store.markNotificationRead(id)
    fun markAllNotificationsRead() = store.markAllNotificationsRead()
    fun deleteNotification(id: String) = store.deleteNotification(id)
}
