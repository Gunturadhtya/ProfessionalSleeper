package com.gntr.ui.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.alarm.IAppDiscoveryService
import com.gntr.domain.model.AppInfo
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.domain.usecase.SeedDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLauncherViewModel @Inject constructor(
    private val prefsRepo: IPreferencesRepository,
    private val appDiscoveryService: IAppDiscoveryService,
    private val seedDatabaseUseCase: SeedDatabaseUseCase
) : ViewModel() {

    private var currentPageIndex = 0
    private val pageSize = 20
    private var isFetchingApps = false

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val targetAppPackage: StateFlow<String?> = prefsRepo.targetAppPackageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alarmRingtoneUri: StateFlow<String> = prefsRepo.alarmRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_installedApps, _searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.appName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initializeAppList() {
        if (_installedApps.value.isNotEmpty() || isFetchingApps) return
        viewModelScope.launch { loadNextAppPage() }
    }

    fun loadNextAppPage() {
        if (isFetchingApps) return
        isFetchingApps = true

        viewModelScope.launch {
            val newApps = appDiscoveryService.getInstalledApps(currentPageIndex, pageSize)
            if (newApps.isNotEmpty()) {
                _installedApps.update { current -> current + newApps }
                currentPageIndex += newApps.size
            }
            isFetchingApps = false
        }
    }

    fun saveTargetApp(packageName: String) {
        viewModelScope.launch { prefsRepo.saveTargetAppPackage(packageName) }
    }

    fun saveAlarmRingtone(uriString: String) {
        viewModelScope.launch { prefsRepo.saveAlarmRingtoneUri(uriString) }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) fetchAllApps()
    }

    private fun fetchAllApps() {
        if (isFetchingApps) return
        isFetchingApps = true

        viewModelScope.launch {
            try {
                while (true) {
                    val nextBatch = appDiscoveryService.getInstalledApps(currentPageIndex, pageSize)
                    if (nextBatch.isEmpty()) break
                    _installedApps.update { current -> current + nextBatch }
                    currentPageIndex += nextBatch.size
                }
            } finally {
                isFetchingApps = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun seedMockData() {
        viewModelScope.launch {
            seedDatabaseUseCase()
        }
    }
}