package com.callscheduler.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.callscheduler.data.model.CallHistoryEntry
import com.callscheduler.data.model.CallStatus
import com.callscheduler.data.model.RepeatMode
import com.callscheduler.data.model.ScheduledCall
import com.callscheduler.service.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class CallSchedulerRepository(private val context: Context) {

    companion object {
        private const val TAG = "CallSchedulerRepo"
    }

    private val db = CallSchedulerDatabase.getInstance(context)
    private val scheduledCallDao = db.scheduledCallDao()
    private val callHistoryDao = db.callHistoryDao()

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val appContext = context.applicationContext

    // ── FLUX DE DONNÉES ──────────────────────────────────

    fun getAllScheduledCalls(): Flow<List<ScheduledCall>> = scheduledCallDao.getAllCalls()
    fun getEnabledCalls(): Flow<List<ScheduledCall>> = scheduledCallDao.getEnabledCalls()
    fun getCallsByGroup(tag: String): Flow<List<ScheduledCall>> = scheduledCallDao.getCallsByGroup(tag)
    fun getAllGroups(): Flow<List<String>> = scheduledCallDao.getAllGroups()
    fun getCallHistory(): Flow<List<CallHistoryEntry>> = callHistoryDao.getRecentHistory()
    suspend fun getCallById(id: String): ScheduledCall? = scheduledCallDao.getCallById(id)

    // ── CRUD APPELS PLANIFIÉS ─────────────────────────────

    suspend fun insertScheduledCall(call: ScheduledCall) {
        val withNext = call.copy(nextCallTimestamp = computeNextTimestamp(call))
        scheduledCallDao.insertCall(withNext)
        if (withNext.isEnabled) scheduleAlarm(withNext)
    }

    suspend fun updateScheduledCall(call: ScheduledCall) {
        val withNext = call.copy(nextCallTimestamp = computeNextTimestamp(call))
        scheduledCallDao.updateCall(withNext)
        cancelAlarm(withNext.id)
        if (withNext.isEnabled) scheduleAlarm(withNext)
    }

    suspend fun deleteScheduledCall(callId: String) {
        scheduledCallDao.deleteCallById(callId)
        cancelAlarm(callId)
    }

    suspend fun toggleCallEnabled(callId: String, enabled: Boolean) {
        scheduledCallDao.setEnabled(callId, enabled)
        if (enabled) {
            getCallById(callId)?.let { call ->
                val withNext = call.copy(
                    isEnabled = true,
                    nextCallTimestamp = computeNextTimestamp(call)
                )
                scheduledCallDao.updateNextCallTimestamp(callId, withNext.nextCallTimestamp)
                scheduleAlarm(withNext)
            }
        } else {
            cancelAlarm(callId)
        }
    }

    // ── HISTORIQUE ────────────────────────────────────────

    suspend fun addHistoryEntry(entry: CallHistoryEntry) {
        callHistoryDao.insert(entry)
    }

    suspend fun clearOldHistory() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        callHistoryDao.deleteOlderThan(thirtyDaysAgo)
    }

    /**
     * Appelé quand un appel vient de se terminer : met à jour les stats
     * et planifie automatiquement la prochaine occurrence (répétition infinie).
     */
    suspend fun markCallCompleted(callId: String, status: CallStatus) {
        scheduledCallDao.updateCallStats(callId, status, System.currentTimeMillis())

        val call = getCallById(callId) ?: return
        val next = computeNextTimestamp(call, fromNow = false)
        if (next > 0 && next < call.endDate) {
            scheduledCallDao.updateNextCallTimestamp(callId, next)
            if (call.isEnabled) scheduleAlarm(call.copy(nextCallTimestamp = next))
        }
    }

    // ── CALCUL DU PROCHAIN HORAIRE ───────────────────────

    fun computeNextTimestamp(call: ScheduledCall, fromNow: Boolean = true): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            if (fromNow) timeInMillis = now
            set(Calendar.HOUR_OF_DAY, call.scheduledHour)
            set(Calendar.MINUTE, call.scheduledMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si l'heure programmée est déjà passée pour aujourd'hui, viser le jour suivant
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val result = when (call.repeatMode) {
            RepeatMode.ONCE -> {
                val target = cal.timeInMillis
                if (target > now) target else -1L
            }
            RepeatMode.DAILY -> cal.timeInMillis
            RepeatMode.WEEKLY -> cal.timeInMillis
            RepeatMode.WEEKDAYS -> {
                while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            RepeatMode.WEEKENDS -> {
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                    cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            RepeatMode.CUSTOM_DAYS -> {
                val days = call.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (days.isEmpty()) {
                    cal.timeInMillis
                } else {
                    var found = false
                    for (i in 0..6) {
                        val dow = cal.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY=1 .. SATURDAY=7
                        val mapped = if (dow == Calendar.SUNDAY) 7 else dow - 1 // 1=Lun..7=Dim
                        if (mapped in days) { found = true; break }
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    if (found) cal.timeInMillis else -1L
                }
            }
            RepeatMode.INTERVAL -> {
                val intervalDays = call.repeatIntervalDays.coerceAtLeast(1)
                cal.add(Calendar.DAY_OF_YEAR, intervalDays - 1)
                cal.timeInMillis
            }
        }

        if (result <= 0) return result
        return if (result < call.startDate) call.startDate else result
    }

    // ── ALARMES ───────────────────────────────────────────

    private fun scheduleAlarm(call: ScheduledCall) {
        if (call.nextCallTimestamp <= 0) {
            Log.w(TAG, "Pas de prochaine date calculée pour ${call.label}, alarme non programmée")
            return
        }

        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = "com.callscheduler.ACTION_TRIGGER_CALL"
            putExtra("call_id", call.id)
            putExtra("call_label", call.label)
            putExtra("phone_number", call.phoneNumber)
            putExtra("dtmf_extension", call.dtmfExtension)
            putExtra("sim_slot", call.simSlot)
            putExtra("auto_redial", call.autoRedialOnBusy)
            putExtra("redial_max", call.autoRedialMaxAttempts)
            putExtra("redial_delay", call.autoRedialDelaySeconds)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            call.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(call.nextCallTimestamp, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    call.nextCallTimestamp,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarme programmée pour ${call.label} à ${call.nextCallTimestamp}")
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, call.nextCallTimestamp, pendingIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Erreur alarme", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur alarme", e)
        }
    }

    private fun cancelAlarm(callId: String) {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = "com.callscheduler.ACTION_TRIGGER_CALL"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            callId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Reprogramme toutes les alarmes (appelé après un redémarrage de l'appareil).
     */
    suspend fun rescheduleAllAlarms() = withContext(Dispatchers.IO) {
        val enabledCalls = scheduledCallDaoSnapshot()
        enabledCalls.filter { it.isEnabled }.forEach { call ->
            val next = computeNextTimestamp(call)
            if (next > 0) {
                scheduledCallDao.updateNextCallTimestamp(call.id, next)
                scheduleAlarm(call.copy(nextCallTimestamp = next))
            }
        }
    }

    private suspend fun scheduledCallDaoSnapshot(): List<ScheduledCall> {
        // getDueCallsNow avec une date très lointaine renvoie tous les appels actifs
        return scheduledCallDao.getDueCallsNow(Long.MAX_VALUE)
    }
}
