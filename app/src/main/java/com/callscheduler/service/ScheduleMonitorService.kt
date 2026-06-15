package com.callscheduler.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.callscheduler.MainActivity
import com.callscheduler.data.repository.CallSchedulerRepository
import kotlinx.coroutines.*

/**
 * Service de surveillance des planifications.
 * Démarre au boot pour rétablir toutes les alarmes.
 */
class ScheduleMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "schedule_monitor_channel"
        const val NOTIFICATION_ID = 1002
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Reprogrammation des alarmes..."))

        scope.launch {
            try {
                val repo = CallSchedulerRepository(applicationContext)
                repo.rescheduleAllAlarms()
                repo.clearOldHistory()
            } catch (e: Exception) {
                android.util.Log.e("ScheduleMonitor", "Erreur", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Surveillance planifications",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Planificateur d'appels")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
