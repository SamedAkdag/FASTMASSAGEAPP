package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class RemoteUserProfile(
    val userId: String,
    val displayName: String,
    val phoneNumber: String,
    val pairingCode: String,
    val updatedAt: Long
)

data class RemoteAlertMessage(
    val alertId: String,
    val senderId: String,
    val senderName: String,
    val senderPhone: String,
    val senderPairingCode: String,
    val message: String,
    val attemptCount: Int,
    val maxAttempts: Int,
    val retryIntervalSeconds: Long,
    val timestamp: Long
)

sealed interface RemoteIncomingEvent {
    val eventNtfyId: String
    data class NewAlert(override val eventNtfyId: String, val alert: RemoteAlertMessage) : RemoteIncomingEvent
    data class AlertAcknowledged(override val eventNtfyId: String, val alertId: String, val senderPairingCode: String) : RemoteIncomingEvent
    data class PairAnnounce(override val eventNtfyId: String, val profile: RemoteUserProfile) : RemoteIncomingEvent
}

class CloudRelayService {

    private val tag = "CloudRelayService"
    private val rawPayloadMediaType = "text/plain; charset=utf-8".toMediaType()

    private val ipv4PreferringDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4 = addresses.filterIsInstance<Inet4Address>()
                if (ipv4.isNotEmpty()) ipv4 else addresses
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectionPool(okhttp3.ConnectionPool(5, 30, TimeUnit.SECONDS))
        .dns(ipv4PreferringDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val streamingClient = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectionPool(okhttp3.ConnectionPool(2, 5, TimeUnit.MINUTES))
        .dns(ipv4PreferringDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for SSE stream
        .retryOnConnectionFailure(true)
        .build()

    fun sanitizeCode(code: String): String {
        return code.filter { it.isLetterOrDigit() }.uppercase()
    }

    fun formatPairingCode(raw: String): String {
        val clean = sanitizeCode(raw)
        return if (clean.length == 6) {
            "${clean.substring(0, 4)}-${clean.substring(4, 6)}"
        } else {
            clean
        }
    }

    private fun getProfileTopic(sanitizedCode: String): String = "pingring_prof_${sanitizedCode.lowercase()}"
    private fun getInboxTopic(sanitizedCode: String): String = "pingring_inbox_${sanitizedCode.lowercase()}"

    /**
     * Publishes current user's profile to cloud relay cache so any device
     * looking for this pairing code can resolve their name and phone.
     */
    suspend fun publishUserProfile(
        userId: String,
        displayName: String,
        phoneNumber: String,
        pairingCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        val sanitized = sanitizeCode(pairingCode)
        if (sanitized.isEmpty()) return@withContext false

        val topic = getProfileTopic(sanitized)
        val json = JSONObject().apply {
            put("type", "USER_PROFILE")
            put("userId", userId)
            put("displayName", displayName)
            put("phoneNumber", phoneNumber)
            put("pairingCode", formatPairingCode(pairingCode))
            put("updatedAt", System.currentTimeMillis())
        }

        try {
            val url = "https://ntfy.sh/$topic"
            val body = json.toString().toRequestBody(rawPayloadMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .addHeader("X-Title", "PingRing Profile")
                .addHeader("X-Tags", "identification_card")
                .build()

            httpClient.newCall(request).execute().use { response ->
                Log.d(tag, "publishUserProfile to $topic: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to publish user profile for $pairingCode: ${e.message}")
            false
        }
    }

    private fun extractJsonObjects(raw: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return results

        val lines = trimmed.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            var braceDepth = 0
            var startIndex = -1
            for (i in line.indices) {
                val c = line[i]
                if (c == '{') {
                    if (braceDepth == 0) startIndex = i
                    braceDepth++
                } else if (c == '}') {
                    braceDepth--
                    if (braceDepth == 0 && startIndex != -1) {
                        val candidate = line.substring(startIndex, i + 1)
                        try {
                            results.add(JSONObject(candidate))
                        } catch (_: Exception) {}
                        startIndex = -1
                    }
                }
            }
        }
        return results
    }

    /**
     * Fetches the user profile associated with a given pairing code.
     */
    suspend fun fetchUserProfile(pairingCode: String): RemoteUserProfile? = withContext(Dispatchers.IO) {
        val sanitized = sanitizeCode(pairingCode)
        if (sanitized.isEmpty()) return@withContext null

        val topic = getProfileTopic(sanitized)
        try {
            val url = "https://ntfy.sh/$topic/json?poll=1&since=all"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(tag, "fetchUserProfile HTTP ${response.code} for $topic")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null

                val jsonObjects = extractJsonObjects(bodyString)
                val profiles = mutableListOf<RemoteUserProfile>()
                for (eventObj in jsonObjects) {
                    try {
                        if (eventObj.optString("event") == "message") {
                            val msgStr = eventObj.optString("message")
                            val data = if (msgStr.startsWith("{")) JSONObject(msgStr) else eventObj
                            if (data.optString("type") == "USER_PROFILE") {
                                val profile = RemoteUserProfile(
                                    userId = data.optString("userId"),
                                    displayName = data.optString("displayName"),
                                    phoneNumber = data.optString("phoneNumber"),
                                    pairingCode = data.optString("pairingCode", formatPairingCode(pairingCode)),
                                    updatedAt = data.optLong("updatedAt", System.currentTimeMillis())
                                )
                                profiles.add(profile)
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed
                    }
                }
                val latest = profiles.maxByOrNull { it.updatedAt }
                if (latest != null) {
                    Log.d(tag, "Found remote profile for $sanitized: ${latest.displayName} (updatedAt=${latest.updatedAt})")
                    return@withContext latest
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch profile for $pairingCode: ${e.message}")
        }
        null
    }

    /**
     * Sends a mutual pairing announcement to the target user's inbox.
     */
    suspend fun sendPairAnnounce(
        targetPairingCode: String,
        myUserId: String,
        myDisplayName: String,
        myPhoneNumber: String,
        myPairingCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        val targetSanitized = sanitizeCode(targetPairingCode)
        if (targetSanitized.isEmpty()) return@withContext false

        val topic = getInboxTopic(targetSanitized)
        val json = JSONObject().apply {
            put("type", "PAIR_ANNOUNCE")
            put("userId", myUserId)
            put("displayName", myDisplayName)
            put("phoneNumber", myPhoneNumber)
            put("pairingCode", formatPairingCode(myPairingCode))
            put("timestamp", System.currentTimeMillis())
        }

        try {
            val url = "https://ntfy.sh/$topic"
            val body = json.toString().toRequestBody(rawPayloadMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .addHeader("X-Title", "PingRing Device Paired")
                .addHeader("X-Priority", "4")
                .build()

            httpClient.newCall(request).execute().use { response ->
                Log.d(tag, "sendPairAnnounce to $topic: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send pair announce to $targetPairingCode: ${e.message}")
            false
        }
    }

    /**
     * Sends a Critical Emergency Alert to the recipient's inbox.
     */
    suspend fun sendEmergencyAlert(
        targetPairingCode: String,
        alert: RemoteAlertMessage
    ): Boolean = withContext(Dispatchers.IO) {
        val targetSanitized = sanitizeCode(targetPairingCode)
        if (targetSanitized.isEmpty()) {
            Log.e(tag, "sendEmergencyAlert failed: targetPairingCode is empty ($targetPairingCode)")
            return@withContext false
        }

        val topic = getInboxTopic(targetSanitized)
        val json = JSONObject().apply {
            put("type", "EMERGENCY_ALERT")
            put("alertId", alert.alertId)
            put("senderId", alert.senderId)
            put("senderName", alert.senderName)
            put("senderPhone", alert.senderPhone)
            put("senderPairingCode", formatPairingCode(alert.senderPairingCode))
            put("message", alert.message)
            put("attemptCount", alert.attemptCount)
            put("maxAttempts", alert.maxAttempts)
            put("retryIntervalSeconds", alert.retryIntervalSeconds)
            put("timestamp", alert.timestamp)
        }

        try {
            val url = "https://ntfy.sh/$topic"
            val body = json.toString().toRequestBody(rawPayloadMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .addHeader("X-Title", "CRITICAL EMERGENCY ALERT")
                .addHeader("X-Priority", "5")
                .addHeader("X-Tags", "warning,rotating_light")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d(tag, "sendEmergencyAlert to topic $topic response code: ${response.code}, success: $success")
                success
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send emergency alert to $targetPairingCode ($topic): ${e.message}", e)
            false
        }
    }

    /**
     * Sends an Acknowledgment ("Okudum") back to the sender to cancel retries.
     */
    suspend fun sendAlertAcknowledgment(
        senderPairingCode: String,
        alertId: String,
        myPairingCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        val senderSanitized = sanitizeCode(senderPairingCode)
        if (senderSanitized.isEmpty()) return@withContext false

        val topic = getInboxTopic(senderSanitized)
        val json = JSONObject().apply {
            put("type", "ALERT_ACKNOWLEDGED")
            put("alertId", alertId)
            put("senderPairingCode", formatPairingCode(myPairingCode))
            put("timestamp", System.currentTimeMillis())
        }

        try {
            val url = "https://ntfy.sh/$topic"
            val body = json.toString().toRequestBody(rawPayloadMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .addHeader("X-Title", "Alert Acknowledged")
                .addHeader("X-Priority", "3")
                .build()

            httpClient.newCall(request).execute().use { response ->
                Log.d(tag, "sendAlertAcknowledgment to $topic: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send acknowledgment for $alertId: ${e.message}")
            false
        }
    }

    /**
     * Polls the inbox topic for recent messages.
     */
    suspend fun pollInbox(myPairingCode: String): List<RemoteIncomingEvent> = withContext(Dispatchers.IO) {
        val sanitized = sanitizeCode(myPairingCode)
        if (sanitized.isEmpty()) return@withContext emptyList()

        val topic = getInboxTopic(sanitized)
        val events = mutableListOf<RemoteIncomingEvent>()

        try {
            val url = "https://ntfy.sh/$topic/json?poll=1&since=all"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext events
                val bodyString = response.body?.string() ?: return@withContext events

                val jsonObjects = extractJsonObjects(bodyString)
                for (eventObj in jsonObjects) {
                    try {
                        if (eventObj.optString("event") == "message") {
                            val eventId = eventObj.optString("id")
                            val msgStr = eventObj.optString("message")
                            val parsed = parseIncomingJson(eventId, msgStr)
                            if (parsed != null) {
                                events.add(parsed)
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed polling inbox for $myPairingCode: ${e.message}")
        }
        events
    }

    /**
     * Streams incoming events continuously from ntfy via Server-Sent Events (SSE).
     */
    suspend fun listenToInboxStream(
        myPairingCode: String,
        onEvent: suspend (RemoteIncomingEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        val sanitized = sanitizeCode(myPairingCode)
        if (sanitized.isEmpty()) return@withContext

        val topic = getInboxTopic(sanitized)
        // Use live mode for real-time streaming, with poll=1 to get new messages as they arrive
        val url = "https://ntfy.sh/$topic/json?live"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "PingRing-App/1.0")
                .get()
                .build()

            Log.d(tag, "Starting SSE stream for topic: $topic")
            streamingClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(tag, "SSE stream failed with HTTP ${response.code}")
                    return@withContext
                }
                Log.d(tag, "SSE stream connected successfully")
                val stream = response.body?.byteStream() ?: return@withContext
                val reader = BufferedReader(InputStreamReader(stream))

                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        try {
                            val eventObj = JSONObject(line)
                            if (eventObj.optString("event") == "message") {
                                val eventId = eventObj.optString("id")
                                val msgStr = eventObj.optString("message")
                                val parsed = parseIncomingJson(eventId, msgStr)
                                if (parsed != null) {
                                    onEvent(parsed)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing stream json line: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Stream disconnected for $myPairingCode (${e.message})", e)
        }
    }

    private fun parseIncomingJson(eventId: String, rawJson: String): RemoteIncomingEvent? {
        return try {
            val data = JSONObject(rawJson)
            when (data.optString("type")) {
                "EMERGENCY_ALERT" -> {
                    val alert = RemoteAlertMessage(
                        alertId = data.getString("alertId"),
                        senderId = data.optString("senderId"),
                        senderName = data.optString("senderName", "Acil Durum Gönderen"),
                        senderPhone = data.optString("senderPhone", ""),
                        senderPairingCode = data.optString("senderPairingCode"),
                        message = data.getString("message"),
                        attemptCount = data.optInt("attemptCount", 1),
                        maxAttempts = data.optInt("maxAttempts", 5),
                        retryIntervalSeconds = data.optLong("retryIntervalSeconds", 180L),
                        timestamp = data.optLong("timestamp", System.currentTimeMillis())
                    )
                    RemoteIncomingEvent.NewAlert(eventId, alert)
                }
                "ALERT_ACKNOWLEDGED" -> {
                    val alertId = data.getString("alertId")
                    val senderPairingCode = data.optString("senderPairingCode", "")
                    RemoteIncomingEvent.AlertAcknowledged(eventId, alertId, senderPairingCode)
                }
                "PAIR_ANNOUNCE" -> {
                    val profile = RemoteUserProfile(
                        userId = data.optString("userId"),
                        displayName = data.optString("displayName", "Eşleşen Cihaz"),
                        phoneNumber = data.optString("phoneNumber", ""),
                        pairingCode = data.optString("pairingCode", ""),
                        updatedAt = data.optLong("timestamp", System.currentTimeMillis())
                    )
                    RemoteIncomingEvent.PairAnnounce(eventId, profile)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
