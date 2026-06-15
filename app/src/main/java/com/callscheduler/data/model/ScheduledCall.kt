package com.callscheduler.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Modèle d'un appel planifié.
 * Supporte : appels uniques, répétitions infinies, SIM duale, groupes.
 */
@Entity(tableName = "scheduled_calls")
data class ScheduledCall(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Identité
    val label: String,                          // Ex: "Razik Fin"
    val phoneNumber: String,                    // Numéro complet
    val dtmfExtension: String = "",             // Poste (ex: "739286") envoyé en DTMF
    val contactName: String = "",               // Nom du contact si trouvé
    val groupTag: String = "",                  // Regrouper les appels (ex: "Razik")

    // Planification
    val scheduledHour: Int,                     // Heure (0-23)
    val scheduledMinute: Int,                   // Minute (0-59)
    val startDate: Long,                        // Timestamp de début (ms)
    val endDate: Long = Long.MAX_VALUE,         // Timestamp de fin (ms), MAX = illimité
    val repeatMode: RepeatMode = RepeatMode.DAILY, // Mode de répétition
    val repeatDays: String = "1,2,3,4,5,6,7",  // Jours (1=Lun...7=Dim), séparés par virgule
    val repeatIntervalDays: Int = 1,            // Toutes les N jours

    // SIM
    val simSlot: Int = 0,                       // 0 = SIM1, 1 = SIM2, -1 = par défaut

    // Comportement
    val isEnabled: Boolean = true,
    val autoRedialOnBusy: Boolean = true,       // Recomposer si occupé
    val autoRedialMaxAttempts: Int = 3,         // Max tentatives
    val autoRedialDelaySeconds: Int = 60,       // Délai entre tentatives (sec)
    val callDurationSeconds: Int = 0,           // 0 = pas de limite
    val dtmfDelaySeconds: Int = 3,              // Délai avant envoi DTMF

    // Stats
    val totalCallsMade: Int = 0,
    val lastCallTimestamp: Long = 0L,
    val lastCallStatus: CallStatus = CallStatus.PENDING,
    val nextCallTimestamp: Long = 0L,

    // Métadonnées
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val color: Int = 0,                         // Couleur personnalisée (ARGB)
    val priority: Int = 0,                      // Priorité (0=normal, 1=haute, 2=urgente)
)

enum class RepeatMode {
    ONCE,           // Une seule fois
    DAILY,          // Chaque jour
    WEEKLY,         // Chaque semaine
    CUSTOM_DAYS,    // Jours personnalisés
    INTERVAL,       // Toutes les N jours
    WEEKDAYS,       // Lundi-Vendredi
    WEEKENDS        // Samedi-Dimanche
}

enum class CallStatus {
    PENDING,        // En attente
    CALLING,        // En cours d'appel
    CONNECTED,      // Connecté
    COMPLETED,      // Terminé avec succès
    BUSY,           // Occupé
    FAILED,         // Échoué
    CANCELLED,      // Annulé
    REDIALING       // En recomposition
}

/**
 * Historique des appels effectués
 */
@Entity(tableName = "call_history")
data class CallHistoryEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scheduledCallId: String,
    val label: String,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: CallStatus,
    val durationSeconds: Int = 0,
    val attemptNumber: Int = 1,
    val simSlot: Int = 0,
    val errorMessage: String = ""
)
