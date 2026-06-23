package com.gntr.professionalsleeper.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gntr.professionalsleeper.presentation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val targetAppPackage by viewModel.targetAppPackage.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.initializeAppList(context)
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
            viewModel.loadNextAppPage(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Launch Trigger", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(
                text = "Pilih aplikasi yang akan otomatis terbuka saat alarm dimatikan:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName == targetAppPackage

                    ListItem(
                        headlineContent = { Text(app.appName, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.labelMedium) },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.saveTargetApp(app.packageName)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}