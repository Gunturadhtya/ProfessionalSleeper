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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gntr.domain.auth.IAuthManager
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.framework.calendar.CalendarSyncWorker
import com.gntr.ui.Route
import com.gntr.ui.auth.AuthScreen
import com.gntr.ui.auth.AuthViewModel
import com.gntr.ui.profile.ProfileScreen
import com.gntr.ui.quicknap.QuickNapScreen
import com.gntr.ui.schedule.ScheduleScreen
import com.gntr.ui.schedule.ScheduleViewModel
import com.gntr.ui.schedule.edit.EditSessionScreen
import com.gntr.ui.setup.SetupScreen
import com.gntr.ui.setup.SetupViewModel
import com.gntr.ui.settings.SettingsScreen
import com.gntr.ui.theme.ProfessionalSleeperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsRepo: IPreferencesRepository

    @Inject
    lateinit var authManager: IAuthManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContent {
            ProfessionalSleeperTheme {
                RequestEssentialPermissions(context = this)

                var startRoute by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val account = authManager.getSignedInAccount()
                    val isSetupComplete = prefsRepo.isSetupCompleteFlow.first()

                    startRoute = when {
                        account == null -> Route.Auth.route
                        !isSetupComplete -> Route.Setup.route
                        else -> Route.Schedule.route
                    }
                }

                if (startRoute == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@ProfessionalSleeperTheme
                }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = startRoute!!
                ) {
                    composable(Route.Auth.route) {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        val coroutineScope = rememberCoroutineScope()
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = { userEmail ->
                                if (userEmail != null) {
                                    enqueueCalendarSyncEngine(this@MainActivity, userEmail)
                                } else {
                                    Timber.i("User opted for offline execution. Bypassing Calendar Sync Engine.")
                                }
                                val cameFromProfile = navController.previousBackStackEntry
                                    ?.destination?.route == Route.Profile.route

                                coroutineScope.launch {
                                    val setupComplete = prefsRepo.isSetupCompleteFlow.first()
                                    val destination = when {
                                        cameFromProfile -> Route.Profile.route
                                        setupComplete -> Route.Schedule.route
                                        else -> Route.Setup.route
                                    }
                                    navController.navigate(destination) {
                                        popUpTo(Route.Auth.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(Route.Schedule.route) {
                        val viewModel: ScheduleViewModel = hiltViewModel()
                        ScheduleScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = {
                                navController.navigate(Route.Settings.route)
                            },
                            onNavigateToProfile = {
                                navController.navigate(Route.Profile.route)
                            },
                            onResetComplete = {
                                navController.navigate(Route.Setup.route) {
                                    popUpTo(Route.Schedule.route) { inclusive = true }
                                }
                            },
                            onNavigateToEditSession = { sessionId ->
                                navController.navigate(Route.EditSession.createRoute(sessionId))
                            },
                            onNavigateToQuickNap = {
                                navController.navigate(Route.QuickNap.route)
                            }
                        )
                    }

                    composable(
                        route = Route.EditSession.route,
                        arguments = listOf(
                            navArgument(Route.EditSession.ARG_SESSION_ID) { type = NavType.LongType }
                        )
                    ) {
                        EditSessionScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Route.Profile.route) {
                        ProfileScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onLoginRequested = {
                                navController.navigate(Route.Auth.route)
                            },
                            onLogoutRequest = {
                                navController.navigate(Route.Schedule.route)
                            }
                        )
                    }

                    composable(Route.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Route.Setup.route) {
                        val viewModel: SetupViewModel = hiltViewModel()
                        SetupScreen(
                            viewModel = viewModel,
                            onComplete = {
                                navController.navigate(Route.Schedule.route) {
                                    popUpTo(Route.Setup.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Route.QuickNap.route) {
                        QuickNapScreen()
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