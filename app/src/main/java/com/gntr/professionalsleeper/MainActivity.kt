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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.domain.auth.AuthAccount
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import com.gntr.professionalsleeper.framework.calendar.CalendarSyncWorker
import com.gntr.professionalsleeper.presentation.MainViewModel
import com.gntr.professionalsleeper.presentation.auth.AuthScreen
import com.gntr.professionalsleeper.presentation.auth.AuthViewModel
import com.gntr.professionalsleeper.presentation.setup.OnboardingScreen
import com.gntr.professionalsleeper.presentation.setup.SetupViewModel
import com.gntr.professionalsleeper.presentation.schedule.ScheduleScreen
import com.gntr.professionalsleeper.presentation.settings.SettingsScreen
import com.gntr.professionalsleeper.ui.theme.ProfessionalSleeperTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var prefsRepo: AppPreferencesRepository

    @Inject
    lateinit var authManager: IAuthManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())



        setContent {
            ProfessionalSleeperTheme {
                RequestEssentialPermissions(context = this)

                val authAccount by produceState<AuthAccount?>(initialValue = null) {
                    value = authManager.getSignedInAccount()
                }

                val isSetupComplete by prefsRepo.isSetupCompleteFlow.collectAsState(false)

                val navController = rememberNavController()

                val startRoute = when {
                    authAccount == null -> Route.Auth.route
                    !isSetupComplete -> Route.Setup.route
                    else -> Route.Schedule.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startRoute
                ) {
                    composable(Route.Auth.route) {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = { userEmail ->
                                if (userEmail != null) {
                                    enqueueCalendarSyncEngine(this@MainActivity, userEmail)
                                } else {
                                    Timber.i("User opted for offline execution. Bypassing Calendar Sync Engine.")
                                }

                                val destination = if (isSetupComplete) {
                                    Route.Schedule.route
                                } else {
                                    Route.Setup.route
                                }

                                navController.navigate(destination) {
                                    popUpTo(Route.Auth.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Route.Schedule.route) {
                        val viewModel: MainViewModel = hiltViewModel()
                        ScheduleScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = {
                                navController.navigate(Route.Settings.route)
                            },
                            onResetComplete = {
                                navController.navigate(Route.Setup.route) {
                                    popUpTo(Route.Schedule.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Route.Settings.route) {
                        val viewModel: MainViewModel = hiltViewModel()
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Route.Setup.route) {
                        val viewModel: SetupViewModel = hiltViewModel()
                        OnboardingScreen(
                            viewModel = viewModel,
                            onComplete = {
                                navController.navigate(Route.Schedule.route) {
                                    popUpTo(Route.Setup.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun enqueueCalendarSyncEngine(context: Context, accountEmail: String) {
    Timber.d("Configuring WorkManager constraints for background calendar synchronization.")

    val syncConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val periodicSyncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
        1, TimeUnit.HOURS,
        15, TimeUnit.MINUTES
    )
        .setConstraints(syncConstraints)
        .setInputData(workDataOf("account_email" to accountEmail))
        .build()

    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
        "UniqueProfessionalSleeperCalendarSync",
        ExistingPeriodicWorkPolicy.KEEP,
        periodicSyncRequest
    )
    Timber.i("Unique periodic synchronization request successfully committed to WorkManager queue.")
}

@Composable
fun RequestEssentialPermissions(context: Context) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

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