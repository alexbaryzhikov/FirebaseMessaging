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

import android.annotation.SuppressLint
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alexbaryzhikov.squawker.provider.SquawkContract
import java.text.SimpleDateFormat
import java.util.*

/**
 * Converts cursor data for squawk messages into visible list items in a RecyclerView
 */
class SquawkAdapter : RecyclerView.Adapter<SquawkAdapter.SquawkViewHolder>() {
    private var data: Cursor? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SquawkViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_squawk_list, parent, false)
        return SquawkViewHolder(v)
    }

    override fun onBindViewHolder(holder: SquawkViewHolder, position: Int) {
        val dataCp = data ?: return
        dataCp.moveToPosition(position)

        val message = dataCp.getString(MainActivity.COL_NUM_MESSAGE)
        val author = dataCp.getString(MainActivity.COL_NUM_AUTHOR)
        val authorKey = dataCp.getString(MainActivity.COL_NUM_AUTHOR_KEY)

        // Get the date for displaying
        val dateMillis = dataCp.getLong(MainActivity.COL_NUM_DATE)
        val now = System.currentTimeMillis()

        // Change how the date is displayed depending on whether it was written in the last minute,
        // the hour, etc.
        var date = if (now - dateMillis < DAY_MILLIS) {
            if (now - dateMillis < HOUR_MILLIS) {
                val minutes = Math.round((now - dateMillis) / MINUTE_MILLIS.toDouble())
                "${minutes}m"
            } else {
                val minutes = Math.round((now - dateMillis) / HOUR_MILLIS.toDouble())
                "${minutes}h"
            }
        } else {
            val dateDate = Date(dateMillis)
            dateFmt.format(dateDate)
        }

        // Add a dot to the date string
        date = "\u2022 $date"

        holder.messageTextView.text = message
        holder.authorTextView.text = author
        holder.dateTextView.text = date

        // Choose the correct, and in this case, locally stored asset for the instructor. If there
        // were more users, you'd probably download this as part of the message.
        when (authorKey) {
            SquawkContract.ASSER_KEY -> holder.authorImageView.setImageResource(R.drawable.asser)
            SquawkContract.CEZANNE_KEY -> holder.authorImageView.setImageResource(R.drawable.cezanne)
            SquawkContract.JLIN_KEY -> holder.authorImageView.setImageResource(R.drawable.jlin)
            SquawkContract.LYLA_KEY -> holder.authorImageView.setImageResource(R.drawable.lyla)
            SquawkContract.NIKITA_KEY -> holder.authorImageView.setImageResource(R.drawable.nikita)
            else -> holder.authorImageView.setImageResource(R.drawable.test)
        }
    }

    override fun getItemCount(): Int {
        return data?.count ?: 0
    }

    internal fun swapCursor(newCursor: Cursor?) {
        // Finalize old cursor
        data?.close()
        // Setup new cursor
        data = newCursor
        notifyDataSetChanged()
    }

    inner class SquawkViewHolder(layoutView: View) : RecyclerView.ViewHolder(layoutView) {
        val authorTextView: TextView = layoutView.findViewById(R.id.author_text_view)
        val messageTextView: TextView = layoutView.findViewById(R.id.message_text_view)
        val dateTextView: TextView = layoutView.findViewById(R.id.date_text_view)
        val authorImageView: ImageView = layoutView.findViewById(R.id.author_image_view)
    }

    companion object {
        private const val TAG = "SquawkAdapter"
        private const val MINUTE_MILLIS = 1000L * 60
        private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS = 24 * HOUR_MILLIS
        @SuppressLint("ConstantLocale")
        private val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    }
}
