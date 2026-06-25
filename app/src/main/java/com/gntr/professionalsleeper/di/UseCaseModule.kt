package com.gntr.professionalsleeper.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.usecase.ISeedDatabaseUseCase
import com.gntr.domain.usecase.ITriggerDebugAlarmUseCase
import com.gntr.domain.usecase.SeedDatabaseUseCase
import com.gntr.domain.usecase.TriggerDebugAlarmUseCase
import com.gntr.domain.repository.ISleepDebtRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.repository.ITransactionRunner
import com.gntr.domain.service.SleepSessionManager
import com.gntr.professionalsleeper.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideTriggerDebugAlarmUseCase(
        sleepSessionManager: SleepSessionManager
    ): ITriggerDebugAlarmUseCase {
        return if (BuildConfig.DEBUG) {
            TriggerDebugAlarmUseCase(sleepSessionManager)
        } else {
            object : ITriggerDebugAlarmUseCase {
                @RequiresApi(Build.VERSION_CODES.O)
                override suspend fun invoke() = Unit
            }
        }
    }

    @Provides
    fun provideSeedDatabaseUseCase(
        sessionRepository: ISleepSessionRepository,
        sleepDebtRepository: ISleepDebtRepository,
        transactionRunner: ITransactionRunner
    ): ISeedDatabaseUseCase {
        return if (BuildConfig.DEBUG) {
            SeedDatabaseUseCase(sessionRepository, sleepDebtRepository, transactionRunner)
        } else {
            object : ISeedDatabaseUseCase {
                @RequiresApi(Build.VERSION_CODES.O)
                override suspend fun invoke() = Unit
            }
        }
    }
}