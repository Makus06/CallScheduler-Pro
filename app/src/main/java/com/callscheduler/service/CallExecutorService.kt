package com.callscheduler.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.*
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callscheduler.MainActivity
import com.callscheduler.R
import com.callscheduler.data.model.CallHistoryEntry
import com.callscheduler.data.model.CallStatus
import com.callscheduler.data.repository.CallSchedulerRepository
import kotlinx.coroutines.*

/**
 * Service de premier plan qui exécute les appels téléphoniques planifiés.
 * Gère : SIM duale, DTMF, recomposition automatique, état d'appel.
 */
class CallExecutorService : Service() {

    companion object {
        private const val TAG = "CallExecutorService"
        const val CHANNEL_ID = "call_executor_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: CallSchedulerRepository
    private var currentCallId: String? = null
    private var redialAttempt = 0

    override fun onCreate() {
        super.onCreate()
        repository = CallSchedulerRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Préparation de l'appel..."))

        val callId = intent?.getStringExtra("call_id") ?: run { stopSelf(); return START_NOT_STICKY }
        val phoneNumber = intent.getStringExtra("phone_number") ?: run { stopSelf(); return START_NOT_STICKY }
        val label = intent.getStringExtra("call_label") ?: phoneNumber
        val dtmf = intent.getStringExtra("dtmf_extension") ?: ""
        val simSlot = intent.getIntExtra("sim_slot", 0)
        val autoRedial = intent.getBooleanExtra("auto_redial", true)
        val redialMax = intent.getIntExtra("redial_max", 3)
        val redialDelay = intent.getIntExtra("redial_delay", 60)

        currentCallId = callId
        redialAttempt = 1

        serviceScope.launch {
            executeCall(
                callId = callId,
                phoneNumber = phoneNumber,
                label = label,
                dtmfExtension = dtmf,
                simSlot = simSlot,
                autoRedial = autoRedial,
                redialMax = redialMax,
                redialDelaySeconds = redialDelay
            )
        }

        return START_NOT_STICKY
    }

    private suspend fun executeCall(
        callId: String,
        phoneNumber: String,
        label: String,
        dtmfExtension: String,
        simSlot: Int,
        autoRedial: Boolean,
        redialMax: Int,
        redialDelaySeconds: Int
    ) {
        updateNotification("📞 Appel en cours : $label")
        Log.i(TAG, "Lancement appel → $phoneNumber (SIM $simSlot) tentative $redialAttempt")

        val startTime = System.currentTimeMillis()
        var finalStatus = CallStatus.CALLING

        try {
            placeCall(phoneNumber, simSlot, dtmfExtension)
            finalStatus = monitorCallState(phoneNumber, redialDelaySeconds)

            if (finalStatus == CallStatus.BUSY && autoRedial && redialAttempt < redialMax) {
                redialAttempt++
                updateNotification("🔄 Recomposition #$redialAttempt dans ${redialDelaySeconds}s...")
                delay(redialDelaySeconds * 1000L)
                executeCall(callId, phoneNumber, label, dtmfExtension, simSlot, autoRedial, redialMax, redialDelaySeconds)
                return
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission refusée pour l'appel: ${e.message}")
            finalStatus = CallStatus.FAILED
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'appel: ${e.message}")
            finalStatus = CallStatus.FAILED
        }

        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()

        withContext(Dispatchers.IO) {
            repository.addHistoryEntry(
                CallHistoryEntry(
                    scheduledCallId = callId,
                    label = label,
                    phoneNumber = phoneNumber,
                    status = finalStatus,
                    durationSeconds = durationSeconds,
                    attemptNumber = redialAttempt,
                    simSlot = simSlot
                )
            )
            repository.markCallCompleted(callId, finalStatus)
        }

        updateNotification(
            when (finalStatus) {
                CallStatus.COMPLETED -> "✅ Appel terminé : $label"
                CallStatus.BUSY -> "📵 Occupé : $label (max tentatives atteint)"
                CallStatus.FAILED -> "❌ Échec : $label"
                else -> "⚠️ $label - ${finalStatus.name}"
            }
        )

        delay(3000)
        stopSelf()
    }

    private fun placeCall(phoneNumber: String, simSlot: Int, dtmfExtension: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val fullNumber = if (dtmfExtension.isNotEmpty()) "$cleanNumber,,,,$dtmfExtension" else cleanNumber

        val uri = Uri.fromParts("tel", fullNumber, null)
        val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra("com.android.phone.extra.slot", simSlot)
                putExtra("Asus.Telecom.extra.slot", simSlot)
                putExtra("com.samsung.android.telecom.extra.slot", simSlot)
            }
        }

        startActivity(callIntent)

        if (dtmfExtension.isNotEmpty()) {
            Handler(Looper.getMainLooper()).postDelayed({
                sendDtmf(dtmfExtension)
            }, 5000)
        }
    }

    /**
     * Envoie des tonalités DTMF via ToneGenerator (API Android standard).
     */
    private fun sendDtmf(dtmf: String) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, ToneGenerator.MAX_VOLUME)
            for (char in dtmf) {
                val toneType = when (char) {
                    '0' -> ToneGenerator.TONE_DTMF_0
                    '1' -> ToneGenerator.TONE_DTMF_1
                    '2' -> ToneGenerator.TONE_DTMF_2
                    '3' -> ToneGenerator.TONE_DTMF_3
                    '4' -> ToneGenerator.TONE_DTMF_4
                    '5' -> ToneGenerator.TONE_DTMF_5
                    '6' -> ToneGenerator.TONE_DTMF_6
                    '7' -> ToneGenerator.TONE_DTMF_7
                    '8' -> ToneGenerator.TONE_DTMF_8
                    '9' -> ToneGenerator.TONE_DTMF_9
                    '#' -> ToneGenerator.TONE_DTMF_P
                    '*' -> ToneGenerator.TONE_DTMF_S
                    else -> {
                        Log.w(TAG, "Caractère DTMF ignoré: $char")
                        continue
                    }
                }
                toneGenerator.startTone(toneType, 200)
                Thread.sleep(300)
            }
            toneGenerator.release()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur DTMF: ${e.message}")
        }
    }

    private suspend fun monitorCallState(phoneNumber: String, timeoutSeconds: Int): CallStatus {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val timeout = System.currentTimeMillis() + (timeoutSeconds * 1000L + 30_000L)

        var wasConnected = false

        while (System.currentTimeMillis() < timeout) {
            delay(1000)
            val state = tm?.callState ?: TelephonyManager.CALL_STATE_IDLE

            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    wasConnected = true
                    updateNotification("🟢 Connecté...")
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    updateNotification("🔔 En train de sonner...")
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    return if (wasConnected) CallStatus.COMPLETED else CallStatus.BUSY
                }
            }
        }

        return if (wasConnected) CallStatus.COMPLETED else CallStatus.FAILED
    }

    // ── NOTIFICATIONS ─────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Exécution des appels",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pendant les appels automatiques"
                setShowBadge(true)
                enableLights(true)
                enableVibration(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📞 Planificateur d'appels")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
