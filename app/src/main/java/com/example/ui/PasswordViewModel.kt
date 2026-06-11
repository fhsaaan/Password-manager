package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PasswordEntity
import com.example.data.PasswordRepository
import com.example.security.CryptographyManager
import com.example.security.EncryptedData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest

class PasswordViewModel(
    application: Application,
    private val repository: PasswordRepository
) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        "SecureVaultPrefs",
        Context.MODE_PRIVATE
    )

    private val cryptoManager = CryptographyManager()

    // Screen State
    private val _isPinSet = MutableStateFlow(false)
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Entire Database passwords list
    val rawPasswords: StateFlow<List<PasswordEntity>> = repository.allPasswords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Decrypted cache for detail views (stores raw decrypted values temporarily for the currently viewing dialog, not persisted in memory indefinitely)
    private val _decryptedPassword = MutableStateFlow<String?>(null)
    val decryptedPassword: StateFlow<String?> = _decryptedPassword.asStateFlow()

    private val _decryptedUsername = MutableStateFlow<String?>(null)
    val decryptedUsername: StateFlow<String?> = _decryptedUsername.asStateFlow()

    private val _decryptedNotes = MutableStateFlow<String?>(null)
    val decryptedNotes: StateFlow<String?> = _decryptedNotes.asStateFlow()

    // Current item being edited
    private val _selectedEntity = MutableStateFlow<PasswordEntity?>(null)
    val selectedEntity: StateFlow<PasswordEntity?> = _selectedEntity.asStateFlow()

    // Dialog & UI flows
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        // Check if master PIN is set
        val hashedSavedPin = sharedPrefs.getString("master_pin_hash", null)
        _isPinSet.value = !hashedSavedPin.isNullOrEmpty()
    }

    // Hash Helper (SHA-256)
    private fun hashPin(pin: String): String {
        return try {
            val bytes = pin.toByteArray(Charsets.UTF_8)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("PasswordViewModel", "PIN Hash Error", e)
            ""
        }
    }

    // Set Up PIN
    fun setupVaultPin(pin: String) {
        if (pin.length == 4) {
            val hash = hashPin(pin)
            sharedPrefs.edit().putString("master_pin_hash", hash).apply()
            _isPinSet.value = true
            _isUnlocked.value = true
            showMessage("Vault Master PIN configured successfully.")
        } else {
            showMessage("Error: PIN must be exactly 4 digits.")
        }
    }

    // Unlock PIN
    fun unlockWithPin(pin: String): Boolean {
        val savedHash = sharedPrefs.getString("master_pin_hash", "")
        val inputHash = hashPin(pin)
        if (savedHash == inputHash) {
            _isUnlocked.value = true
            return true
        }
        showMessage("Incorrect Master PIN.")
        return false
    }

    // Biometric Unlock Success
    fun unlockWithBiometrics() {
        _isUnlocked.value = true
        showMessage("Biometric unlock successful.")
    }

    // Lock Vault
    fun lockVault() {
        _isUnlocked.value = false
        clearDecryptedCache()
    }

    // CRUD Cryptography Actions
    fun saveCredentials(
        title: String,
        usernameOpt: String,
        passwordRaw: String,
        urlOpt: String,
        notesOpt: String,
        category: String
    ) {
        viewModelScope.launch {
            try {
                // Encrypt sensitive fields
                val encUser = cryptoManager.encrypt(usernameOpt)
                val encPass = cryptoManager.encrypt(passwordRaw)
                val encNotes = cryptoManager.encrypt(notesOpt)

                val entity = PasswordEntity(
                    title = title.ifBlank { "Untitled" },
                    category = category,
                    websiteUrl = urlOpt,
                    encryptedUsername = encUser.ciphertext,
                    usernameIv = encUser.iv,
                    encryptedPassword = encPass.ciphertext,
                    passwordIv = encPass.iv,
                    encryptedNotes = encNotes.ciphertext,
                    notesIv = encNotes.iv,
                    updatedAt = System.currentTimeMillis()
                )

                repository.insertPassword(entity)
                showMessage("Security record added successfully.")
            } catch (e: Exception) {
                showMessage("Encryption/Save failure: ${e.localizedMessage}")
            }
        }
    }

    fun updateCredentials(
        id: Int,
        title: String,
        usernameOpt: String,
        passwordRaw: String,
        urlOpt: String,
        notesOpt: String,
        category: String
    ) {
        viewModelScope.launch {
            try {
                val encUser = cryptoManager.encrypt(usernameOpt)
                val encPass = cryptoManager.encrypt(passwordRaw)
                val encNotes = cryptoManager.encrypt(notesOpt)

                val updatedEntity = PasswordEntity(
                    id = id,
                    title = title.ifBlank { "Untitled" },
                    category = category,
                    websiteUrl = urlOpt,
                    encryptedUsername = encUser.ciphertext,
                    usernameIv = encUser.iv,
                    encryptedPassword = encPass.ciphertext,
                    passwordIv = encPass.iv,
                    encryptedNotes = encNotes.ciphertext,
                    notesIv = encNotes.iv,
                    updatedAt = System.currentTimeMillis()
                )

                repository.updatePassword(updatedEntity)
                showMessage("Security record updated.")
            } catch (e: Exception) {
                showMessage("Crypto update failure: ${e.localizedMessage}")
            }
        }
    }

    fun deleteCredentials(entity: PasswordEntity) {
        viewModelScope.launch {
            repository.deletePassword(entity)
            showMessage("Credentials deleted.")
        }
    }

    // On-demand decrypters
    fun decryptSelectedEntity(entity: PasswordEntity) {
        _selectedEntity.value = entity
        _decryptedUsername.value = cryptoManager.decrypt(entity.encryptedUsername, entity.usernameIv)
        _decryptedPassword.value = cryptoManager.decrypt(entity.encryptedPassword, entity.passwordIv)
        _decryptedNotes.value = cryptoManager.decrypt(entity.encryptedNotes, entity.notesIv)
    }

    fun getDecryptedPasswordDirect(entity: PasswordEntity): String {
        return cryptoManager.decrypt(entity.encryptedPassword, entity.passwordIv)
    }

    fun getDecryptedUsernameDirect(entity: PasswordEntity): String {
        return cryptoManager.decrypt(entity.encryptedUsername, entity.usernameIv)
    }

    fun clearDecryptedCache() {
        _selectedEntity.value = null
        _decryptedUsername.value = null
        _decryptedPassword.value = null
        _decryptedNotes.value = null
    }

    fun updateFilters(query: String, category: String) {
        _searchQuery.value = query
        _selectedCategory.value = category
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    // Clipboard auto-clear logic in Coroutine
    fun scheduleClipboardClear(context: Context) {
        viewModelScope.launch {
            delay(30000) // 30 seconds
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                // Clear safely
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    val clip = android.content.ClipData.newPlainText("", "")
                    clipboard.setPrimaryClip(clip)
                }
                showMessage("Clipboard cleared securely.")
            } catch (e: Exception) {
                // Ignore clip failures
            }
        }
    }
}

class PasswordViewModelFactory(
    private val application: Application,
    private val repository: PasswordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PasswordViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
