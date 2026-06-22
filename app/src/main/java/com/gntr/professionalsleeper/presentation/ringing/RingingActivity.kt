package com.gntr.professionalsleeper.presentation.ringing

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.gntr.professionalsleeper.R
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.datastore.dataStore
import com.gntr.professionalsleeper.framework.alarm.AlarmService
import com.gntr.professionalsleeper.ui.theme.ProfessionalSleeperTheme
import com.gntr.professionalsleeper.framework.launcher.AppLauncherHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            ProfessionalSleeperTheme {
                RingingScreen(
                    onDismissClick = { handleDismissAlarm() }
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
}

@Composable
fun RingingScreen(onDismissClick: () -> Unit) {
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

            Button(
                onClick = onDismissClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.size(width = 200.dp, height = 56.dp)
            ) {
                Text(
                    text = stringResource(R.string.ringing_button_dismiss),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}