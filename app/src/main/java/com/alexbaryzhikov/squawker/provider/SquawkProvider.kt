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

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.net.Uri
import com.alexbaryzhikov.squawker.provider.SquawkContract.MessagesEntry

class SquawkProvider : ContentProvider() {
    private lateinit var squawkDbHelper: SquawkDbHelper

    override fun onCreate(): Boolean {
        squawkDbHelper = SquawkDbHelper(context)
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != MESSAGES) {
            throw UnsupportedOperationException("Unknown uri: $uri")
        }

        val db = squawkDbHelper.writableDatabase
        val id = db.insert(MessagesEntry.TABLE_NAME, null, values)
        if (id <= 0) {
            throw SQLException("Failed to insert row into $uri")
        }

        context?.contentResolver?.notifyChange(uri, null)
        SquawkContract.onUpdate?.invoke()
        return ContentUris.withAppendedId(MessagesEntry.CONTENT_URI, id)
    }

    @SuppressLint("Recycle")
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != MESSAGES) {
            throw UnsupportedOperationException("Unknown uri: $uri")
        }

        val db = squawkDbHelper.writableDatabase
        return db.query(
            MessagesEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        ).apply { setNotificationUri(context?.contentResolver, uri) }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        if (uriMatcher.match(uri) != MESSAGE_WITH_ID) {
            throw UnsupportedOperationException("Unknown uri: $uri")
        }

        val db = squawkDbHelper.writableDatabase
        val id = uri.pathSegments[1]
        return db.update(MessagesEntry.TABLE_NAME, values, "_id=?", arrayOf(id))
            .also { if (it != 0) context?.contentResolver?.notifyChange(uri, null) }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (uriMatcher.match(uri) != MESSAGE_WITH_ID) {
            throw UnsupportedOperationException("Unknown uri: $uri")
        }

        val db = squawkDbHelper.writableDatabase
        val id = uri.pathSegments[1]
        return db.delete(MessagesEntry.TABLE_NAME, "_id=?", arrayOf(id))
            .also { if (it != 0) context?.contentResolver?.notifyChange(uri, null) }
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        MESSAGES -> "vnd.android.cursor.dir/{${SquawkContract.AUTHORITY}.${SquawkContract.PATH_MESSAGES}"
        MESSAGE_WITH_ID -> "vnd.android.cursor.item/{${SquawkContract.AUTHORITY}.${SquawkContract.PATH_MESSAGES}"
        else -> throw java.lang.UnsupportedOperationException("Unknown uri: $uri")
    }

    companion object {
        const val MESSAGES = 100
        const val MESSAGE_WITH_ID = 101
        val uriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(SquawkContract.AUTHORITY, SquawkContract.PATH_MESSAGES, MESSAGES)
            addURI(SquawkContract.AUTHORITY, "${SquawkContract.PATH_MESSAGES}/#", MESSAGE_WITH_ID)
        }
    }
}