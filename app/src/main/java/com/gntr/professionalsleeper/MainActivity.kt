package com.gntr.professionalsleeper

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.gntr.professionalsleeper.data.local.AppDatabase
import com.gntr.professionalsleeper.data.local.datastore.dataStore
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.repository.SleepSessionRepositoryImpl
import com.gntr.professionalsleeper.framework.alarm.AlarmSchedulerImpl
import com.gntr.professionalsleeper.presentation.MainViewModel
import com.gntr.professionalsleeper.presentation.schedule.ScheduleScreen
import com.gntr.professionalsleeper.presentation.settings.SettingsScreen
import com.gntr.professionalsleeper.ui.theme.ProfessionalSleeperTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "sleeper_db").build()
        val repository = SleepSessionRepositoryImpl(db.sleepSessionDao())
        val alarmScheduler = AlarmSchedulerImpl(applicationContext)
        val prefsRepo = AppPreferencesRepository(applicationContext.dataStore)

        Timber.plant(Timber.DebugTree())

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, alarmScheduler, prefsRepo) as T
            }
        }

        setContent {
            ProfessionalSleeperTheme {
                RequestEssentialPermissions(context = this)

                val viewModel: MainViewModel = viewModel(factory = factory)
                var currentScreen by remember { mutableStateOf(ScreenState.SCHEDULE) }

                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        ScreenState.SCHEDULE -> {
                            ScheduleScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = ScreenState.SETTINGS }
                            )
                        }
                        ScreenState.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = ScreenState.SCHEDULE }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestEssentialPermissions(context: Context) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {

    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}