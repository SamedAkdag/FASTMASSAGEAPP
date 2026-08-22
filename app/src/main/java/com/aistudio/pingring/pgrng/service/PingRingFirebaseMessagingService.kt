package com.aistudio.pingring.pgrng.service

import android.util.Log
import com.aistudio.pingring.pgrng.data.local.AppDatabase
import com.aistudio.pingring.pgrng.data.model.AlertEntity
import com.aistudio.pingring.pgrng.data.model.AlertStatus
import com.aistudio.pingring.pgrng.data.model.PairedContactEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Firebase Cloud Messaging Service to receive background push notifications
 * when the app is completely closed/killed.
 */
class PingRingFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "PingRingFCMService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "Refreshed FCM Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(tag, "FCM Message received: ${remoteMessage.data}")

        val data = remoteMessage.data
        val type = data["type"] ?: return

        val context = applicationContext
        val db = AppDatabase.getInstance(context)
        val alertDao = db.alertDao()
        val contactDao = db.contactDao()
        val userDao = db.userDao()
        val notificationManager = AlertNotificationManager(context)

        serviceScope.launch {
            val currentUser = userDao.getCurrentUser()

            when (type) {
                "EMERGENCY_ALERT" -> {
                    val alertId = data["alertId"] ?: UUID.randomUUID().toString()
                    val senderId = data["senderId"] ?: ""
                    val senderName = data["senderName"] ?: "Acil Durum Gönderen"
                    val senderPhone = data["senderPhone"] ?: ""
                    val senderPairingCode = data["senderPairingCode"] ?: ""
                    val message = data["message"] ?: "ACİL UYARI!"
                    val attemptCount = data["attemptCount"]?.toIntOrNull() ?: 1
                    val maxAttempts = data["maxAttempts"]?.toIntOrNull() ?: 5
                    val retryIntervalSeconds = data["retryIntervalSeconds"]?.toLongOrNull() ?: 180L
                    val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

                    // Auto-save contact if new
                    if (senderPairingCode.isNotBlank()) {
                        val existingContact = contactDao.getContactByPairingCode(senderPairingCode)
                        if (existingContact == null) {
                            contactDao.insertContact(
                                PairedContactEntity(
                                    id = senderId.ifEmpty { UUID.randomUUID().toString() },
                                    name = senderName,
                                    phoneNumber = senderPhone,
                                    pairingCode = senderPairingCode
                                )
                            )
                        }
                    }

                    val incomingAlert = AlertEntity(
                        id = alertId,
                        senderId = senderId,
                        senderName = senderName,
                        senderPhone = senderPhone,
                        senderPairingCode = senderPairingCode,
                        receiverId = currentUser?.id ?: "local_user",
                        receiverName = currentUser?.displayName ?: "Ben",
                        receiverPhone = currentUser?.phoneNumber ?: "",
                        receiverPairingCode = currentUser?.pairingCode ?: "",
                        message = message,
                        status = AlertStatus.PENDING,
                        attemptCount = attemptCount,
                        maxAttempts = maxAttempts,
                        retryIntervalSeconds = retryIntervalSeconds,
                        createdAt = timestamp,
                        lastAttemptAt = System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + (retryIntervalSeconds * 1000L),
                        isIncoming = true
                    )

                    alertDao.insertAlert(incomingAlert)

                    // Wake up screen, ring full-screen alarm notification even if killed
                    notificationManager.showEmergencyNotification(incomingAlert)
                }

                "ALERT_ACKNOWLEDGED" -> {
                    val alertId = data["alertId"] ?: return@launch
                    notificationManager.cancelNotification(alertId)
                    alertDao.updateAlertStatus(alertId, AlertStatus.ACKNOWLEDGED)
                    alertDao.deleteAlertById(alertId)
                }
            }
        }
    }
}
