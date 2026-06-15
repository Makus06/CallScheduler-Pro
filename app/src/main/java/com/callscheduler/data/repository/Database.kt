package com.callscheduler.data.repository

import androidx.room.*
import com.callscheduler.data.model.CallHistoryEntry
import com.callscheduler.data.model.CallStatus
import com.callscheduler.data.model.RepeatMode
import com.callscheduler.data.model.ScheduledCall
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
// TYPE CONVERTERS
// ─────────────────────────────────────────────

class Converters {
    @TypeConverter fun fromRepeatMode(v: RepeatMode): String = v.name
    @TypeConverter fun toRepeatMode(v: String): RepeatMode = RepeatMode.valueOf(v)
    @TypeConverter fun fromCallStatus(v: CallStatus): String = v.name
    @TypeConverter fun toCallStatus(v: String): CallStatus = CallStatus.valueOf(v)
}

// ─────────────────────────────────────────────
// DAO — APPELS PLANIFIÉS
// ─────────────────────────────────────────────

@Dao
interface ScheduledCallDao {

    @Query("SELECT * FROM scheduled_calls ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getAllCalls(): Flow<List<ScheduledCall>>

    @Query("SELECT * FROM scheduled_calls WHERE isEnabled = 1 ORDER BY nextCallTimestamp ASC")
    fun getEnabledCalls(): Flow<List<ScheduledCall>>

    @Query("SELECT * FROM scheduled_calls WHERE id = :id")
    suspend fun getCallById(id: String): ScheduledCall?

    @Query("SELECT * FROM scheduled_calls WHERE isEnabled = 1 AND nextCallTimestamp <= :now AND nextCallTimestamp > 0")
    suspend fun getDueCallsNow(now: Long): List<ScheduledCall>

    @Query("SELECT * FROM scheduled_calls WHERE groupTag = :tag ORDER BY scheduledHour ASC")
    fun getCallsByGroup(tag: String): Flow<List<ScheduledCall>>

    @Query("SELECT DISTINCT groupTag FROM scheduled_calls WHERE groupTag != ''")
    fun getAllGroups(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: ScheduledCall)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<ScheduledCall>)

    @Update
    suspend fun updateCall(call: ScheduledCall)

    @Delete
    suspend fun deleteCall(call: ScheduledCall)

    @Query("DELETE FROM scheduled_calls WHERE id = :id")
    suspend fun deleteCallById(id: String)

    @Query("UPDATE scheduled_calls SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE scheduled_calls SET lastCallStatus = :status, lastCallTimestamp = :ts, totalCallsMade = totalCallsMade + 1 WHERE id = :id")
    suspend fun updateCallStats(id: String, status: CallStatus, ts: Long)

    @Query("UPDATE scheduled_calls SET nextCallTimestamp = :next WHERE id = :id")
    suspend fun updateNextCallTimestamp(id: String, next: Long)

    @Query("SELECT COUNT(*) FROM scheduled_calls WHERE isEnabled = 1")
    fun getEnabledCount(): Flow<Int>

    @Query("SELECT SUM(totalCallsMade) FROM scheduled_calls")
    fun getTotalCallsMade(): Flow<Int>
}

// ─────────────────────────────────────────────
// DAO — HISTORIQUE
// ─────────────────────────────────────────────

@Dao
interface CallHistoryDao {

    @Query("SELECT * FROM call_history ORDER BY timestamp DESC LIMIT 200")
    fun getRecentHistory(): Flow<List<CallHistoryEntry>>

    @Query("SELECT * FROM call_history WHERE scheduledCallId = :id ORDER BY timestamp DESC")
    fun getHistoryForCall(id: String): Flow<List<CallHistoryEntry>>

    @Query("SELECT COUNT(*) FROM call_history WHERE status = 'COMPLETED' AND timestamp >= :since")
    fun getSuccessCountSince(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CallHistoryEntry)

    @Query("DELETE FROM call_history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM call_history")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────
// DATABASE
// ─────────────────────────────────────────────

@Database(
    entities = [ScheduledCall::class, CallHistoryEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CallSchedulerDatabase : RoomDatabase() {
    abstract fun scheduledCallDao(): ScheduledCallDao
    abstract fun callHistoryDao(): CallHistoryDao

    companion object {
        @Volatile private var INSTANCE: CallSchedulerDatabase? = null

        fun getInstance(context: android.content.Context): CallSchedulerDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CallSchedulerDatabase::class.java,
                    "call_scheduler.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
