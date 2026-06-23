package com.gntr.professionalsleeper.presentation.ringing

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.gntr.professionalsleeper.R
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.datastore.dataStore
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import com.gntr.professionalsleeper.framework.alarm.AlarmService
import com.gntr.professionalsleeper.framework.launcher.AppLauncherHelper
import com.gntr.professionalsleeper.ui.theme.ProfessionalSleeperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RingingActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)

        setContent {
            ProfessionalSleeperTheme {
                val viewModel: RingingViewModel = hiltViewModel()
                val currentSession by viewModel.currentSession.collectAsState()

                LaunchedEffect(sessionId) {
                    if (sessionId != -1) {
                        viewModel.loadSession(sessionId)
                    }
                }

                RingingScreen(
                    session = currentSession,
                    onDismissClick = { handleDismissAlarm() },
                    onSnoozeClick = { session ->
                        viewModel.triggerSnooze(session)
                        handleSnoozeAlarm()
                    }
                )
            }
        }
    }

    private fun handleDismissAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)

        val prefsRepo = AppPreferencesRepository(applicationContext.dataStore)

        lifecycleScope.launch {
            val targetPackage = prefsRepo.targetAppPackageFlow.first()
            AppLauncherHelper.launchApp(this@RingingActivity, targetPackage)
            finish()
        }
    }

    private fun handleSnoozeAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)
        finish()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RingingScreen(
    session: SleepSession?,
    onDismissClick: () -> Unit,
    onSnoozeClick: (SleepSession) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.ringing_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.ringing_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(64.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onDismissClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ringing_button_dismiss),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (session != null && session.snoozeCount < 2) {
                    OutlinedButton(
                        onClick = { onSnoozeClick(session) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ringing_button_snooze, 2 - session.snoozeCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}