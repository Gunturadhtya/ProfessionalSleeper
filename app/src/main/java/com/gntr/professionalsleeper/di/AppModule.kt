package com.gntr.professionalsleeper.di

import android.content.Context
import androidx.room.Room
import com.gntr.professionalsleeper.data.local.AppDatabase
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.datastore.dataStore
import com.gntr.professionalsleeper.data.repository.SleepSessionRepositoryImpl
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import com.gntr.professionalsleeper.domain.calendar.ICalendarSyncService
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import com.gntr.professionalsleeper.framework.alarm.AlarmSchedulerImpl
import com.gntr.professionalsleeper.framework.auth.AuthManagerImpl
import com.gntr.professionalsleeper.framework.calendar.GoogleCalendarServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "sleeper_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSleepSessionDao(database: AppDatabase): SleepSessionDao {
        return database.sleepSessionDao()
    }

    @Provides
    @Singleton
    fun provideSleepSessionRepository(dao: SleepSessionDao): ISleepSessionRepository {
        return SleepSessionRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideAppPreferencesRepository(@ApplicationContext context: Context): AppPreferencesRepository {
        return AppPreferencesRepository(context.dataStore)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): IAlarmScheduler {
        return AlarmSchedulerImpl(context)
    }

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): IAuthManager {
        return AuthManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideCalendarSyncService(@ApplicationContext context: Context): ICalendarSyncService {
        return GoogleCalendarServiceImpl(context)
    }
}