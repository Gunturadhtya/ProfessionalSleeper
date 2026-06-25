package com.gntr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gntr.data.local.entity.SleepDebtEntity

@Dao
interface SleepDebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDebt(sleepDebtEntity: SleepDebtEntity)

    @Query("SELECT * FROM sleep_debts WHERE isSynced = 0")
    suspend fun getUnsyncedDebts(): List<SleepDebtEntity>

    @Query("UPDATE sleep_debts SET isSynced = 1 WHERE date IN (:dates)")
    suspend fun markAsSynced(dates: List<String>)
}