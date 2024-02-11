/*
 * Copyright (c) 2021, Karthikeyan Singaravelan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xtreak.notificationdictionary

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File


@Database(entities = [Word::class, History::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun historyDao(): HistoryDao


    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var DATABASE_NAME: String? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("ndict:", "Migration started")
                database.execSQL("CREATE TABLE IF NOT EXISTS history(id INTEGER primary key, word TEXT, is_favourite INTEGER default 0 NOT NULL, last_accessed_at INTEGER)")
                Log.d("ndict:", "Migration completed")
            }
        }

        fun getDatabase(context: Context): AppDatabase {

            val sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
            )
            val default_database_key = context.getString(R.string.default_database)
            val database_name =
                sharedPref.getString(default_database_key, "dictionary.db") as String

            // On first instance set the shared preference as DATABASE_NAME.
            if (DATABASE_NAME == null) {
                DATABASE_NAME = database_name
            }

            // On subsequent calls if the share preference has changed then we are holding to older
            // connection. Close it properly and set it as null to return new database. This makes
            // sure the instance is singleton as long as the preference is not changed.
            if (INSTANCE != null && DATABASE_NAME != database_name) {
                DATABASE_NAME = database_name
                INSTANCE!!.close()
                INSTANCE = null
            }

            val package_data_directory =
                Environment.getDataDirectory().absolutePath + "/data/" + context.packageName
            val db_path = File("$package_data_directory/databases/$database_name")


            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    database_name
                ).createFromFile(db_path).addMigrations(MIGRATION_1_2).build()
                // return instance
                INSTANCE = instance
                instance
            }
        }
    }

}