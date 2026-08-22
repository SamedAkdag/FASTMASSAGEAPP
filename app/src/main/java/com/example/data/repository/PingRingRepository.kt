package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AlertEntity
import com.example.data.model.AlertStatus
import com.example.data.model.PairedContactEntity
import com.example.data.model.UserEntity
import com.example.data.remote.CloudRelayService
import com.example.data.remote.RemoteAlertMessage
import com.example.data.remote.RemoteIncomingEvent
import com.example.service.AlertNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class PingRingRepository(private val context: Context) {

    private val tag = "PingRingRepository"
    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val contactDao = db.contactDao()
    private val alertDao = db.alertDao()
    private val notificationManager = AlertNotificationManager(context)
    private val cloudRelay = CloudRelayService()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    // Active full-screen alert currently visible to the receiver
    private val _activeFullScreenAlert = MutableStateFlow<AlertEntity?>(null)
    val activeFullScreenAlert: StateFlow<AlertEntity?> = _activeFullScreenAlert.asStateFlow()

    // Test acceleration mode (allows testing 10s retries instead of waiting 3 minutes)
    val fastRetryMode = MutableStateFlow(false)

    // Tracks processed ntfy events to prevent duplicate processing
    private val processedEventIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private var streamJob: Job? = null
    private var pollJob: Job? = null

    init {
        startRetryTicker()
        startCloudSync()
    }

    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()

    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    fun getContactsFlow(): Flow<List<PairedContactEntity>> = contactDao.getAllContactsFlow()

    fun getAllAlertsFlow(): Flow<List<AlertEntity>> = alertDao.getAllAlertsFlow()

    fun getPendingIncomingAlertsFlow(): Flow<List<AlertEntity>> = alertDao.getPendingIncomingAlertsFlow()

    suspend fun registerOrLogin(phoneNumber: String, displayName: String): UserEntity {
        val existing = userDao.getCurrentUser()
        val pairingCode = existing?.pairingCode ?: generateUniquePairingCode()
        val user = UserEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            phoneNumber = phoneNumber.trim(),
            displayName = displayName.trim().ifEmpty { "Kullanıcı" },
            pairingCode = pairingCode
        )
        userDao.insertUser(user)

        // Publish profile immediately to Cloud Relay
        withContext(Dispatchers.IO) {
            cloudRelay.publishUserProfile(
                userId = user.id,
                displayName = user.displayName,
                phoneNumber = user.phoneNumber,
                pairingCode = user.pairingCode
            )
        }

        // Restart sync for this user
        startCloudSync()

        return user
    }

    suspend fun logout() {
        userDao.deleteAll()
        streamJob?.cancel()
        pollJob?.cancel()
    }

    suspend fun generateNewPairingCode(): String {
        val user = userDao.getCurrentUser() ?: return generateUniquePairingCode()
        val newCode = generateUniquePairingCode()
        val updatedUser = user.copy(pairingCode = newCode)
        userDao.insertUser(updatedUser)

        withContext(Dispatchers.IO) {
            cloudRelay.publishUserProfile(
                userId = updatedUser.id,
                displayName = updatedUser.displayName,
                phoneNumber = updatedUser.phoneNumber,
                pairingCode = updatedUser.pairingCode
            )
        }

        startCloudSync()
        return newCode
    }

    fun generateUniquePairingCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..2).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$part1-$part2"
    }

    suspend fun pairWithCode(rawCode: String, customName: String? = null): Result<PairedContactEntity> {
        val cleanCode = rawCode.filter { it.isLetterOrDigit() }.uppercase()
        if (cleanCode.length < 4) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir 6 haneli eşleştirme kodu girin (Örn: A7K9-42)"))
        }

        val formattedCode = cloudRelay.formatPairingCode(cleanCode)
        val currentUser = userDao.getCurrentUser()
        if (currentUser != null && cleanCode == cloudRelay.sanitizeCode(currentUser.pairingCode)) {
            return Result.failure(IllegalArgumentException("Kendi cihazınızın eşleştirme kodunu ekleyemezsiniz."))
        }

        // 1. Fetch live profile from cloud relay with retries
        var remoteProfile = cloudRelay.fetchUserProfile(formattedCode)
        if (remoteProfile == null) {
            delay(500L)
            remoteProfile = cloudRelay.fetchUserProfile(formattedCode)
        }
        if (remoteProfile == null) {
            delay(500L)
            remoteProfile = cloudRelay.fetchUserProfile(formattedCode)
        }

        // Strict verification: If the code is fake or does not exist on the cloud network, reject it!
        if (remoteProfile == null) {
            return Result.failure(
                IllegalArgumentException("'$formattedCode' koduna sahip kayıtlı bir kullanıcı bulunamadı.\nLütfen karşı tarafın uygulamayı açtığından ve kodunu doğru girdiğinizden emin olun.")
            )
        }

        val resolvedName = customName?.trim()?.ifEmpty { null }
            ?: remoteProfile.displayName.ifEmpty { null }
            ?: "Eşleşen Kişi ($formattedCode)"

        val resolvedPhone = remoteProfile.phoneNumber

        val existingContact = contactDao.getContactByPairingCode(formattedCode)
        val contactId = existingContact?.id ?: remoteProfile.userId.ifEmpty { null } ?: UUID.randomUUID().toString()

        val contact = PairedContactEntity(
            id = contactId,
            name = resolvedName,
            phoneNumber = resolvedPhone,
            pairingCode = formattedCode,
            isDefault = false
        )
        contactDao.insertContact(contact)

        // 2. Announce to target device over cloud relay so they automatically add us too!
        if (currentUser != null) {
            withContext(Dispatchers.IO) {
                cloudRelay.sendPairAnnounce(
                    targetPairingCode = formattedCode,
                    myUserId = currentUser.id,
                    myDisplayName = currentUser.displayName,
                    myPhoneNumber = currentUser.phoneNumber,
                    myPairingCode = currentUser.pairingCode
                )
            }
        }

        return Result.success(contact)
    }

    suspend fun deleteContact(contact: PairedContactEntity) {
        contactDao.deleteContact(contact)
    }

    suspend fun sendCriticalAlert(contact: PairedContactEntity, messageText: String): Result<AlertEntity> {
        val currentUser = userDao.getCurrentUser()
            ?: return Result.failure(IllegalStateException("Lütfen önce giriş yapın."))

        val trimmedMessage = messageText.trim().take(100)
        if (trimmedMessage.isEmpty()) {
            return Result.failure(IllegalArgumentException("Uyarı mesajı boş olamaz."))
        }

        val interval = if (fastRetryMode.value) 10L else 180L // 3 minutes standard (180s)
        val alertId = UUID.randomUUID().toString()
        val alert = AlertEntity(
            id = alertId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderPhone = currentUser.phoneNumber,
            senderPairingCode = currentUser.pairingCode,
            receiverId = contact.id,
            receiverName = contact.name,
            receiverPhone = contact.phoneNumber,
            receiverPairingCode = contact.pairingCode,
            message = trimmedMessage,
            status = AlertStatus.PENDING,
            attemptCount = 1,
            maxAttempts = 5,
            retryIntervalSeconds = interval,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = System.currentTimeMillis(),
            nextRetryAt = System.currentTimeMillis() + (interval * 1000L),
            isIncoming = false
        )

        // Save in local DB for sender tracking
        alertDao.insertAlert(alert)

        // Note: For SENDER, do NOT open the full screen receiver alert.
        // Send directly over internet to recipient device!
        val sent = withContext(Dispatchers.IO) {
            cloudRelay.sendEmergencyAlert(
                targetPairingCode = contact.pairingCode,
                alert = RemoteAlertMessage(
                    alertId = alert.id,
                    senderId = alert.senderId,
                    senderName = alert.senderName,
                    senderPhone = alert.senderPhone,
                    senderPairingCode = alert.senderPairingCode,
                    message = alert.message,
                    attemptCount = 1,
                    maxAttempts = 5,
                    retryIntervalSeconds = interval,
                    timestamp = alert.createdAt
                )
            )
        }
        Log.d(tag, "Emergency alert sent to ${contact.pairingCode}: $sent")

        return Result.success(alert)
    }

    suspend fun simulateIncomingAlert(
        senderName: String = "Acil Durum Kontağı",
        senderPhone: String = "+90 555 999 8877",
        message: String = "ACİL DURUM! Lütfen hemen bana ulaşın, yardıma ihtiyacım var!"
    ): AlertEntity {
        val currentUser = userDao.getCurrentUser()
        val interval = if (fastRetryMode.value) 10L else 180L
        val alert = AlertEntity(
            id = UUID.randomUUID().toString(),
            senderId = "incoming_sender_${System.currentTimeMillis()}",
            senderName = senderName,
            senderPhone = senderPhone,
            senderPairingCode = "DEMO-01",
            receiverId = currentUser?.id ?: "local_user",
            receiverName = currentUser?.displayName ?: "Ben",
            receiverPhone = currentUser?.phoneNumber ?: "",
            receiverPairingCode = currentUser?.pairingCode ?: "",
            message = message.trim().take(100),
            status = AlertStatus.PENDING,
            attemptCount = 1,
            maxAttempts = 5,
            retryIntervalSeconds = interval,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = System.currentTimeMillis(),
            nextRetryAt = System.currentTimeMillis() + (interval * 1000L),
            isIncoming = true
        )

        alertDao.insertAlert(alert)
        _activeFullScreenAlert.value = alert
        notificationManager.showEmergencyNotification(alert)
        return alert
    }

    suspend fun openAlertInFullScreen(alert: AlertEntity) {
        _activeFullScreenAlert.value = alert
    }

    suspend fun acknowledgeAlert(alertId: String) {
        // "Okudum" (I Read This): Deletes/cancels message completely and stops all retries
        val existing = alertDao.getAlertById(alertId)
        val currentUser = userDao.getCurrentUser()

        notificationManager.cancelNotification(alertId)
        alertDao.updateAlertStatus(alertId, AlertStatus.ACKNOWLEDGED)
        alertDao.deleteAlertById(alertId)

        if (_activeFullScreenAlert.value?.id == alertId) {
            _activeFullScreenAlert.value = null
        }

        // If this was an incoming alert from another phone, notify sender over the cloud!
        if (existing != null && existing.isIncoming && existing.senderPairingCode.isNotBlank()) {
            repositoryScope.launch {
                cloudRelay.sendAlertAcknowledgment(
                    senderPairingCode = existing.senderPairingCode,
                    alertId = alertId,
                    myPairingCode = currentUser?.pairingCode ?: ""
                )
            }
        }
    }

    fun dismissAlertScreen(alertId: String) {
        // "Kapat" (Dismiss): Closes screen only, leaves message in 'pending' status so 3-min retries continue
        if (_activeFullScreenAlert.value?.id == alertId) {
            _activeFullScreenAlert.value = null
        }
    }

    suspend fun cancelOutgoingAlert(alertId: String) {
        alertDao.deleteAlertById(alertId)
        notificationManager.cancelNotification(alertId)
    }

    private fun startCloudSync() {
        streamJob?.cancel()
        pollJob?.cancel()

        // 1. Continuous SSE Real-Time Stream Listener
        streamJob = repositoryScope.launch {
            while (isActive) {
                val user = userDao.getCurrentUser()
                if (user != null && user.pairingCode.isNotBlank()) {
                    try {
                        cloudRelay.listenToInboxStream(user.pairingCode) { event ->
                            handleIncomingCloudEvent(event, user)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Stream exception: ${e.message}")
                    }
                }
                delay(3000L) // Retry stream if disconnected
            }
        }

        // 2. Fallback Periodic Polling Loop & Profile Keepalive (Every 3 seconds)
        pollJob = repositoryScope.launch {
            while (isActive) {
                val user = userDao.getCurrentUser()
                if (user != null && user.pairingCode.isNotBlank()) {
                    // Refresh profile in cloud cache
                    cloudRelay.publishUserProfile(
                        userId = user.id,
                        displayName = user.displayName,
                        phoneNumber = user.phoneNumber,
                        pairingCode = user.pairingCode
                    )

                    // Poll inbox
                    try {
                        val events = cloudRelay.pollInbox(user.pairingCode)
                        for (event in events) {
                            handleIncomingCloudEvent(event, user)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Polling exception: ${e.message}")
                    }
                }
                delay(3000L)
            }
        }
    }

    private suspend fun handleIncomingCloudEvent(event: RemoteIncomingEvent, currentUser: UserEntity) {
        // Deduplicate event processing
        if (event.eventNtfyId.isNotBlank() && !processedEventIds.add(event.eventNtfyId)) {
            return
        }

        when (event) {
            is RemoteIncomingEvent.NewAlert -> {
                val raw = event.alert
                Log.d(tag, "Received NewAlert event: ${raw.alertId} from ${raw.senderName} (${raw.senderPairingCode})")
                val existing = alertDao.getAlertById(raw.alertId)
                if (existing == null) {
                    // Automatically add/update sender in contacts if not paired yet
                    val senderCode = cloudRelay.formatPairingCode(raw.senderPairingCode)
                    val existingContact = contactDao.getContactByPairingCode(senderCode)
                    if (existingContact == null && senderCode.isNotBlank()) {
                        contactDao.insertContact(
                            PairedContactEntity(
                                id = raw.senderId.ifEmpty { UUID.randomUUID().toString() },
                                name = raw.senderName.ifEmpty { "Eşleşen Kişi ($senderCode)" },
                                phoneNumber = raw.senderPhone,
                                pairingCode = senderCode
                            )
                        )
                    }

                    val incomingAlert = AlertEntity(
                        id = raw.alertId,
                        senderId = raw.senderId,
                        senderName = raw.senderName,
                        senderPhone = raw.senderPhone,
                        senderPairingCode = senderCode,
                        receiverId = currentUser.id,
                        receiverName = currentUser.displayName,
                        receiverPhone = currentUser.phoneNumber,
                        receiverPairingCode = currentUser.pairingCode,
                        message = raw.message,
                        status = AlertStatus.PENDING,
                        attemptCount = raw.attemptCount,
                        maxAttempts = raw.maxAttempts,
                        retryIntervalSeconds = raw.retryIntervalSeconds,
                        createdAt = raw.timestamp,
                        lastAttemptAt = System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + (raw.retryIntervalSeconds * 1000L),
                        isIncoming = true
                    )

                    alertDao.insertAlert(incomingAlert)
                    // Trigger full screen red alert and loud alarm on RECEIVER
                    _activeFullScreenAlert.value = incomingAlert
                    notificationManager.showEmergencyNotification(incomingAlert)
                } else if (existing.status == AlertStatus.PENDING) {
                    // If alert is already pending on receiver (e.g. dismissed previously with 'Kapat'), re-trigger!
                    val updated = existing.copy(
                        attemptCount = raw.attemptCount,
                        lastAttemptAt = System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + (raw.retryIntervalSeconds * 1000L)
                    )
                    alertDao.updateAlert(updated)
                    _activeFullScreenAlert.value = updated
                    notificationManager.showEmergencyNotification(updated)
                }
            }

            is RemoteIncomingEvent.PairAnnounce -> {
                val profile = event.profile
                val formattedPairingCode = cloudRelay.formatPairingCode(profile.pairingCode)
                val existing = contactDao.getContactByPairingCode(formattedPairingCode)
                if (existing == null && formattedPairingCode != currentUser.pairingCode) {
                    contactDao.insertContact(
                        PairedContactEntity(
                            id = profile.userId.ifEmpty { UUID.randomUUID().toString() },
                            name = profile.displayName.ifEmpty { "Eşleşen Cihaz ($formattedPairingCode)" },
                            phoneNumber = profile.phoneNumber,
                            pairingCode = formattedPairingCode
                        )
                    )
                }
            }

            is RemoteIncomingEvent.AlertAcknowledged -> {
                val alert = alertDao.getAlertById(event.alertId)
                if (alert != null) {
                    notificationManager.cancelNotification(alert.id)
                    alertDao.updateAlertStatus(alert.id, AlertStatus.ACKNOWLEDGED)
                    alertDao.deleteAlertById(alert.id)
                }
            }
        }
    }

    private fun startRetryTicker() {
        repositoryScope.launch {
            while (isActive) {
                delay(2000L) // check every 2 seconds
                try {
                    val now = System.currentTimeMillis()
                    val pendingAlerts = alertDao.getPendingAlerts()
                    for (alert in pendingAlerts) {
                        if (alert.status == AlertStatus.PENDING && now >= alert.nextRetryAt) {
                            val nextAttempt = alert.attemptCount + 1
                            if (nextAttempt <= alert.maxAttempts) {
                                val interval = if (fastRetryMode.value) 10L else alert.retryIntervalSeconds
                                val nextRetry = now + (interval * 1000L)
                                alertDao.updateRetryProgress(
                                    id = alert.id,
                                    attemptCount = nextAttempt,
                                    lastAttemptAt = now,
                                    nextRetryAt = nextRetry,
                                    status = AlertStatus.PENDING
                                )
                                val updated = alert.copy(
                                    attemptCount = nextAttempt,
                                    lastAttemptAt = now,
                                    nextRetryAt = nextRetry
                                )

                                if (updated.isIncoming) {
                                    // Receiver phone: re-trigger loud sound & full screen alert
                                    notificationManager.showEmergencyNotification(updated)
                                    _activeFullScreenAlert.value = updated
                                } else {
                                    // Sender phone: re-dispatch over cloud relay to target receiver
                                    if (updated.receiverPairingCode.isNotBlank()) {
                                        cloudRelay.sendEmergencyAlert(
                                            targetPairingCode = updated.receiverPairingCode,
                                            alert = RemoteAlertMessage(
                                                alertId = updated.id,
                                                senderId = updated.senderId,
                                                senderName = updated.senderName,
                                                senderPhone = updated.senderPhone,
                                                senderPairingCode = updated.senderPairingCode,
                                                message = updated.message,
                                                attemptCount = nextAttempt,
                                                maxAttempts = updated.maxAttempts,
                                                retryIntervalSeconds = interval,
                                                timestamp = updated.createdAt
                                            )
                                        )
                                    }
                                }
                            } else {
                                // 5th retry reached -> expired/discontinued
                                alertDao.updateAlertStatus(alert.id, AlertStatus.EXPIRED)
                                notificationManager.cancelNotification(alert.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Safe error handling
                }
            }
        }
    }
}
