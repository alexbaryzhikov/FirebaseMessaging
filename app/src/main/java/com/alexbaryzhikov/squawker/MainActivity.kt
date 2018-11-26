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

package com.alexbaryzhikov.squawker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexbaryzhikov.squawker.following.FollowingPreferenceActivity
import com.alexbaryzhikov.squawker.provider.SquawkContract
import com.alexbaryzhikov.squawker.provider.SquawkProvider
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.launch

class MainActivity : ScopedAppActivity() {
    private lateinit var adapter: SquawkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.squawks_recycler_view)

        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true)

        // Use a linear layout manager
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Add dividers
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Specify an adapter
        adapter = SquawkAdapter()
        recyclerView.adapter = adapter

        // Load stored data
        launch {
            val selection = SquawkContract.createSelectionForCurrentFollowers(
                PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
            )
            Log.d(TAG, "Selection is $selection")
            val data = contentResolver.query(
                SquawkProvider.SquawkMessages.CONTENT_URI,
                MESSAGES_PROJECTION,
                selection,
                null,
                SquawkContract.COLUMN_DATE + " DESC"
            )
            adapter.swapCursor(data)
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.d(TAG, "Key: $key Value: $value")
            }
        }

        // Get token from the ID Service you created and show it in a log
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "getInstanceId failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result?.token

            // Log and toast
            val msg = getString(R.string.message_token_format, token)
            Log.d(TAG, msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_following_preferences) {
            // Opens the following activity when the menu icon is pressed
            val startFollowingActivity = Intent(this, FollowingPreferenceActivity::class.java)
            startActivity(startFollowingActivity)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val COL_NUM_AUTHOR = 0
        const val COL_NUM_MESSAGE = 1
        const val COL_NUM_DATE = 2
        const val COL_NUM_AUTHOR_KEY = 3

        private const val TAG = "MainActivity"

        private val MESSAGES_PROJECTION = arrayOf(
            SquawkContract.COLUMN_AUTHOR,
            SquawkContract.COLUMN_MESSAGE,
            SquawkContract.COLUMN_DATE,
            SquawkContract.COLUMN_AUTHOR_KEY
        )
    }
}
