package com.gntr.professionalsleeper.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.usecase.ISeedDatabaseUseCase
import com.gntr.domain.usecase.ITriggerDebugAlarmUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ReleaseUseCaseModule {

    @Provides
    fun provideTriggerDebugAlarmUseCase(): ITriggerDebugAlarmUseCase =
        object : ITriggerDebugAlarmUseCase {
            @RequiresApi(Build.VERSION_CODES.O)
            override suspend fun invoke() = Unit  // intentional no-op
        }

    @Provides
    fun provideSeedDatabaseUseCase(): ISeedDatabaseUseCase =
        object : ISeedDatabaseUseCase {
            @RequiresApi(Build.VERSION_CODES.O)
            override suspend fun invoke() = Unit  // intentional no-op
        }
}