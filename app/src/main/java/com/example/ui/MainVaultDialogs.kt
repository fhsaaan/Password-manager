package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PasswordEntity
import com.example.security.PasswordGenerator
import com.example.security.PasswordStrength

@Composable
fun CredentialDetailDialog(
    viewModel: PasswordViewModel,
    onClose: () -> Unit,
    onEdit: (PasswordEntity) -> Unit
) {
    val context = LocalContext.current
    val entity by viewModel.selectedEntity.collectAsState()
    val usernameClear by viewModel.decryptedUsername.collectAsState()
    val passwordClear by viewModel.decryptedPassword.collectAsState()
    val notesClear by viewModel.decryptedNotes.collectAsState()

    var hidePassword by remember { mutableStateOf(true) }
    var hideNotes by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val activeEntity = entity ?: return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(activeEntity.category),
                            contentDescription = "Details Icon category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeEntity.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeEntity.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("detail_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close security record viewer"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(20.dp))

                // Username Card
                DetailItemRow(
                    label = "Username / Email",
                    value = usernameClear ?: "• • • • • •",
                    tagPrefix = "detail_username",
                    context = context,
                    viewModel = viewModel,
                    isMaskable = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Card
                val strength = remember(passwordClear) { PasswordGenerator.evaluateStrength(passwordClear ?: "") }
                DetailItemRow(
                    label = "Password",
                    value = passwordClear ?: "• • • • • •",
                    tagPrefix = "detail_password",
                    context = context,
                    viewModel = viewModel,
                    isMaskable = true,
                    isMasked = hidePassword,
                    onMaskToggle = { hidePassword = !hidePassword },
                    strengthMeter = {
                        if (!hidePassword && !passwordClear.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            PasswordStrengthBadge(strength = strength)
                        }
                    }
                )

                if (activeEntity.websiteUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailItemRow(
                        label = "Website URL",
                        value = activeEntity.websiteUrl,
                        tagPrefix = "detail_url",
                        context = context,
                        viewModel = viewModel,
                        isMaskable = false,
                        trailingAction = {
                            IconButton(
                                onClick = {
                                    try {
                                        val url = if (!activeEntity.websiteUrl.startsWith("http://") && !activeEntity.websiteUrl.startsWith("https://")) {
                                            "https://${activeEntity.websiteUrl}"
                                        } else {
                                            activeEntity.websiteUrl
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        viewModel.showMessage("Could not open web link.")
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open Website link in external browser",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }

                val currentNotes = notesClear
                if (!currentNotes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailItemRow(
                        label = "Secure Notes",
                        value = currentNotes,
                        tagPrefix = "detail_notes",
                        context = context,
                        viewModel = viewModel,
                        isMaskable = true,
                        isMasked = hideNotes,
                        onMaskToggle = { hideNotes = !hideNotes }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Panel Control Actions (Edit & Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_delete_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete credentials")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }

                    Button(
                        onClick = { onEdit(activeEntity) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("detail_edit_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit credentials")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Record")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this record? This action cannot be undone.", fontSize = 15.sp) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCredentials(activeEntity)
                        showDeleteConfirm = false
                        onClose()
                    },
                    modifier = Modifier.testTag("dialog_confirm_delete")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun DetailItemRow(
    label: String,
    value: String,
    tagPrefix: String,
    context: Context,
    viewModel: PasswordViewModel,
    isMaskable: Boolean,
    isMasked: Boolean = false,
    onMaskToggle: (() -> Unit)? = null,
    strengthMeter: @Composable (() -> Unit)? = null,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isMaskable && isMasked) "••••••••••••" else value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("${tagPrefix}_text"),
                    maxLines = if (isMaskable && isMasked) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    trailingAction?.invoke()

                    if (isMaskable && onMaskToggle != null) {
                        IconButton(
                            onClick = onMaskToggle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (isMasked) "Reveal plaintext" else "Obscure text",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            copyToClipboard(context, value, "$label copied securely.")
                            viewModel.scheduleClipboardClear(context)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("${tagPrefix}_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy text directly",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            strengthMeter?.invoke()
        }
    }
}

@Composable
fun AddEditCredentialDialog(
    viewModel: PasswordViewModel,
    editingEntity: PasswordEntity? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(editingEntity?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(editingEntity?.category ?: "Personal") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(editingEntity?.websiteUrl ?: "") }
    var notes by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var showGenerator by remember { mutableStateOf(false) }

    // Crucial: Load and decrypt fields on launch for edit mode
    LaunchedEffect(editingEntity) {
        editingEntity?.let {
            username = viewModel.getDecryptedUsernameDirect(it)
            password = viewModel.getDecryptedPasswordDirect(it)
            val notesClear = viewModel.getDecryptedPasswordDirect(it) // Wait, notes decrypt!
            // Let's use direct decrypters or specific utility
            notes = viewModel.decryptedNotes.value ?: ""
            // To ensure we get the notes absolutely correct, we decrypt via ViewModel
            viewModel.decryptSelectedEntity(it)
            username = viewModel.decryptedUsername.value ?: ""
            password = viewModel.decryptedPassword.value ?: ""
            notes = viewModel.decryptedNotes.value ?: ""
        }
    }

    // Secondary listener to ensure loaded values bind
    val decryptedU by viewModel.decryptedUsername.collectAsState()
    val decryptedP by viewModel.decryptedPassword.collectAsState()
    val decryptedN by viewModel.decryptedNotes.collectAsState()

    LaunchedEffect(decryptedU, decryptedP, decryptedN) {
        if (editingEntity != null) {
            decryptedU?.let { username = it }
            decryptedP?.let { password = it }
            decryptedN?.let { notes = it }
        }
    }

    val strength = remember(password) { PasswordGenerator.evaluateStrength(password) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Headline Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingEntity == null) "Add Password" else "Edit Password",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close prompt dialog")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Box
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g. Google, Netflix)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_title"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector Segment
                    Column {
                        Text(
                            text = "Category Association",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val categoriesList = listOf("Personal", "Work", "Financial", "Social")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoriesList.forEach { cat ->
                                val selected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) Color(0xFFEADDFF)
                                            else Color(0xFFF3EDF7)
                                        )
                                        .border(
                                            width = if (selected) 0.dp else 1.dp,
                                            color = if (selected) Color.Transparent else Color(0xFF79747E),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(vertical = 10.dp)
                                        .testTag("dialog_category_$cat"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) Color(0xFF21005D) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Username Box
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username or Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_username"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Password Box
                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password value") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = "Toggle password plaintext view"
                                        )
                                    }
                                    IconButton(
                                        onClick = { showGenerator = true },
                                        modifier = Modifier.testTag("launch_generator_dialog_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = "Open customized random generator widget",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )

                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            PasswordStrengthBadge(strength = strength)
                        }
                    }

                    // Web URL Box
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Website URL (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_url"),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g. netflix.com") },
                        singleLine = true
                    )

                    // Notes Box (Multiline)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Secure Notes / Answer Prompts (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("input_notes"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action controls Save (Confirm) & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                viewModel.showMessage("Title is required.")
                                return@Button
                            }
                            if (password.isBlank()) {
                                viewModel.showMessage("Password is required.")
                                return@Button
                            }

                            if (editingEntity == null) {
                                viewModel.saveCredentials(
                                    title = title,
                                    usernameOpt = username,
                                    passwordRaw = password,
                                    urlOpt = url,
                                    notesOpt = notes,
                                    category = selectedCategory
                                )
                            } else {
                                viewModel.updateCredentials(
                                    id = editingEntity.id,
                                    title = title,
                                    usernameOpt = username,
                                    passwordRaw = password,
                                    urlOpt = url,
                                    notesOpt = notes,
                                    category = selectedCategory
                                )
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_credential_submit")
                    ) {
                        Text(if (editingEntity == null) "Save Credentials" else "Update Record")
                    }
                }
            }
        }
    }

    if (showGenerator) {
        CustomGeneratorDialog(
            onDismiss = { showGenerator = false },
            onPasswordChosen = { generatedStr ->
                password = generatedStr
                showGenerator = false
            }
        )
    }
}

@Composable
fun PasswordStrengthBadge(strength: PasswordStrength) {
    val barColor = when (strength) {
        PasswordStrength.EMPTY -> Color.Transparent
        PasswordStrength.WEAK -> Color(0xFFEF4444)      // SecurityDanger red
        PasswordStrength.MEDIUM -> Color(0xFFEAB308)    // SecurityWarning yellow
        PasswordStrength.STRONG -> Color(0xFF22C55E)    // SecurityGreen green
    }

    val fraction = when (strength) {
        PasswordStrength.EMPTY -> 0.0f
        PasswordStrength.WEAK -> 0.33f
        PasswordStrength.MEDIUM -> 0.66f
        PasswordStrength.STRONG -> 1.0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength Analysis:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = strength.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun CustomGeneratorDialog(
    onDismiss: () -> Unit,
    onPasswordChosen: (String) -> Unit
) {
    var length by remember { mutableStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }

    var generatedResult by remember { mutableStateOf("") }

    // Re-generate automatically when options alter
    LaunchedEffect(length, includeUppercase, includeLowercase, includeDigits, includeSymbols) {
        generatedResult = PasswordGenerator.generate(
            length = length.toInt(),
            includeUppercase = includeUppercase,
            includeLowercase = includeLowercase,
            includeDigits = includeDigits,
            includeSymbols = includeSymbols
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Password Generator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Rendered Result Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = generatedResult.ifEmpty { "Select at least one set" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (generatedResult.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("generator_result_text")
                        )
                    }
                }

                // Length slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Length Selector",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${length.toInt()} chars",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        valueRange = 8f..32f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generator_length_slider")
                    )
                }

                // Checkbox Options in grid-like rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckboxOptionRow(
                        label = "Uppercase (A-Z)",
                        checked = includeUppercase,
                        onCheckedChange = { includeUppercase = it },
                        tag = "uppercase_check"
                    )
                    CheckboxOptionRow(
                        label = "Lowercase (a-z)",
                        checked = includeLowercase,
                        onCheckedChange = { includeLowercase = it },
                        tag = "lowercase_check"
                    )
                    CheckboxOptionRow(
                        label = "Numbers (0-9)",
                        checked = includeDigits,
                        onCheckedChange = { includeDigits = it },
                        tag = "numbers_check"
                    )
                    CheckboxOptionRow(
                        label = "Special Symbols (%$#@)",
                        checked = includeSymbols,
                        onCheckedChange = { includeSymbols = it },
                        tag = "symbols_check"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        enabled = generatedResult.isNotEmpty(),
                        onClick = { onPasswordChosen(generatedResult) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("use_generated_submit"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Use Password")
                    }
                }
            }
        }
    }
}

@Composable
fun CheckboxOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
