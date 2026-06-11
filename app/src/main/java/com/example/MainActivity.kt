package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.PasswordRepository
import com.example.ui.LockScreen
import com.example.ui.MainVaultScreen
import com.example.ui.PasswordViewModel
import com.example.ui.PasswordViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Room Repository Wireframes Setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PasswordRepository(database.passwordDao())
        val factory = PasswordViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[PasswordViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val isUnlocked by viewModel.isUnlocked.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isUnlocked) {
                        MainVaultScreen(viewModel = viewModel)
                    } else {
                        LockScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
