package com.gntr.professionalsleeper.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val TARGET_APP_PACKAGE = stringPreferencesKey("target_app_package")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val CORE_SLEEP_TARGET = intPreferencesKey("core_sleep_target")
    }

    val targetAppPackageFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TARGET_APP_PACKAGE]
    }

    val isSetupCompleteFlow: Flow<Boolean> = dataStore.data.map { it[IS_SETUP_COMPLETE] ?: false }

    suspend fun saveTargetAppPackage(packageName: String) {
        dataStore.edit { preferences ->
            preferences[TARGET_APP_PACKAGE] = packageName
        }
    }

    suspend fun setSetupComplete(isComplete: Boolean) {
        dataStore.edit { it[IS_SETUP_COMPLETE] = isComplete }
    }

    suspend fun saveCoreSleepTarget(minutes: Int) {
        dataStore.edit { it[CORE_SLEEP_TARGET] = minutes }
    }
}