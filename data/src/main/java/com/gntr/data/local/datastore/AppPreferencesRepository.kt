package com.gntr.data.local.datastore

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gntr.domain.repository.IPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : IPreferencesRepository {

    companion object {
        val TARGET_APP_PACKAGE = stringPreferencesKey("target_app_package")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val CORE_SLEEP_TARGET = intPreferencesKey("core_sleep_target")
        val EVERYMAN_TYPE = stringPreferencesKey("everyman_type")
        val WAKE_UP_HOUR = intPreferencesKey("wake_up_hour")
        val WAKE_UP_MINUTE = intPreferencesKey("wake_up_minute")
        val ALARM_RINGTONE_URI = stringPreferencesKey("alarm_ringtone_uri")
    }

    override val targetAppPackageFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TARGET_APP_PACKAGE]
    }

    override val isSetupCompleteFlow: Flow<Boolean> = dataStore.data.map { it[IS_SETUP_COMPLETE] ?: false }

    override val everymanTypeFlow: Flow<String?> = dataStore.data.map { it[EVERYMAN_TYPE] }

    override val wakeUpHourFlow: Flow<Int?> = dataStore.data.map { it[WAKE_UP_HOUR] }

    override val wakeUpMinuteFlow: Flow<Int?> = dataStore.data.map { it[WAKE_UP_MINUTE] }

    override val alarmRingtoneUriFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[ALARM_RINGTONE_URI] ?: Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
    }

    override suspend fun saveTargetAppPackage(packageName: String) {
        dataStore.edit { preferences ->
            preferences[TARGET_APP_PACKAGE] = packageName
        }
    }

    override suspend fun setSetupComplete(isComplete: Boolean) {
        dataStore.edit { it[IS_SETUP_COMPLETE] = isComplete }
    }

    override suspend fun saveCoreSleepTarget(minutes: Int) {
        dataStore.edit { it[CORE_SLEEP_TARGET] = minutes }
    }

    override suspend fun saveEverymanType(type: String) {
        dataStore.edit { it[EVERYMAN_TYPE] = type }
    }

    override suspend fun saveWakeUpTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[WAKE_UP_HOUR] = hour
            it[WAKE_UP_MINUTE] = minute
        }
    }

    override suspend fun saveAlarmRingtoneUri(uriString: String) {
        dataStore.edit { preferences ->
            preferences[ALARM_RINGTONE_URI] = uriString
        }
    }
}