package com.aistudio.pingring.pgrng.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.pingring.pgrng.R
import com.aistudio.pingring.pgrng.data.model.AlertEntity
import com.aistudio.pingring.pgrng.data.model.AlertStatus
import com.aistudio.pingring.pgrng.data.model.AppLanguage
import com.aistudio.pingring.pgrng.data.model.PairedContactEntity
import com.aistudio.pingring.pgrng.ui.PingRingViewModel
import com.aistudio.pingring.pgrng.ui.components.LanguageSelectionDialog
import com.aistudio.pingring.pgrng.ui.theme.CrimsonBorder
import com.aistudio.pingring.pgrng.ui.theme.CrimsonBright
import com.aistudio.pingring.pgrng.ui.theme.CrimsonDark
import com.aistudio.pingring.pgrng.ui.theme.CrimsonLight
import com.aistudio.pingring.pgrng.ui.theme.CrimsonPrimary
import com.aistudio.pingring.pgrng.ui.theme.SlateBorder
import com.aistudio.pingring.pgrng.ui.theme.SlateBorderDark
import com.aistudio.pingring.pgrng.ui.theme.SlateCard
import com.aistudio.pingring.pgrng.ui.theme.SlateDark
import com.aistudio.pingring.pgrng.ui.theme.SlateLight
import com.aistudio.pingring.pgrng.ui.theme.StatusInfo
import com.aistudio.pingring.pgrng.ui.theme.StatusInfoBorder
import com.aistudio.pingring.pgrng.ui.theme.StatusInfoLight
import com.aistudio.pingring.pgrng.ui.theme.StatusSafe
import com.aistudio.pingring.pgrng.ui.theme.StatusSafeBorder
import com.aistudio.pingring.pgrng.ui.theme.StatusSafeLight
import com.aistudio.pingring.pgrng.ui.theme.StatusWarning
import com.aistudio.pingring.pgrng.ui.theme.TextMuted
import com.aistudio.pingring.pgrng.ui.theme.TextPrimary
import com.aistudio.pingring.pgrng.ui.theme.TextSecondary
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
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    // Dialog state
    val showMyCodeDialog by viewModel.showMyCodeDialog.collectAsState()
    val showAddByCodeDialog by viewModel.showAddByCodeDialog.collectAsState()
    val showLanguageDialog by viewModel.showLanguageDialog.collectAsState()
    val inputPairingCode by viewModel.inputPairingCode.collectAsState()
    val inputContactName by viewModel.inputContactName.collectAsState()
    val pairingError by viewModel.pairingError.collectAsState()
    val isPairingInProgress by viewModel.isPairingInProgress.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CrimsonPrimary, CrimsonDark)
                                    )
                                ),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ping-Ring",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(StatusSafe, CircleShape)
                                )
                            }
                            Text(
                                text = currentUser?.let { "${it.displayName} • ${it.pairingCode}" }
                                    ?: stringResource(R.string.header_network_active),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.showLanguageDialog.value = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("topbar_language_btn"),
                        color = Color.White
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentLanguage.flag, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLanguage.code.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Fast retry toggle chip
                    FilterChip(
                        selected = fastRetryMode,
                        onClick = { viewModel.toggleFastRetryMode() },
                        label = {
                            Text(
                                text = if (fastRetryMode) stringResource(R.string.fast_retry_label_short, 10)
                                else stringResource(R.string.standard_retry_label_short, 3),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonLight,
                            selectedLabelColor = CrimsonPrimary,
                            selectedLeadingIconColor = CrimsonPrimary,
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Profile / Logout Icon
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(R.string.account_logout),
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateLight
                )
            )
        },
        containerColor = SlateLight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: DEVICE PAIRING & IDENTITY CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
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
                                text = stringResource(R.string.header_pairing_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusSafeLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusSafeBorder)
                            ) {
                                Text(
                                    text = stringResource(R.string.contact_status_active),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSafe,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.header_pairing_subtitle),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // 6-digit Device Code Display Box
                        val myCode = currentUser?.pairingCode ?: "..."
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CrimsonBorder, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = CrimsonLight
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.my_device_code_label),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonDark,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = myCode,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = CrimsonPrimary,
                                        letterSpacing = 3.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Copy button
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Ping-Ring Code", myCode)
                                            clipboard.setPrimaryClip(clip)
                                            viewModel.toastMessage.value = context.getString(R.string.toast_code_copied, myCode)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = CrimsonPrimary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonBorder),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.btn_copy), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Add by Code Button
                                    Button(
                                        onClick = {
                                            viewModel.pairingError.value = null
                                            viewModel.showAddByCodeDialog.value = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusInfo),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("add_by_code_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.btn_add_by_code),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: PAIRED CONTACTS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
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
                                text = stringResource(R.string.paired_devices_title, contacts.size),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (contacts.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.paired_devices_subtitle),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (contacts.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                                color = SlateLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhoneIphone,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.no_paired_devices),
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                contacts.forEach { contact ->
                                    val isSelected = selectedContact?.id == contact.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) CrimsonPrimary else SlateBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.selectContact(contact) }
                                            .testTag("contact_item_${contact.pairingCode}"),
                                        color = if (isSelected) CrimsonLight else Color.White
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
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
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) CrimsonPrimary else SlateDark
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
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = contact.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = CrimsonPrimary
                                                            ) {
                                                                Text(
                                                                    text = stringResource(R.string.contact_selected_badge),
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "Kod: ${contact.pairingCode} ${if (contact.phoneNumber.isNotBlank()) "• ${contact.phoneNumber}" else ""}",
                                                        fontSize = 12.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteContact(contact) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.btn_delete_pair),
                                                    tint = TextMuted,
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

            // SECTION 3: SEND CRITICAL ALERT PANEL
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.send_alert_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = if (selectedContact != null) {
                                stringResource(
                                    R.string.send_alert_recipient_selected,
                                    selectedContact?.name ?: "",
                                    selectedContact?.pairingCode ?: ""
                                )
                            } else {
                                stringResource(R.string.send_alert_select_prompt)
                            },
                            fontSize = 12.sp,
                            fontWeight = if (selectedContact != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedContact != null) StatusInfo else CrimsonPrimary,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Quick emergency message template chips
                        Text(
                            text = stringResource(R.string.quick_templates_title),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val template1 = stringResource(R.string.quick_template_help)
                        val template2 = stringResource(R.string.quick_template_call)
                        val template3 = stringResource(R.string.quick_template_location)
                        val template4 = stringResource(R.string.quick_template_safe)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickTemplateChip(
                                text = template1,
                                onClick = { viewModel.onMessageTextChanged(template1) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickTemplateChip(
                                text = template2,
                                onClick = { viewModel.onMessageTextChanged(template2) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickTemplateChip(
                                text = template3,
                                onClick = { viewModel.onMessageTextChanged(template3) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickTemplateChip(
                                text = template4,
                                onClick = { viewModel.onMessageTextChanged(template4) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Text input
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { viewModel.onMessageTextChanged(it) },
                            label = { Text(stringResource(R.string.alert_message_label)) },
                            placeholder = { Text(stringResource(R.string.alert_message_hint)) },
                            supportingText = {
                                Text(
                                    text = stringResource(R.string.char_count_format, messageText.length),
                                    fontSize = 11.sp,
                                    color = if (messageText.length == 100) CrimsonPrimary else TextSecondary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonPrimary,
                                focusedLabelColor = CrimsonPrimary,
                                cursorColor = CrimsonPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("critical_message_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Command Emergency Send Button
                        Button(
                            onClick = { viewModel.sendCriticalAlert() },
                            enabled = !isSendingAlert && selectedContact != null && messageText.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = Color.White,
                                disabledContainerColor = SlateBorder,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("send_critical_alert_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSendingAlert) stringResource(R.string.sending_indicator)
                                    else stringResource(R.string.btn_send_alert),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (selectedContact == null) {
                            Text(
                                text = stringResource(R.string.send_alert_warning_helper),
                                fontSize = 11.sp,
                                color = CrimsonPrimary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 4: ACTIVE ALERTS & REPEAT LOGS
            item {
                Text(
                    text = stringResource(R.string.active_alerts_title, allAlerts.size),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (allAlerts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                        color = SlateCard,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_active_alerts),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
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
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isPending) 1.5.dp else 1.dp,
                            color = if (alert.isIncoming) CrimsonPrimary else StatusInfo
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
                                                if (alert.isIncoming) CrimsonPrimary else StatusInfo,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (alert.isIncoming) stringResource(R.string.alert_incoming_label)
                                        else stringResource(R.string.alert_outgoing_label),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alert.isIncoming) CrimsonPrimary else StatusInfo
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.retry_count_label, alert.attemptCount, alert.maxAttempts, timeFormatted),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPending) CrimsonPrimary else TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = alert.message,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (alert.isIncoming) stringResource(R.string.alert_sender_label, alert.senderName, alert.senderPairingCode)
                                else stringResource(R.string.alert_target_label, alert.receiverName, alert.receiverPairingCode),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAcknowledged) stringResource(R.string.alert_status_acknowledged)
                                    else if (alert.isIncoming) stringResource(R.string.alert_status_alarm_ringing)
                                    else stringResource(R.string.alert_status_waiting_read),
                                    fontSize = 11.sp,
                                    color = if (isAcknowledged) StatusSafe else if (isPending) CrimsonPrimary else TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )

                                Row {
                                    if (alert.isIncoming && isPending) {
                                        TextButton(
                                            onClick = { onOpenEmergencyAlert(alert) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_open_fullscreen),
                                                fontSize = 11.sp,
                                                color = CrimsonPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.acknowledgeAlert(alert.id) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_read_stop),
                                                fontSize = 11.sp,
                                                color = StatusSafe,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (!alert.isIncoming && isPending) {
                                        TextButton(
                                            onClick = { viewModel.cancelOutgoingAlert(alert.id) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_cancel),
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: LOCAL SINGLE-DEVICE SIMULATION TOOL
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCEDC8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.test_mode_info_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF33691E)
                        )
                        Text(
                            text = stringResource(R.string.test_mode_info_text),
                            fontSize = 11.sp,
                            color = Color(0xFF558B2F),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.simulateIncomingAlert() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF558B2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("simulate_incoming_alert_button")
                        ) {
                            Text(
                                text = stringResource(R.string.simulation_btn_text),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 1. LANGUAGE SELECTION DIALOG
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { newLang ->
                viewModel.setLanguage(newLang)
            },
            onDismiss = { viewModel.showLanguageDialog.value = false }
        )
    }

    // 2. DIALOG: ADD DEVICE BY CODE
    if (showAddByCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddByCodeDialog.value = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_add_code_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.dialog_add_code_hint),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = inputPairingCode,
                        onValueChange = { viewModel.inputPairingCode.value = it.uppercase() },
                        label = { Text("6 Haneli Kod") },
                        placeholder = { Text("A7K9-42") },
                        singleLine = true,
                        isError = pairingError != null,
                        supportingText = pairingError?.let { errKey ->
                            {
                                val resId = context.resources.getIdentifier(errKey, "string", context.packageName)
                                val displayMsg = if (resId != 0 && errKey.startsWith("pairing_error_")) {
                                    // For user-not-found error, we need to format with the code
                                    if (errKey == "pairing_error_user_not_found") {
                                        context.getString(resId, inputPairingCode)
                                    } else {
                                        context.getString(resId)
                                    }
                                } else {
                                    context.getString(R.string.pairing_error, errKey)
                                }
                                Text(displayMsg, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pair_code_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputContactName,
                        onValueChange = { viewModel.inputContactName.value = it },
                        label = { Text(stringResource(R.string.dialog_contact_name_hint)) },
                        placeholder = { Text(stringResource(R.string.dialog_contact_name_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitPairWithCode() },
                    enabled = !isPairingInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusInfo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("pair_submit_button")
                ) {
                    if (isPairingInProgress) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.pairing_verifying))
                    } else {
                        Text(stringResource(R.string.btn_pair))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showAddByCodeDialog.value = false },
                    enabled = !isPairingInProgress
                ) {
                    Text(stringResource(R.string.btn_cancel_pair))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. DIALOG: PROFILE / LOGOUT
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.logout_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    currentUser?.let { user ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = user.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = user.phoneNumber,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "Kod: ${user.pairingCode}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.logout_confirm_msg),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.btn_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun QuickTemplateChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = SlateLight
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
