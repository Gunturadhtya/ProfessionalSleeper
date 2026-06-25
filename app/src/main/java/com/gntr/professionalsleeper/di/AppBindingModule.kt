package com.gntr.professionalsleeper.di

import com.gntr.domain.alarm.IRingingIntentProvider
import com.gntr.domain.auth.ClientId
import com.gntr.professionalsleeper.BuildConfig
import com.gntr.professionalsleeper.framework.AndroidRingingIntentProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppBindingModule {

    @Binds
    @Singleton
    abstract fun bindRingingIntentProvider(impl: AndroidRingingIntentProvider): IRingingIntentProvider
}

@Module
@InstallIn(SingletonComponent::class)
internal object AppProvidersModule {

    @Provides
    @ClientId
    fun provideClientId(): String = BuildConfig.ANDROID_CLIENT_ID
}