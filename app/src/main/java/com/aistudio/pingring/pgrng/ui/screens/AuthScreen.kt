package com.aistudio.pingring.pgrng.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.pingring.pgrng.R
import com.aistudio.pingring.pgrng.data.model.AppLanguage
import com.aistudio.pingring.pgrng.ui.components.SegmentedLanguageBar
import com.aistudio.pingring.pgrng.ui.theme.CrimsonBorder
import com.aistudio.pingring.pgrng.ui.theme.CrimsonBright
import com.aistudio.pingring.pgrng.ui.theme.CrimsonDark
import com.aistudio.pingring.pgrng.ui.theme.CrimsonLight
import com.aistudio.pingring.pgrng.ui.theme.CrimsonPrimary
import com.aistudio.pingring.pgrng.ui.theme.SlateBorder
import com.aistudio.pingring.pgrng.ui.theme.SlateCard
import com.aistudio.pingring.pgrng.ui.theme.SlateDark
import com.aistudio.pingring.pgrng.ui.theme.SlateLight
import com.aistudio.pingring.pgrng.ui.theme.StatusSafe
import com.aistudio.pingring.pgrng.ui.theme.TextMuted
import com.aistudio.pingring.pgrng.ui.theme.TextPrimary
import com.aistudio.pingring.pgrng.ui.theme.TextSecondary

/**
 * Modern, ultra-professional Entrance & Authentication Screen.
 * Features:
 * - Direct Turkish 🇹🇷, English 🇬🇧, Russian 🇷🇺 Language Selection at top
 * - High-craft emergency crimson & slate visual identity
 * - Clear setup inputs with instant demo profiles for rapid testing
 * - Security assurance pills
 */
@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLoginSuccess: (phoneNumber: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("+90 555 123 45 67") }
    var displayName by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF1F2),
                        SlateLight,
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. LANGUAGE SELECTOR ON ENTRANCE SCREEN
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedLanguageBar(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // 2. HERO BRANDING & BADGE
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CrimsonPrimary, CrimsonDark)
                        )
                    )
                    .border(2.dp, CrimsonBorder, RoundedCornerShape(22.dp))
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = CrimsonPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.auth_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )

            Text(
                text = stringResource(R.string.auth_subtitle),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // 3. MAIN SIGN IN / REGISTRATION CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.auth_card_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.auth_card_subtitle),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Phone Input Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            phoneError = null
                        },
                        label = { Text(stringResource(R.string.auth_phone_label)) },
                        placeholder = { Text(stringResource(R.string.auth_phone_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = CrimsonPrimary
                            )
                        },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            focusedLabelColor = CrimsonPrimary,
                            cursorColor = CrimsonPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_phone_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display Name Field
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.auth_name_label)) },
                        placeholder = { Text(stringResource(R.string.auth_name_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                val phoneText = phoneNumber.trim()
                                if (phoneText.isEmpty()) {
                                    phoneError = "Lütfen telefon numaranızı girin."
                                } else {
                                    onLoginSuccess(phoneText, displayName)
                                }
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            focusedLabelColor = CrimsonPrimary,
                            cursorColor = CrimsonPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login / Start Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val phoneText = phoneNumber.trim()
                            if (phoneText.isEmpty()) {
                                phoneError = "Lütfen telefon numaranızı girin."
                            } else {
                                onLoginSuccess(phoneText, displayName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_login_button")
                    ) {
                        Text(
                            text = stringResource(R.string.auth_btn_login),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. QUICK TEST PROFILES
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_quick_profiles),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    phoneNumber = "+90 555 111 2233"
                                    displayName = if (currentLanguage == AppLanguage.RUSSIAN) "Иван (Тест 1)"
                                    else if (currentLanguage == AppLanguage.ENGLISH) "Device 1 (John)"
                                    else "Ahmet (Cihaz 1)"
                                }
                                .testTag("demo_profile_1_btn"),
                            color = SlateLight
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_profile_device_a),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "+90 555 111 2233",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    phoneNumber = "+90 555 999 8877"
                                    displayName = if (currentLanguage == AppLanguage.RUSSIAN) "Мария (Тест 2)"
                                    else if (currentLanguage == AppLanguage.ENGLISH) "Device 2 (Mary)"
                                    else "Ayşe (Cihaz 2)"
                                }
                                .testTag("demo_profile_2_btn"),
                            color = SlateLight
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_profile_device_b),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "+90 555 999 8877",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. FEATURE ASSURANCE BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeatureBadge(
                    icon = Icons.Filled.Security,
                    text = stringResource(R.string.auth_feature_e2e),
                    modifier = Modifier.weight(1f)
                )
                FeatureBadge(
                    icon = Icons.Filled.Repeat,
                    text = stringResource(R.string.auth_feature_retry),
                    modifier = Modifier.weight(1f)
                )
                FeatureBadge(
                    icon = Icons.Filled.VolumeUp,
                    text = stringResource(R.string.auth_feature_siren),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp)),
        color = Color.White.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
