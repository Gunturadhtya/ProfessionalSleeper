package com.gntr.data.di

import android.content.Context
import androidx.room.Room
import com.gntr.data.local.AppDatabase
import com.gntr.data.local.dao.CalendarEventDao
import com.gntr.data.local.dao.CalendarSourceDao
import com.gntr.data.local.dao.SleepDebtDao
import com.gntr.data.local.dao.SleepSessionDao
import com.gntr.data.local.datastore.AppPreferencesRepository
import com.gntr.data.local.datastore.dataStore
import com.gntr.data.local.migrations.MIGRATION_8_9
import com.gntr.domain.repository.IPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "sleeper_db")
            .addMigrations(MIGRATION_8_9)
            .build()
    }

    @Provides
    @Singleton
    fun provideSleepSessionDao(database: AppDatabase): SleepSessionDao = database.sleepSessionDao()

    @Provides
    @Singleton
    fun provideSleepDebtDao(database: AppDatabase): SleepDebtDao = database.sleepDebtDao()

    @Provides
    @Singleton
    fun provideCalendarEventDao(database: AppDatabase): CalendarEventDao = database.calendarEventDao()

    @Provides
    @Singleton
    fun provideCalendarSourceDao(database: AppDatabase): CalendarSourceDao = database.calendarSourceDao()

    @Provides
    @Singleton
    fun provideAppPreferencesRepository(@ApplicationContext context: Context): IPreferencesRepository {
        return AppPreferencesRepository(context.dataStore)
    }
}