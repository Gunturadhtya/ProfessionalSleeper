package com.gntr.professionalsleeper.di

import com.gntr.domain.usecase.ISeedDatabaseUseCase
import com.gntr.domain.usecase.ITriggerDebugAlarmUseCase
import com.gntr.domain.usecase.SeedDatabaseUseCase
import com.gntr.domain.usecase.TriggerDebugAlarmUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DebugUseCaseModule {

    @Binds
    abstract fun bindTriggerDebugAlarm(
        impl: TriggerDebugAlarmUseCase
    ): ITriggerDebugAlarmUseCase

    @Binds
    abstract fun bindSeedDatabase(
        impl: SeedDatabaseUseCase
    ): ISeedDatabaseUseCase
}