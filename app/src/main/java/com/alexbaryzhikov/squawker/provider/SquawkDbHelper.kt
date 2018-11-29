/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.alexbaryzhikov.squawker.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.alexbaryzhikov.squawker.provider.SquawkContract.MessagesEntry

class SquawkDbHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        val sqlCreateMessagesTable = """CREATE TABLE ${MessagesEntry.TABLE_NAME} (
                ${MessagesEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${MessagesEntry.COLUMN_AUTHOR} TEXT NOT NULL,
                ${MessagesEntry.COLUMN_AUTHOR_KEY} TEXT NOT NULL,
                ${MessagesEntry.COLUMN_MESSAGE} TEXT NOT NULL,
                ${MessagesEntry.COLUMN_DATE} INTEGER NOT NULL
            );"""
        db?.execSQL(sqlCreateMessagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${MessagesEntry.TABLE_NAME}")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "squawker.db"
        private const val DATABASE_VERSION = 1
    }
}