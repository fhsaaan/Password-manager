package com.example.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper

@Composable
fun LockScreen(
    viewModel: PasswordViewModel,
    modifier: Modifier = Modifier
) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var inputPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN

    val message by viewModel.uiMessage.collectAsState()

    // Automatic biometric prompt load if biometric is set and enrolled
    LaunchedEffect(isPinSet) {
        if (isPinSet) {
            val activity = context as? FragmentActivity
            if (activity != null && BiometricHelper.isBiometricEnrollmentAvailable(activity)) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { _ ->
                        viewModel.unlockWithBiometrics()
                    },
                    onError = { _, errString ->
                        // Silent fail or low-priority feedback, allowing manual PIN
                        Loge("Biometrics state: $errString")
                    },
                    onFailed = {
                        viewModel.showMessage("Biometric matching failed.")
                    }
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Encrypted Security Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = if (!isPinSet) "Configure Vault" else "Vault Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                val subtitleText = when {
                    !isPinSet && setupStep == 1 -> "Create a 4-digit master PIN to sign details locally."
                    !isPinSet && setupStep == 2 -> "Re-enter your 4-digit PIN to confirm setup."
                    else -> "Authentication required to read credentials"
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Circular PIN Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeLength = inputPin.length
                    for (i in 0 until 4) {
                        val isActive = i < activeLength
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) Color(0xFF21005D)
                                    else Color(0xFFECE6F0)
                                )
                        )
                    }
                }
            }

            // Keypad numpad grid & biometrics
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Warning message banner
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.contains("Error", true) || it.contains("fail", true) || it.contains("Incorrect", true)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                NumpadGrid(
                    onDigitPress = { digit ->
                        if (inputPin.length < 4) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            inputPin += digit
                        }

                        // Handle PIN check when 4 digits reached
                        if (inputPin.length == 4) {
                            if (!isPinSet) {
                                if (setupStep == 1) {
                                    confirmPin = inputPin
                                    inputPin = ""
                                    setupStep = 2
                                    viewModel.showMessage("Please confirm your master PIN.")
                                } else {
                                    if (inputPin == confirmPin) {
                                        viewModel.setupVaultPin(inputPin)
                                    } else {
                                        viewModel.showMessage("PINs do not match. Restarting setup.")
                                        inputPin = ""
                                        setupStep = 1
                                    }
                                }
                            } else {
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                handler.postDelayed({
                                    val success = viewModel.unlockWithPin(inputPin)
                                    if (!success) {
                                        inputPin = ""
                                    }
                                }, 150)
                            }
                        }
                    },
                    onDeletePress = {
                        if (inputPin.isNotEmpty()) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            inputPin = inputPin.dropLast(1)
                        }
                    },
                    onBiometricPress = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricHelper.showBiometricPrompt(
                                activity = activity,
                                onSuccess = {
                                    viewModel.unlockWithBiometrics()
                                },
                                onError = { _, errString ->
                                    viewModel.showMessage("Biometric unlock failed: $errString")
                                },
                                onFailed = {
                                    viewModel.showMessage("Biometric match failed.")
                                }
                            )
                        }
                    },
                    isBiometricEnabled = isPinSet && BiometricHelper.isBiometricEnrollmentAvailable(context)
                )
            }
        }
    }
}

@Composable
fun NumpadGrid(
    onDigitPress: (String) -> Unit,
    onDeletePress: () -> Unit,
    onBiometricPress: () -> Unit,
    isBiometricEnabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (digit in row) {
                    NumpadButton(text = digit, onClick = { onDigitPress(digit) })
                }
            }
        }

        // Bottom numeric row including Biometrics and Backspace
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Left corner key
            if (isBiometricEnabled) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable { onBiometricPress() }
                        .testTag("biometric_shortcut_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Trigger Biometric Scanner",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(76.dp))
            }

            NumpadButton(text = "0", onClick = { onDigitPress("0") })

            // Right corner key (Backspace)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable { onDeletePress() }
                    .testTag("numpad_backspace_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Remove Last Digit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun NumpadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xFFF3EDF7))
            .clickable { onClick() }
            .testTag("numpad_key_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF1C1B1F),
            textAlign = TextAlign.Center
        )
    }
}

private fun Loge(msg: String) {
    android.util.Log.e("LockScreen", msg)
}
