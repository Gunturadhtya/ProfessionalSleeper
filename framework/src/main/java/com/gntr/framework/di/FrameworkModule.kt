package com.gntr.framework.di

import android.content.Context
import com.gntr.domain.alarm.IPreSessionNotificationScheduler
import com.gntr.domain.auth.ClientId
import com.gntr.domain.auth.IAuthManager
import com.gntr.domain.auth.ISecureTokenStore
import com.gntr.domain.calendar.ICalendarSyncService
import com.gntr.framework.alarm.PreSessionNotificationScheduler
import com.gntr.framework.auth.AuthManagerImpl
import com.gntr.framework.calendar.GoogleCalendarServiceImpl
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FrameworkModule {

    @Provides
    @Singleton
    fun provideHttpTransport(): HttpTransport {
        return NetHttpTransport()
    }

    @Provides
    @Singleton
    fun provideJsonFactory(): JsonFactory {
        return GsonFactory.getDefaultInstance()
    }

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context,
        secureTokenManager: ISecureTokenStore,
        transport: HttpTransport,
        jsonFactory: JsonFactory,
        @ClientId clientId: String
    ): IAuthManager {
        return AuthManagerImpl(
            context,
            secureTokenManager,
            transport,
            jsonFactory,
            clientId
        )
    }

    @Provides
    @Singleton
    fun provideCalendarSyncService(
        transport: HttpTransport,
        jsonFactory: JsonFactory
    ): ICalendarSyncService {
        return GoogleCalendarServiceImpl(transport, jsonFactory)
    }

    @Provides
    @Singleton
    fun providePreSessionNotificationScheduler(
        @ApplicationContext context: Context
    ): IPreSessionNotificationScheduler {
        val scheduler = PreSessionNotificationScheduler(context)
        scheduler.createNotificationChannel()
        return scheduler
    }
}