package com.callscheduler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.callscheduler.service.CallExecutorService
import com.callscheduler.service.ScheduleMonitorService

class CallSchedulerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CallExecutorService.CHANNEL_ID,
                    "Exécution des appels",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications pendant les appels automatiques"
                    setShowBadge(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    ScheduleMonitorService.CHANNEL_ID,
                    "Surveillance planifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Service de surveillance en arrière-plan"
                    setShowBadge(false)
                }
            )
        }
    }
}
