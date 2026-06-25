package com.gntr.domain.alarm

import com.gntr.domain.model.AppInfo

interface IAppDiscoveryService {
    suspend fun getInstalledApps(pageIndex: Int, pageSize: Int): List<AppInfo>
    suspend fun getTotalAppCount(): Int
}