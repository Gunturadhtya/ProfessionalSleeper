package com.gntr.professionalsleeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gntr.professionalsleeper.data.local.dao.CalendarEventDao
import com.gntr.professionalsleeper.data.local.dao.CalendarSourceDao
import com.gntr.professionalsleeper.data.local.dao.SleepDebtDao
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.entity.CalendarEventEntity
import com.gntr.professionalsleeper.data.local.entity.CalendarSourceEntity
import com.gntr.professionalsleeper.data.local.entity.SleepDebtEntity
import com.gntr.professionalsleeper.data.local.entity.SleepSessionEntity

@Database(entities = [
    SleepSessionEntity::class,
    SleepDebtEntity::class,
    CalendarEventEntity::class,
    CalendarSourceEntity::class
                     ], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun sleepDebtDao(): SleepDebtDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun calendarSourceDao(): CalendarSourceDao
}