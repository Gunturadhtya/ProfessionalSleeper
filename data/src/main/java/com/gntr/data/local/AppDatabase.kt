package com.gntr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gntr.data.local.dao.CalendarEventDao
import com.gntr.data.local.dao.CalendarSourceDao
import com.gntr.data.local.dao.SleepDebtDao
import com.gntr.data.local.dao.SleepSessionDao
import com.gntr.data.local.entity.CalendarEventEntity
import com.gntr.data.local.entity.CalendarSourceEntity
import com.gntr.data.local.entity.SleepDebtEntity
import com.gntr.data.local.entity.SleepSessionEntity

@Database(entities = [
    SleepSessionEntity::class,
    SleepDebtEntity::class,
    CalendarEventEntity::class,
    CalendarSourceEntity::class
                     ], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun sleepDebtDao(): SleepDebtDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun calendarSourceDao(): CalendarSourceDao
}