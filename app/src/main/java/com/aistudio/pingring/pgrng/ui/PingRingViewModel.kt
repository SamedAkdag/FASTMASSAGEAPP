package com.aistudio.pingring.pgrng.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.pingring.pgrng.data.model.AlertEntity
import com.aistudio.pingring.pgrng.data.model.AppLanguage
import com.aistudio.pingring.pgrng.data.model.PairedContactEntity
import com.aistudio.pingring.pgrng.data.model.UserEntity
import com.aistudio.pingring.pgrng.data.repository.PingRingRepository
import com.aistudio.pingring.pgrng.service.PingRingForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}

class PingRingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PingRingRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("ping_ring_settings", Context.MODE_PRIVATE)

    val isInitialLoading = MutableStateFlow(true)

    // Language State
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("selected_lang", "tr") ?: "tr")
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()
    val showLanguageDialog = MutableStateFlow(false)

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("selected_lang", language.code).apply()
    }

    val currentUser: StateFlow<UserEntity?> = repository.getCurrentUserFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            repository.getCurrentUserFlow().collect { user ->
                // Initial load completed once we get the first emission from Room
                isInitialLoading.value = false
                if (user != null && user.pairingCode.isNotBlank()) {
                    PingRingForegroundService.start(getApplication())
                }
            }
        }
    }

    val pairedContacts: StateFlow<List<PairedContactEntity>> = repository.getContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<AlertEntity>> = repository.getAllAlertsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeFullScreenAlert: StateFlow<AlertEntity?> = repository.activeFullScreenAlert

    val fastRetryMode: StateFlow<Boolean> = repository.fastRetryMode.asStateFlow()

    // Form inputs
    val messageText = MutableStateFlow("")
    val selectedContact = MutableStateFlow<PairedContactEntity?>(null)

    // Dialog & UI Visibility State
    val showMyCodeDialog = MutableStateFlow(false)
    val showAddByCodeDialog = MutableStateFlow(false)
    val showSimulateDialog = MutableStateFlow(false)
    val showHistorySheet = MutableStateFlow(false)

    // Code input for pairing
    val inputPairingCode = MutableStateFlow("")
    val inputContactName = MutableStateFlow("")
    val pairingError = MutableStateFlow<String?>(null)
    val isPairingInProgress = MutableStateFlow(false)

    // Status / Toast notification
    val toastMessage = MutableStateFlow<String?>(null)
    val isSendingAlert = MutableStateFlow(false)

    fun onMessageTextChanged(newText: String) {
        if (newText.length <= 100) {
            messageText.value = newText
        }
    }

    fun selectContact(contact: PairedContactEntity) {
        selectedContact.value = contact
    }

    fun registerOrLogin(phoneNumber: String, displayName: String) {
        viewModelScope.launch {
            if (phoneNumber.trim().isEmpty()) {
                toastMessage.value = "Lütfen bir telefon numarası girin."
                return@launch
            }
            val user = repository.registerOrLogin(phoneNumber, displayName)
            toastMessage.value = "Giriş yapıldı. Hoş geldiniz, ${user.displayName}!"
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            PingRingForegroundService.stop(getApplication())
            selectedContact.value = null
            toastMessage.value = "Çıkış yapıldı."
        }
    }

    fun generateNewPairingCode() {
        viewModelScope.launch {
            val code = repository.generateNewPairingCode()
            toastMessage.value = "Yeni eşleştirme kodunuz: $code"
        }
    }

    fun submitPairWithCode() {
        viewModelScope.launch {
            pairingError.value = null
            isPairingInProgress.value = true
            val rawCode = inputPairingCode.value.trim()
            val customName = inputContactName.value.trim().ifEmpty { null }

            val result = repository.pairWithCode(rawCode, customName)
            isPairingInProgress.value = false
            result.onSuccess { contact ->
                selectedContact.value = contact
                inputPairingCode.value = ""
                inputContactName.value = ""
                showAddByCodeDialog.value = false
                toastMessage.value = "${contact.name} (${contact.pairingCode}) başarıyla doğrulandı ve eşleştirildi!"
            }.onFailure { error ->
                pairingError.value = error.message ?: "Eşleştirme başarısız oldu."
            }
        }
    }

    fun deleteContact(contact: PairedContactEntity) {
        viewModelScope.launch {
            if (selectedContact.value?.id == contact.id) {
                selectedContact.value = null
            }
            repository.deleteContact(contact)
            toastMessage.value = "${contact.name} eşleştirmesi kaldırıldı."
        }
    }

    fun sendCriticalAlert() {
        val contact = selectedContact.value
        if (contact == null) {
            toastMessage.value = "Lütfen önce bir eşleşen alıcı seçin veya kod ile ekleyin."
            return
        }

        val text = messageText.value.trim()
        if (text.isEmpty()) {
            toastMessage.value = "Lütfen bir acil durum mesajı yazın (maks. 100 karakter)."
            return
        }

        viewModelScope.launch {
            isSendingAlert.value = true
            val result = repository.sendCriticalAlert(contact, text)
            isSendingAlert.value = false
            result.onSuccess { alert ->
                messageText.value = ""
                toastMessage.value = "🔴 KRİTİK UYARI GÖNDERİLDİ! Alıcıya iletiliyor (Tekrar: 1/5)..."
            }.onFailure { error ->
                toastMessage.value = error.message ?: "Uyarı gönderilemedi."
            }
        }
    }

    fun simulateIncomingAlert(customMessage: String? = null) {
        viewModelScope.launch {
            val contact = selectedContact.value
            val senderName = contact?.name ?: "Eşleşen Kişi (Acil)"
            val senderPhone = contact?.phoneNumber ?: "+90 555 123 4567"
            val msg = customMessage?.trim()?.ifEmpty { null }
                ?: messageText.value.trim().ifEmpty { null }
                ?: "ACİL DURUM! Lütfen acilen beni arayın, yardıma ihtiyacım var!"

            repository.simulateIncomingAlert(
                senderName = senderName,
                senderPhone = senderPhone,
                message = msg
            )
            showSimulateDialog.value = false
        }
    }

    fun openAlertInFullScreen(alert: AlertEntity) {
        viewModelScope.launch {
            repository.openAlertInFullScreen(alert)
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId)
            toastMessage.value = "Mesaj okundu olarak onaylandı, tekrarlar durduruldu."
        }
    }

    fun dismissAlertScreen(alertId: String) {
        repository.dismissAlertScreen(alertId)
        toastMessage.value = "Uyarı ekranı kapatıldı. 3 dakikalık periyodik tekrar aktif kalmaya devam ediyor."
    }

    fun cancelOutgoingAlert(alertId: String) {
        viewModelScope.launch {
            repository.cancelOutgoingAlert(alertId)
            toastMessage.value = "Uyarı iptal edildi."
        }
    }

    fun toggleFastRetryMode() {
        repository.fastRetryMode.value = !repository.fastRetryMode.value
        val mode = if (repository.fastRetryMode.value) "Hızlı Test Modu (10 Saniye)" else "Standart Mod (3 Dakika)"
        toastMessage.value = "Tekrar Modu: $mode"
    }

    fun clearToast() {
        toastMessage.value = null
    }
}
