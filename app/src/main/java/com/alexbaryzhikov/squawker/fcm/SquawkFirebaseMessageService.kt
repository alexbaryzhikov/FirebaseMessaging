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
package com.alexbaryzhikov.squawker.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alexbaryzhikov.squawker.MainActivity
import com.alexbaryzhikov.squawker.R
import com.alexbaryzhikov.squawker.provider.SquawkContract.MessagesEntry
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Listens for squawk FCM messages both in the background and the foreground and responds
 * appropriately depending on type of message
 */
class SquawkFirebaseMessageService : FirebaseMessagingService() {

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // There are two types of messages data messages and notification messages. Data messages
        // are handled here in onMessageReceived whether the app is in the foreground or background.
        // Data messages are the type traditionally used with FCM. Notification messages are only
        // received here in onMessageReceived when the app is in the foreground. When the app is in
        // the background an automatically generated notification is displayed. When the user taps on
        // the notification they are returned to the app. Messages containing both notification and
        // data payloads are treated as notification messages. The Firebase console always sends
        // notification messages.
        // For more see: https://firebase.google.com/docs/cloud-messaging/concept-options\

        // The Squawk server always sends just *data* messages, meaning that onMessageReceived when
        // the app is both in the foreground AND the background

        Log.i(TAG, "From: " + remoteMessage?.from)

        // Check if message contains a data payload.
        remoteMessage?.data?.let { data ->
            if (data.isEmpty()) return@let

            Log.i(TAG, "Message data payload: $data")
            sendNotification(data)
            insertSquawk(data)
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    /**
     * Inserts a single squawk into the database;
     *
     * @param data Map which has the message data in it
     */
    private fun insertSquawk(data: Map<String, String>) = GlobalScope.launch {
        val newMessage = ContentValues()
        newMessage.put(MessagesEntry.COLUMN_AUTHOR, data[JSON_KEY_AUTHOR])
        newMessage.put(MessagesEntry.COLUMN_MESSAGE, data[JSON_KEY_MESSAGE]?.trim { it <= ' ' })
        newMessage.put(MessagesEntry.COLUMN_DATE, data[JSON_KEY_DATE])
        newMessage.put(MessagesEntry.COLUMN_AUTHOR_KEY, data[JSON_KEY_AUTHOR_KEY])
        contentResolver.insert(MessagesEntry.CONTENT_URI, newMessage)
    }

    /**
     * Create and show a simple notification containing the received FCM message
     *
     * @param data Map which has the message data in it
     */
    private fun sendNotification(data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Create the pending intent to launch the activity
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val author = data[JSON_KEY_AUTHOR]
        var message = data[JSON_KEY_MESSAGE]

        // If the message is longer than the max number of characters we want in our
        // notification, truncate it and add the unicode character for ellipsis
        if (message != null && message.length > NOTIFICATION_MAX_CHARACTERS) {
            message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026"
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_duck)
            .setContentTitle(String.format(getString(R.string.notification_message), author))
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        // Create notification channel
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Persist token to third-party servers.
     *
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun sendRegistrationToServer(token: String?) {
        // This method is blank, but if you were to build a server that stores users token
        // information, this is where you'd send the token to the server.
    }

    companion object {
        private const val JSON_KEY_AUTHOR = MessagesEntry.COLUMN_AUTHOR
        private const val JSON_KEY_AUTHOR_KEY = MessagesEntry.COLUMN_AUTHOR_KEY
        private const val JSON_KEY_MESSAGE = MessagesEntry.COLUMN_MESSAGE
        private const val JSON_KEY_DATE = MessagesEntry.COLUMN_DATE

        private const val NOTIFICATION_MAX_CHARACTERS = 30
        private const val CHANNEL_ID = "Squawker"
        private const val TAG = "SquawkFMS"
    }
}
