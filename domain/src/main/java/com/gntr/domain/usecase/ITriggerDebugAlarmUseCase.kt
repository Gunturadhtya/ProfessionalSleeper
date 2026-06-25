package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi

interface ITriggerDebugAlarmUseCase {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke()
}