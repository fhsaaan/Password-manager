package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PasswordEntity
import com.example.security.PasswordGenerator
import com.example.security.PasswordStrength

@Composable
fun MainVaultScreen(
    viewModel: PasswordViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawPasswords by viewModel.rawPasswords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    // Dialog state controllers
    var showAddDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var entityToEdit by remember { mutableStateOf<PasswordEntity?>(null) }

    // Live display combining logic
    val filteredPasswords = remember(rawPasswords, searchQuery, selectedCategory) {
        rawPasswords.filter { entry ->
            val matchesSearch = entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.websiteUrl.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || entry.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    // Auto toast notifier
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_credential_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Security Vault Record"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Geometric Balance Header Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Vault",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Safe Green Dot Status
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34A853))
                        )
                        Text(
                            text = "AES-256 Encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Manual Lock button
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("manual_lock_vault_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Security Vault",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Geometric Initials Circle (JD)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEADDFF))
                            .border(1.dp, Color(0xFFD0BCFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    }
                }
            }

            // Search Filter Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // M3 Style Search capsule
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateFilters(it, selectedCategory) },
                    placeholder = { Text("Search passwords...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Query Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateFilters("", selectedCategory) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reset query input",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("vault_search_input"),
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful M3 rounded category chips
                val categories = listOf("All", "Work", "Personal", "Financial", "Social")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFFEADDFF)
                                    else Color(0xFFF3EDF7)
                                )
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = if (isSelected) Color.Transparent else Color(0xFF79747E),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.updateFilters(searchQuery, category) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_tab_$category"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF21005D),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF21005D) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                }
            }
            }

            // Dynamic Security Health Banner
            if (rawPasswords.isNotEmpty()) {
                val passwordStrengths = remember(rawPasswords) {
                    rawPasswords.map { entry ->
                        val rawPass = viewModel.getDecryptedPasswordDirect(entry)
                        com.example.security.PasswordGenerator.evaluateStrength(rawPass)
                    }
                }
                val weakCount = remember(passwordStrengths) {
                    passwordStrengths.count { it == com.example.security.PasswordStrength.WEAK }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFFEF7FF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (weakCount > 0) Icons.Default.Report else Icons.Default.Shield,
                            contentDescription = "Security health status icon",
                            tint = if (weakCount > 0) Color(0xFFB3261E) else Color(0xFF34A853),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (weakCount > 0) "$weakCount passwords vulnerable" else "Vault security health: Excellent",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = if (weakCount > 0) {
                                    "Leaking risks detected on some credentials. Update them now."
                                } else {
                                    "All stored passwords meet deep cryptographic complexity criteria."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Credentials list view
            if (filteredPasswords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = "Empty state icon padlock",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (rawPasswords.isEmpty()) "No Credentials Yet" else "No matching results",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (rawPasswords.isEmpty()) {
                                "Tap the plus button below to securely encrypt and add your first password record."
                            } else {
                                "Refine your search parameters or select 'All' categories to view records again."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("credentials_lazy_list"),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPasswords, key = { it.id }) { entry ->
                        CredentialListItem(
                            entry = entry,
                            onClick = {
                                viewModel.decryptSelectedEntity(entry)
                                showDetailDialog = true
                            },
                            onCopyUsername = {
                                copyToClipboard(context, viewModel.getDecryptedUsernameDirect(entry), "Username copied.")
                                viewModel.scheduleClipboardClear(context)
                            },
                            onCopyPassword = {
                                copyToClipboard(context, viewModel.getDecryptedPasswordDirect(entry), "Password copied secure.")
                                viewModel.scheduleClipboardClear(context)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog Overlay
    if (showDetailDialog) {
        CredentialDetailDialog(
            viewModel = viewModel,
            onClose = {
                viewModel.clearDecryptedCache()
                showDetailDialog = false
            },
            onEdit = { entity ->
                entityToEdit = entity
                showDetailDialog = false
                showAddDialog = true
            }
        )
    }

    // Add / Edit Dialog Overlay
    if (showAddDialog) {
        val editingRecord = entityToEdit
        AddEditCredentialDialog(
            viewModel = viewModel,
            editingEntity = editingRecord,
            onDismiss = {
                showAddDialog = false
                entityToEdit = null
            }
        )
    }
}

data class CategoryAesthetic(
    val bg: Color,
    val iconColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun getCategoryAesthetic(category: String): CategoryAesthetic {
    return when (category) {
        "Work" -> CategoryAesthetic(Color(0xFFF2F2F2), Color(0xFF1D1B20), Icons.Default.BusinessCenter)
        "Financial" -> CategoryAesthetic(Color(0xFFE8F0FE), Color(0xFF1967D2), Icons.Default.CreditCard)
        "Social" -> CategoryAesthetic(Color(0xFFFFF0E0), Color(0xFFE27100), Icons.Default.Forum)
        "Personal" -> CategoryAesthetic(Color(0xFFEADDFF), Color(0xFF21005D), Icons.Default.AccountCircle)
        else -> CategoryAesthetic(Color(0xFFF3EDF7), Color(0xFF49454F), Icons.Outlined.Lock)
    }
}

@Composable
fun CredentialListItem(
    entry: PasswordEntity,
    onClick: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit
) {
    val aesthetic = getCategoryAesthetic(entry.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("credential_item_card_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category rounded container indicator badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(aesthetic.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = aesthetic.icon,
                        contentDescription = "Category identifier logo",
                        tint = aesthetic.iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (entry.websiteUrl.isNotBlank()) entry.websiteUrl else entry.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Copy Fast-Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCopyUsername,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("quick_copy_username_btn_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = "Copy Username to primary clipboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onCopyPassword,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("quick_copy_password_btn_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy secure password directly",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Work" -> Icons.Default.BusinessCenter
        "Personal" -> Icons.Default.AccountCircle
        "Financial" -> Icons.Default.CreditCard
        "Social" -> Icons.Default.Forum
        else -> Icons.Outlined.Lock
    }
}

fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Credentials", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
