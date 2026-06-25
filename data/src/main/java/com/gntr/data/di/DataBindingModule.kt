package com.gntr.data.di

import com.gntr.data.local.RoomTransactionRunner
import com.gntr.data.repository.CalendarEventRepositoryImpl
import com.gntr.data.repository.CalendarSourceRepositoryImpl
import com.gntr.data.repository.SleepDebtRepositoryImpl
import com.gntr.data.repository.SleepSessionRepositoryImpl
import com.gntr.data.security.SecureTokenManager
import com.gntr.domain.auth.ISecureTokenStore
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.domain.repository.ICalendarSourceRepository
import com.gntr.domain.repository.ISleepDebtRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.repository.ITransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindingModule {

    @Binds
    @Singleton
    abstract fun bindSleepSessionRepository(impl: SleepSessionRepositoryImpl): ISleepSessionRepository

    @Binds
    @Singleton
    abstract fun bindCalendarEventRepository(impl: CalendarEventRepositoryImpl): ICalendarEventRepository

    @Binds
    @Singleton
    abstract fun bindSleepDebtRepository(impl: SleepDebtRepositoryImpl): ISleepDebtRepository

    @Binds
    @Singleton
    abstract fun bindCalendarSourceRepository(impl: CalendarSourceRepositoryImpl): ICalendarSourceRepository

    @Binds
    @Singleton
    abstract fun bindSecureTokenStore(impl: SecureTokenManager): ISecureTokenStore

    @Binds
    @Singleton
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): ITransactionRunner
}