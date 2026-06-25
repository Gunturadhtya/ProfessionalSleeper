package com.gntr.framework.di

import com.gntr.domain.alarm.IAlarmController
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.alarm.IAppDiscoveryService
import com.gntr.domain.alarm.IAppLauncher
import com.gntr.domain.alarm.IAudioPlayer
import com.gntr.domain.calendar.ISyncManager
import com.gntr.framework.alarm.AlarmSchedulerImpl
import com.gntr.framework.alarm.AndroidAlarmController
import com.gntr.framework.alarm.AudioPlayerImpl
import com.gntr.framework.launcher.AndroidAppDiscoveryService
import com.gntr.framework.launcher.AndroidAppLauncher
import com.gntr.framework.sync.SyncManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FrameworkBindingModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AlarmSchedulerImpl): IAlarmScheduler

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: AudioPlayerImpl): IAudioPlayer

    @Binds
    @Singleton
    abstract fun bindSyncManager(impl: SyncManagerImpl): ISyncManager

    @Binds
    @Singleton
    abstract fun bindAlarmController(impl: AndroidAlarmController): IAlarmController

    @Binds
    @Singleton
    abstract fun bindAppLauncher(impl: AndroidAppLauncher): IAppLauncher

    @Binds
    @Singleton
    abstract fun bindAppDiscoveryService(impl: AndroidAppDiscoveryService): IAppDiscoveryService
}