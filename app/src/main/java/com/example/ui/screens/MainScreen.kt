package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertEntity
import com.example.data.model.AlertStatus
import com.example.data.model.PairedContactEntity
import com.example.ui.PingRingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PingRingViewModel,
    onOpenEmergencyAlert: (AlertEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val contacts by viewModel.pairedContacts.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val allAlerts by viewModel.allAlerts.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val fastRetryMode by viewModel.fastRetryMode.collectAsState()
    val isSendingAlert by viewModel.isSendingAlert.collectAsState()

    // Dialog state
    val showMyCodeDialog by viewModel.showMyCodeDialog.collectAsState()
    val showAddByCodeDialog by viewModel.showAddByCodeDialog.collectAsState()
    val inputPairingCode by viewModel.inputPairingCode.collectAsState()
    val inputContactName by viewModel.inputContactName.collectAsState()
    val pairingError by viewModel.pairingError.collectAsState()
    val isPairingInProgress by viewModel.isPairingInProgress.collectAsState()
    var showLogoutDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-select contact when available
    LaunchedEffect(contacts) {
        if (selectedContact == null && contacts.isNotEmpty()) {
            viewModel.selectContact(contacts.first())
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFD32F2F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ping-Ring",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E1E)
                            )
                            Text(
                                text = currentUser?.let { "${it.displayName} • Kod: ${it.pairingCode}" } ?: "Acil Durum Ağı",
                                fontSize = 12.sp,
                                color = Color(0xFF616161)
                            )
                        }
                    }
                },
                actions = {
                    FilterChip(
                        selected = fastRetryMode,
                        onClick = { viewModel.toggleFastRetryMode() },
                        label = {
                            Text(
                                text = if (fastRetryMode) "10sn Test" else "3dk Standart",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F),
                            selectedLeadingIconColor = Color(0xFFD32F2F)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Hesap / Çıkış",
                            tint = Color(0xFF616161),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FA)
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: PAIRING OPTIONS & OWN CODE BANNER
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Cihaz Eşleştirme & Kimlik",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = "Karşı cihazın eşleştirme kodunu girin. Kod girildiğinde iki cihaz da anında birbirini ekler.",
                            fontSize = 13.sp,
                            color = Color(0xFF757575),
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Quick info bar showing my code
                        val myCode = currentUser?.pairingCode ?: "..."
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BU CİHAZIN KODU",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF757575)
                                )
                                Text(
                                    text = myCode,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFD32F2F),
                                    letterSpacing = 2.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Ping-Ring Kodu", myCode)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.toastMessage.value = "Kendi kodunuz kopyalandı: $myCode"
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kopyala", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.pairingError.value = null
                                        viewModel.showAddByCodeDialog.value = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("add_by_code_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kod ile Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: PAIRED CONTACTS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Eşleşen Cihazlar (${contacts.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                            if (contacts.isNotEmpty()) {
                                Text(
                                    text = "Mesaj göndermek için dokunun",
                                    fontSize = 11.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (contacts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.PhoneIphone,
                                        contentDescription = null,
                                        tint = Color(0xFF9E9E9E),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Henüz eşleşen bir cihaz yok.\nYukarıdaki 'Kod ile Ekle' butonuna basarak diğer telefonun 6 haneli kodunu girin.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF616161),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                contacts.forEach { contact ->
                                    val isSelected = selectedContact?.id == contact.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) Color(0xFFFFEBEE) else Color(0xFFFAFAFA),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFD32F2F) else Color(0xFFE0E0E0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectContact(contact) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(
                                                        if (isSelected) Color(0xFFD32F2F) else Color(0xFF757575),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = contact.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF212121)
                                                )
                                                Text(
                                                    text = "Kod: ${contact.pairingCode} ${if (contact.phoneNumber.isNotBlank()) "• ${contact.phoneNumber}" else ""}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF616161)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSelected) {
                                                Text(
                                                    text = "Seçili",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD32F2F),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteContact(contact) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Eşleşmeyi Sil",
                                                    tint = Color(0xFF9E9E9E),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: SEND CRITICAL ALERT (TextInput max 100 chars + 🔴 KRİTİK UYARI GÖNDER)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Kritik Uyarı Gönderme",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )

                        Text(
                            text = if (selectedContact != null) {
                                "Seçili Alıcı: ${selectedContact?.name} (${selectedContact?.pairingCode})"
                            } else {
                                "Lütfen yukarıdan mesaj gönderilecek kişiyi seçin."
                            },
                            fontSize = 13.sp,
                            fontWeight = if (selectedContact != null) FontWeight.Medium else FontWeight.Normal,
                            color = if (selectedContact != null) Color(0xFF1976D2) else Color(0xFFC62828),
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // TextInput (Max 100 chars)
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { viewModel.onMessageTextChanged(it) },
                            label = { Text("Kritik Acil Durum Mesajı") },
                            placeholder = { Text("Örn: ACİL! Evde yangın/su baskını var, acilen ara!") },
                            supportingText = {
                                Text(
                                    text = "${messageText.length}/100 karakter",
                                    fontSize = 12.sp,
                                    color = if (messageText.length == 100) Color(0xFFD32F2F) else Color(0xFF757575),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("critical_message_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // SINGLE BIG BUTTON: '🔴 KRİTİK UYARI GÖNDER'
                        Button(
                            onClick = { viewModel.sendCriticalAlert() },
                            enabled = !isSendingAlert && selectedContact != null && messageText.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE0E0E0),
                                disabledContentColor = Color(0xFF9E9E9E)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("send_critical_alert_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isSendingAlert) "GÖNDERİLİYOR..." else "🔴 KRİTİK UYARI GÖNDER",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (selectedContact == null) {
                            Text(
                                text = "⚠️ Uyarı göndermek için önce yukarıdan bir kişi eşleştirin/seçin.",
                                fontSize = 12.sp,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 4: ACTIVE ALERTS & 3-MINUTE RETRY STATUS
            item {
                Text(
                    text = "Aktif Uyarılar ve Tekrarlar (${allAlerts.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (allAlerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Şu anda bekleyen aktif bir uyarı yok.",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            } else {
                items(allAlerts, key = { it.id }) { alert ->
                    val isPending = alert.status == AlertStatus.PENDING
                    val isAcknowledged = alert.status == AlertStatus.ACKNOWLEDGED
                    val timeFormatted = try {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert.createdAt))
                    } catch (e: Exception) { "" }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPending) Color.White else Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isPending) 1.5.dp else 1.dp,
                            color = if (alert.isIncoming) Color(0xFFD32F2F) else Color(0xFF1976D2)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                if (alert.isIncoming) Color(0xFFD32F2F) else Color(0xFF1976D2),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (alert.isIncoming) "🚨 GELEN KRİTİK UYARI" else "📤 GİDEN UYARI (ALICI BEKLENİYOR)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alert.isIncoming) Color(0xFFD32F2F) else Color(0xFF1976D2)
                                    )
                                }

                                Text(
                                    text = "Tekrar: ${alert.attemptCount}/${alert.maxAttempts} • $timeFormatted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPending) Color(0xFFD32F2F) else Color(0xFF757575)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = alert.message,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (alert.isIncoming) "Gönderen: ${alert.senderName} (${alert.senderPairingCode})"
                                else "Hedef Alıcı: ${alert.receiverName} (${alert.receiverPairingCode})",
                                fontSize = 12.sp,
                                color = Color(0xFF616161)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (alert.isIncoming) {
                                        "Durum: Alarm Çalıyor (3 dk tekrar aktif)"
                                    } else {
                                        "Durum: Karşı tarafın okuması bekleniyor"
                                    },
                                    fontSize = 11.sp,
                                    color = if (isPending) Color(0xFFD32F2F) else Color(0xFF757575),
                                    fontWeight = FontWeight.Medium
                                )

                                Row {
                                    if (alert.isIncoming && isPending) {
                                        TextButton(
                                            onClick = { onOpenEmergencyAlert(alert) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Tam Ekran Aç", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = { viewModel.acknowledgeAlert(alert.id) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Okudum (Durdur)", fontSize = 12.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                                        }
                                    } else if (!alert.isIncoming && isPending) {
                                        TextButton(
                                            onClick = { viewModel.cancelOutgoingAlert(alert.id) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("İptal Et", fontSize = 12.sp, color = Color(0xFF757575))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: LOCAL EMULATOR SIMULATOR (OPTIONAL FOR SINGLE DEVICE TESTING)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Tek Cihazda Deneme Aracı (Simülasyon)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF33691E)
                        )
                        Text(
                            text = "İkinci bir telefon olmadan alıcı tarafındaki tam ekran alarmı ve 'Okudum'/'Kapat' akışını test etmek için:",
                            fontSize = 11.sp,
                            color = Color(0xFF558B2F),
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        Button(
                            onClick = { viewModel.simulateIncomingAlert() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF558B2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("simulate_incoming_alert_button")
                        ) {
                            Text(
                                text = "📲 Gelen Alarmı Simüle Et (Test Ekranı)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // DIALOG A: 'Kendi Kodumu Göster'
    if (showMyCodeDialog) {
        val userCode = currentUser?.pairingCode ?: "A7K9-42"
        AlertDialog(
            onDismissRequest = { viewModel.showMyCodeDialog.value = false },
            title = {
                Text(
                    text = "Kendi Eşleştirme Kodunuz",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bu 6 haneli kodu diğer telefondaki 'Kod ile Ekle' alanına girin:",
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userCode,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFD32F2F),
                            letterSpacing = 4.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Ping-Ring Kodu", userCode)
                                clipboard.setPrimaryClip(clip)
                                viewModel.toastMessage.value = "Kod panoya kopyalandı: $userCode"
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kopyala", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.generateNewPairingCode() },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yeni Kod", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showMyCodeDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Tamam")
                }
            }
        )
    }

    // DIALOG B: 'Kod ile Ekle'
    if (showAddByCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddByCodeDialog.value = false },
            title = {
                Text(
                    text = "Kod ile Cihaz Ekle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Diğer cihazın ekranında görünen 6 haneli eşleştirme kodunu girin (Örnek: A7K9-42):",
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = inputPairingCode,
                        onValueChange = { viewModel.inputPairingCode.value = it.uppercase() },
                        label = { Text("6 Haneli Kod") },
                        placeholder = { Text("A7K9-42") },
                        singleLine = true,
                        isError = pairingError != null,
                        supportingText = pairingError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pair_code_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputContactName,
                        onValueChange = { viewModel.inputContactName.value = it },
                        label = { Text("Cihaz / Kişi Adı (İsteğe Bağlı)") },
                        placeholder = { Text("Örn: Annemin Telefonu, Eşim") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitPairWithCode() },
                    enabled = !isPairingInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("pair_submit_button")
                ) {
                    if (isPairingInProgress) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Doğrulanıyor...")
                    } else {
                        Text("Eşleştir")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showAddByCodeDialog.value = false },
                    enabled = !isPairingInProgress
                ) {
                    Text("İptal")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Profil / Çıkış",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    currentUser?.let { user ->
                        Text(
                            text = "Aktif Kullanıcı: ${user.displayName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = "Telefon: ${user.phoneNumber}",
                            fontSize = 13.sp,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "Eşleşme Kodu: ${user.pairingCode}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )
                    }
                    Text(
                        text = "Çıkış yaparak farklı bir telefon numarası veya isimle yeniden giriş yapabilirsiniz.",
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Çıkış Yap / Profil Değiştir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
