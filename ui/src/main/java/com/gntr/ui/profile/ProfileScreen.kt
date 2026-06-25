package com.gntr.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gntr.ui.R
import com.gntr.domain.model.CalendarSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLoginRequested: () -> Unit,
    onLogoutRequest: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val calendarSources by viewModel.calendarSources.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.syncCalendarList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.isLoggedIn) {
                LoggedInContent(
                    name = state.account?.displayName,
                    email = state.account?.email,
                    photoUrl = state.account?.photoUrl,
                    calendarSources = calendarSources,
                    onCalendarSource = { source, isEnabled ->
                        viewModel.toggleCalendarSource(source, isEnabled)
                    },
                    onLogout = {
                        viewModel.logout()
                        onLogoutRequest.invoke()
                    }
                )
            } else {
                GuestContent(onLoginClick = onLoginRequested)
            }
        }
    }
}

@Composable
private fun LoggedInContent(
    name: String?,
    email: String?,
    photoUrl: String?,
    onLogout: () -> Unit,
    calendarSources: List<CalendarSource>,
    onCalendarSource: (CalendarSource, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileAvatar(photoUrl = photoUrl, size = 96.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_signed_in),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!email.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_logout))
        }

        OutlinedButton(
            onClick = {
                throw RuntimeException("Test Crash")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.Analytics, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crash Out")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Shared Calendars",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)) {
            items(calendarSources, key = { it.id }) { source ->
                ListItem(
                    headlineContent = { Text(source.displayName) },
                    trailingContent = {
                        Switch(
                            checked = source.isEnabled,
                            onCheckedChange = { isChecked ->
                                onCalendarSource(source, isChecked)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GuestContent(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileAvatar(photoUrl = null, size = 96.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.profile_guest),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.profile_guest_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_login_google))
        }

        OutlinedButton(
            onClick = {
                throw RuntimeException("Test Crash")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.Analytics, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crash Out")
        }
    }
}

@Composable
private fun ProfileAvatar(photoUrl: String?, size: androidx.compose.ui.unit.Dp) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = stringResource(R.string.cd_profile_picture),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(R.string.cd_default_profile_picture),
                modifier = Modifier.size(size * 0.55f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}