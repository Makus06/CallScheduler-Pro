package com.callscheduler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Reçoit les alarmes planifiées et démarre le service d'appel.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called - action: ${intent.action}")

        if (intent.action != "com.callscheduler.ACTION_TRIGGER_CALL") {
            Log.w(TAG, "Action inattendue: ${intent.action}")
            return
        }

        // Wake lock pour s'assurer que l'appareil ne se rendort pas
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CallScheduler:AlarmWakeLock"
        )
        wl.acquire(30_000) // 30 secondes max

        try {
            val callId = intent.getStringExtra("call_id") ?: "unknown"
            Log.d(TAG, "Démarrage du service pour l'appel: $callId")

            val serviceIntent = Intent(context, CallExecutorService::class.java).apply {
                putExtras(intent)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "startForegroundService appelé")
            } else {
                context.startService(serviceIntent)
                Log.d(TAG, "startService appelé")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du démarrage du service", e)
        } finally {
            if (wl.isHeld) wl.release()
        }
    }
}

/**
 * Redémarre les alarmes après reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver - action: $action")

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON") {
            return
        }

        try {
            val serviceIntent = Intent(context, ScheduleMonitorService::class.java).apply {
                putExtra("action", "RESCHEDULE_ALL")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "ScheduleMonitorService démarré pour reprogrammer les alarmes")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du redémarrage des alarmes", e)
        }
    }
}
