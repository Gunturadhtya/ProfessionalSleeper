package com.gntr.domain.alarm

interface IAppLauncher {
    fun launchTargetApp(packageName: String?): Boolean
}