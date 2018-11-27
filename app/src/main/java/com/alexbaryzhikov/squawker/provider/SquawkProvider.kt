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

import android.net.Uri

import net.simonvt.schematic.annotation.ContentProvider
import net.simonvt.schematic.annotation.ContentUri
import net.simonvt.schematic.annotation.TableEndpoint

/**
 * Uses the Schematic (https://github.com/SimonVT/schematic) to create a content provider and
 * define URIs for the provider
 */
@ContentProvider(authority = SquawkProvider.AUTHORITY, database = SquawkDatabase::class)
object SquawkProvider {
    const val AUTHORITY = "com.alexbaryzhikov.squawker.provider"

    @TableEndpoint(table = SquawkDatabase.SQUAWK_MESSAGES)
    object SquawkMessages {
        @ContentUri(
            path = "messages",
            type = "vnd.android.cursor.dir/messages",
            defaultSort = "${SquawkContract.COLUMN_DATE} DESC"
        )
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/messages")
    }
}