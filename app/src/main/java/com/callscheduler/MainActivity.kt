package com.callscheduler

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import com.callscheduler.data.model.ScheduledCall
import com.callscheduler.data.repository.CallSchedulerRepository
import com.callscheduler.ui.MainViewModel
import com.callscheduler.ui.MainViewModelFactory
import com.callscheduler.ui.screens.*
import com.callscheduler.ui.theme.AppColors
import com.callscheduler.ui.theme.CallSchedulerTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    object History : Screen()
    data class AddEdit(val call: ScheduledCall? = null) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: CallSchedulerRepository
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            Toast.makeText(this, "⚠️ Certaines permissions sont requises.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        repository = CallSchedulerRepository(this)

        requestPermissions()
        requestExactAlarmPermission()
        requestIgnoreBatteryOptimization()

        setContent {
            CallSchedulerTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
                }

                AppNavigation(viewModel)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            msg?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.SpaceBlack)) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
            },
            label = "screen-transition"
        ) { screen ->
            when (screen) {
                is Screen.Main -> MainScreen(
                    uiState = uiState,
                    onToggleCall = viewModel::toggleCall,
                    onDeleteCall = { call ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Supprimer « ${call.label} » ?",
                                actionLabel = "CONFIRMER"
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.deleteCall(call)
                        }
                    },
                    onAddCall = { currentScreen = Screen.AddEdit(null) },
                    onEditCall = { currentScreen = Screen.AddEdit(it) },
                    onDuplicateCall = viewModel::duplicateCall,
                    onFilterChanged = viewModel::onFilterChanged,
                    onSortChanged = viewModel::onSortChanged,
                    onSearchChanged = viewModel::onSearchQueryChanged,
                    onToggleStats = viewModel::toggleStatsPanel,
                )
                is Screen.AddEdit -> AddEditCallScreen(
                    existingCall = screen.call,
                    onSave = { call ->
                        if (screen.call == null) viewModel.saveCall(call)
                        else viewModel.updateCall(call)
                        currentScreen = Screen.Main
                    },
                    onCancel = { currentScreen = Screen.Main }
                )
                is Screen.History -> HistoryScreen(
                    history = uiState.history,
                    onBack = { currentScreen = Screen.Main }
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        if (currentScreen is Screen.Main) {
            FloatingActionButton(
                onClick = { currentScreen = Screen.History },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 28.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = "Historique")
            }
        }
    }
}
