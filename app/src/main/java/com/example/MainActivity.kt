package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.admin.AdminMainScreen
import com.example.ui.admin.AsmMainScreen
import com.example.ui.admin.MasterMainScreen
import com.example.ui.admin.TlMainScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.bdo.BdoMainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(database)

        // Initialize real-time cloud sync with offline persistence
        com.example.data.cloud.CloudSyncManager.initialize(applicationContext, database)

        lifecycleScope.launch {
            repository.seedDatabaseIfNeeded()
        }

        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository)
                )
                MainAppContent(mainViewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(mainViewModel: MainViewModel) {
    val currentUser by mainViewModel.currentUser.collectAsState()
    val loginError by mainViewModel.loginError.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val user = currentUser) {
            null -> {
                LoginScreen(
                    errorMessage = loginError,
                    onLogin = { username, password ->
                        mainViewModel.login(username, password)
                    }
                )
            }
            else -> {
                when (user.role) {
                    "MASTER" -> MasterMainScreen(viewModel = mainViewModel)
                    "ADMIN" -> AdminMainScreen(viewModel = mainViewModel)
                    "ASM" -> AsmMainScreen(viewModel = mainViewModel)
                    "TL" -> TlMainScreen(viewModel = mainViewModel)
                    else -> BdoMainScreen(viewModel = mainViewModel)
                }
            }
        }
    }
}


