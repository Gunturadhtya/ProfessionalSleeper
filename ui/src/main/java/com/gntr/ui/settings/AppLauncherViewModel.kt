package com.gntr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.alarm.IAppDiscoveryService
import com.gntr.domain.model.AppInfo
import com.gntr.domain.repository.IPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLauncherViewModel @Inject constructor(
    private val prefsRepo: IPreferencesRepository,
    private val appDiscoveryService: IAppDiscoveryService
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

    fun initializeAppList() {
        if (_installedApps.value.isNotEmpty() || isFetchingApps) return

        viewModelScope.launch {
            loadNextAppPage()
        }
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
        viewModelScope.launch {
            prefsRepo.saveTargetAppPackage(packageName)
        }
    }

    fun saveAlarmRingtone(uriString: String) {
        viewModelScope.launch {
            prefsRepo.saveAlarmRingtoneUri(uriString)
        }
    }
}