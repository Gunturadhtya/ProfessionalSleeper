package com.gntr.professionalsleeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gntr.professionalsleeper.data.local.dao.CalendarEventDao
import com.gntr.professionalsleeper.data.local.dao.SleepDebtDao
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.entity.CalendarEventEntity
import com.gntr.professionalsleeper.data.local.entity.SleepDebt
import com.gntr.professionalsleeper.data.local.entity.SleepSession

@Database(entities = [SleepSession::class, SleepDebt::class, CalendarEventEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun sleepDebtDao(): SleepDebtDao
    abstract fun calendarEventDao(): CalendarEventDao
}