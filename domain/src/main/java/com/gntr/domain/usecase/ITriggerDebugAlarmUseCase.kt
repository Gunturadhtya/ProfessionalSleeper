package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi

interface ITriggerDebugAlarmUseCase {
    suspend operator fun invoke()
}