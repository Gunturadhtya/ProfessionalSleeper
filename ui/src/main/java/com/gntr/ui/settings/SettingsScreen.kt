package com.gntr.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gntr.ui.BuildConfig
import com.gntr.ui.R
import androidx.core.net.toUri

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppLauncherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val targetAppPackage by viewModel.targetAppPackage.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val alarmRingtoneUri by viewModel.alarmRingtoneUri.collectAsState()
    val listState = rememberLazyListState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val ringtoneName = remember(alarmRingtoneUri) {
        if (alarmRingtoneUri.isNotEmpty()) {
            val ringtone = RingtoneManager.getRingtone(context, alarmRingtoneUri.toUri())
            ringtone?.getTitle(context) ?: "Unknown Ringtone"
        } else {
            "Default"
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                viewModel.saveAlarmRingtone(uri.toString())
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeAppList()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextAppPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search installed applications...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Text(
                text = "Alarm Preferences",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            ListItem(
                headlineContent = { Text("Alarm Sound") },
                supportingContent = { Text(ringtoneName) },
                modifier = Modifier.clickable {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        if (alarmRingtoneUri.isNotEmpty()) {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                alarmRingtoneUri.toUri())
                        }
                    }
                    ringtoneLauncher.launch(intent)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Text(
                text = "App Launch",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = stringResource(R.string.settings_instruction),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName == targetAppPackage

                    ListItem(
                        leadingContent = {
                            AppIcon(packageName = app.packageName)
                        },
                        headlineContent = { Text(app.appName, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.labelMedium) },
                        trailingContent = {
                            RadioButton(selected = isSelected, onClick = null)
                        },
                        modifier = Modifier.clickable {
                            viewModel.saveTargetApp(app.packageName)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            if (BuildConfig.DEBUG) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                ListItem(
                    headlineContent = { Text("Seed Mock Data") },
                    supportingContent = { Text("Generate 30 days of historical sleep analysis data") },
                    modifier = Modifier.clickable {
                        viewModel.seedMockData()
                        Toast.makeText(context, "Mock data seeded. Pull-to-refresh the schedule to recalculate analytics.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}