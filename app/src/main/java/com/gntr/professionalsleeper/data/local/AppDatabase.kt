package com.gntr.professionalsleeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.entity.SleepSession

@Database(entities = [SleepSession::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
}