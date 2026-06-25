package com.gntr.domain.repository

import kotlinx.coroutines.flow.Flow

interface IPreferencesRepository {
    val targetAppPackageFlow: Flow<String?>
    val isSetupCompleteFlow: Flow<Boolean>
    val everymanTypeFlow: Flow<String?>
    val wakeUpHourFlow: Flow<Int?>
    val wakeUpMinuteFlow: Flow<Int?>
    val alarmRingtoneUriFlow: Flow<String>

    suspend fun saveTargetAppPackage(packageName: String)
    suspend fun setSetupComplete(isComplete: Boolean)
    suspend fun saveCoreSleepTarget(minutes: Int)
    suspend fun saveEverymanType(type: String)
    suspend fun saveWakeUpTime(hour: Int, minute: Int)
    suspend fun saveAlarmRingtoneUri(uriString: String)
}