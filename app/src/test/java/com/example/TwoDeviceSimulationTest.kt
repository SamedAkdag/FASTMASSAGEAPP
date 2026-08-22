package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.UserEntity
import com.example.data.remote.CloudRelayService
import com.example.data.remote.RemoteAlertMessage
import com.example.data.remote.RemoteIncomingEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TwoDeviceSimulationTest {

    @Test
    fun `simulate two devices pairing and emergency alert exchange`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val userDao = db.userDao()
        val cloudRelay = CloudRelayService()

        // 1. Setup Device 1 (Target User: "Ahmet", Code: "S1XX-YY")
        val rand = kotlin.random.Random.nextInt(10, 99)
        val dev1Code = "S1$rand-AA"
        val dev1User = UserEntity(
            id = "user-dev1-${UUID.randomUUID()}",
            displayName = "Ahmet (Cihaz 1)",
            phoneNumber = "+905551112233",
            pairingCode = dev1Code
        )
        userDao.insertUser(dev1User)

        // Publish Dev1 profile to Cloud Relay
        val publishSuccess = cloudRelay.publishUserProfile(
            userId = dev1User.id,
            displayName = dev1User.displayName,
            phoneNumber = dev1User.phoneNumber,
            pairingCode = dev1User.pairingCode
        )
        println("Device 1 Profile published: $publishSuccess")
        assertTrue("Device 1 profile publish must succeed", publishSuccess)

        // 2. Setup Device 2 (Sender User: "Mehmet", Code: "S2XX-BB")
        val dev2Code = "S2$rand-BB"
        val dev2User = UserEntity(
            id = "user-dev2-${UUID.randomUUID()}",
            displayName = "Mehmet (Cihaz 2)",
            phoneNumber = "+905554445566",
            pairingCode = dev2Code
        )

        // Give 1 second for cloud relay message propagation
        delay(1000L)

        // 3. Device 2 fetches Dev1's profile from Cloud Relay (Simulation of Pairing verification)
        var fetchedProfile = cloudRelay.fetchUserProfile(dev1Code)
        if (fetchedProfile == null) {
            delay(1000L)
            fetchedProfile = cloudRelay.fetchUserProfile(dev1Code)
        }
        println("Device 2 fetched Dev1 profile: $fetchedProfile")
        assertNotNull("Dev1 profile should be reachable via cloud relay", fetchedProfile)
        assertEquals("Ahmet (Cihaz 1)", fetchedProfile?.displayName)

        // 4. Device 2 sends an Emergency Alert to Device 1
        val alertId = UUID.randomUUID().toString()
        val remoteAlert = RemoteAlertMessage(
            alertId = alertId,
            senderId = dev2User.id,
            senderName = dev2User.displayName,
            senderPhone = dev2User.phoneNumber,
            senderPairingCode = dev2User.pairingCode,
            message = "Lütfen acil bana ulaş!",
            attemptCount = 1,
            maxAttempts = 5,
            retryIntervalSeconds = 10,
            timestamp = System.currentTimeMillis()
        )

        val alertSent = cloudRelay.sendEmergencyAlert(
            targetPairingCode = dev1Code,
            alert = remoteAlert
        )
        println("Device 2 sent emergency alert to Device 1: $alertSent")
        assertTrue("Emergency alert send must succeed", alertSent)

        // 5. Device 1 queries its inbox on Cloud Relay (with retry for network propagation)
        var alertEvent: RemoteIncomingEvent.NewAlert? = null
        var attempts = 0
        while (alertEvent == null && attempts < 5) {
            delay(800L)
            val incomingEvents = cloudRelay.pollInbox(dev1Code)
            println("Device 1 inbox events count (attempt $attempts): ${incomingEvents.size}")
            alertEvent = incomingEvents.filterIsInstance<RemoteIncomingEvent.NewAlert>()
                .find { it.alert.alertId == alertId }
            attempts++
        }

        assertNotNull("Device 1 must receive the emergency alert in its inbox", alertEvent)
        assertEquals("Mehmet (Cihaz 2)", alertEvent?.alert?.senderName)
        assertEquals("Lütfen acil bana ulaş!", alertEvent?.alert?.message)

        // 6. Device 1 sends Acknowledgment (ACK / Okudum) back to Device 2
        val ackSent = cloudRelay.sendAlertAcknowledgment(
            senderPairingCode = dev2Code,
            alertId = alertId,
            myPairingCode = dev1Code
        )
        println("Device 1 sent ACK to Device 2: $ackSent")
        assertTrue("ACK send must succeed", ackSent)

        delay(600)

        // 7. Device 2 queries its inbox on Cloud Relay for the ACK
        var dev2Events = cloudRelay.pollInbox(dev2Code)
        var ackEvent = dev2Events.filterIsInstance<RemoteIncomingEvent.AlertAcknowledged>()
            .find { it.alertId == alertId }

        if (ackEvent == null) {
            delay(1000L)
            dev2Events = cloudRelay.pollInbox(dev2Code)
            ackEvent = dev2Events.filterIsInstance<RemoteIncomingEvent.AlertAcknowledged>()
                .find { it.alertId == alertId }
        }

        println("Device 2 inbox events count: ${dev2Events.size}")
        assertNotNull("Device 2 must receive the ACK in its inbox", ackEvent)
        println("================================================================")
        println("SUCCESS: END-TO-END 2-DEVICE EXCHANGE SIMULATION PASSED!")
        println("1. Device 1 created and published identity on cloud ($dev1Code)")
        println("2. Device 2 verified & paired with Device 1 ($dev1Code -> 'Ahmet (Cihaz 1)')")
        println("3. Device 2 triggered emergency alert to Device 1")
        println("4. Device 1 received emergency alert with message 'Lütfen acil bana ulaş!'")
        println("5. Device 1 sent ACK ('Okundu') to Device 2 ($dev2Code)")
        println("6. Device 2 received ACK confirmation")
        println("================================================================")
    }
}
