package com.aistudio.pingring.pgrng.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.pingring.pgrng.data.model.AlertEntity
import com.aistudio.pingring.pgrng.data.model.AlertStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM active_alerts ORDER BY createdAt DESC")
    fun getAllAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM active_alerts WHERE isIncoming = 1 AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingIncomingAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM active_alerts WHERE isIncoming = 1 AND status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    fun getLatestActiveIncomingAlertFlow(): Flow<AlertEntity?>

    @Query("SELECT * FROM active_alerts WHERE status = 'PENDING'")
    suspend fun getPendingAlerts(): List<AlertEntity>

    @Query("SELECT * FROM active_alerts WHERE id = :id LIMIT 1")
    suspend fun getAlertById(id: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE active_alerts SET status = :newStatus WHERE id = :id")
    suspend fun updateAlertStatus(id: String, newStatus: AlertStatus)

    @Query("UPDATE active_alerts SET attemptCount = :attemptCount, lastAttemptAt = :lastAttemptAt, nextRetryAt = :nextRetryAt, status = :status WHERE id = :id")
    suspend fun updateRetryProgress(id: String, attemptCount: Int, lastAttemptAt: Long, nextRetryAt: Long, status: AlertStatus)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM active_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: String)

    @Query("DELETE FROM active_alerts WHERE status = 'ACKNOWLEDGED'")
    suspend fun deleteAcknowledgedAlerts()
}
