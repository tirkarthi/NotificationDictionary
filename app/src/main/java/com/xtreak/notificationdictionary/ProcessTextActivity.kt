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

import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import io.sentry.Sentry
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private class TTSOnInitListener(
    private val in_word: String,
    private val in_definition: String,
    private val context: Context
) : TextToSpeech.OnInitListener {
    val tts = TextToSpeech(context, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            Log.d("ndict current locale", Locale.getDefault().language)
            tts.speak("$in_word, $in_definition", TextToSpeech.QUEUE_FLUSH, null)
            Sentry.captureMessage("Process read event.")
        }
    }
}

open class ProcessIntentActivity : AppCompatActivity() {

    private val CHANNEL_ID = "Dictionary"
    private val CHANNEL_NUMBER = 1
    private val NOTIFICATION_TIMEOUT = 20000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val word: String
        var lexicalCategory: String = ""
        val context = applicationContext
        val executor = Executors.newSingleThreadExecutor()
        var definition = "No meaning found"

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            word = intent.getCharSequenceExtra(Intent.EXTRA_TEXT).toString().lowercase()
        } else {
            word = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString().lowercase()
        }


        // https://stackoverflow.com/questions/1250643/how-to-wait-for-all-threads-to-finish-using-executorservice
        executor.execute {
            val database = AppDatabase.getDatabase(this)
            val dao = database.dictionaryDao()
            val historyDao = database.historyDao()

            var meaning: Word?
            try {
                meaning = dao.getMeaningsByWord(word, 1)
                if (meaning != null) {
                    resolveRedirectMeaning(listOf(meaning), dao)
                    addHistoryEntry(historyDao, word)
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
                meaning = Word(
                    1, "", "Error", 1, 1,
                    "There was an error while trying to fetch the meaning. The app tries to download the database at first launch for offline usage." +
                            "The error usually occurs if the database was not downloaded properly due to network issue during start or changing language." +
                            "Please turn on your internet connection and restart the app to download the database."
                )
            }
            lexicalCategory = meaning?.lexicalCategory ?: ""

            if (meaning?.definition != null) {
                definition = "${lexicalCategory}, ${meaning?.definition}"
            } else {
                definition = "No meaning found"
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        val builder = NotificationCompat.Builder(
            context,
            CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_baseline_fact_check_24)
            .setContentTitle(word)
            .setContentText(definition)
            .setStyle(NotificationCompat.BigTextStyle().bigText(definition))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(null) // sound is set null but still the notification importance level seems to trigger sound
            .setAutoCancel(true)
            .setTimeoutAfter(NOTIFICATION_TIMEOUT.toLong())

        if (definition != "No meaning found") {
            /* https://developer.android.com/reference/android/app/Notification.Builder.html#addAction(android.app.Notification.Action)
                A notification in its expanded form can display up to 3 actions, from left to right in the order they were added.
            */

            addShareButton(word, definition, context, builder)
            addReadButton(word, definition, context, builder)
            addFavouriteButton(word, context, builder)
        }

        val intent = Intent(
            context, MainActivity::class.java
        )

        // Add word to notification so that on opening home the word can be retrieved and filled in home page
        intent.putExtra("NotificationWord", word)
        val stack = TaskStackBuilder.create(context)
        stack.addNextIntentWithParentStack(intent)
        // https://github.com/square/leakcanary/pull/2090
        // https://stackoverflow.com/questions/67045607/how-to-resolve-missing-pendingintent-mutability-flag-lint-warning-in-android-a
        // https://stackoverflow.com/questions/9223420/passing-multiple-flags-to-an-intent-in-android/9223566
        val flags = if (Build.VERSION.SDK_INT > 30) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
        val pendingIntent = stack.getPendingIntent(0, flags)

        builder.setContentIntent(pendingIntent)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(CHANNEL_NUMBER, builder.build())

        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        val read_definition = sharedPref.getBoolean(
            "read_definition",
            false
        )

        Sentry.captureMessage("Process text event")
        if (read_definition) {
            TTSOnInitListener(word, definition, context)
        }
        // Android intent filters should have an activity but we need to raise only a notification, so call finish
        // When the app is not open in background or actively running the white screen appears for a second or so.
        this.finish()
    }

    private fun addShareButton(
        word: String,
        definition: String,
        context: Context,
        builder: NotificationCompat.Builder
    ) {
        val notificationShare: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                val content =
                    "${word}\n\n${definition}\n\nSent via Notification Dictionary (https://play.google.com/store/apps/details?id=com.xtreak.notificationdictionary)"
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, word)
                sharingIntent.putExtra(Intent.EXTRA_TEXT, content)
                startActivity(Intent.createChooser(sharingIntent, "Share via"))

                // unregister the receiver else they will keep adding themselves to context resulting in duplicate calls
                try {
                    context.unregisterReceiver(this)
                    Sentry.captureMessage("Process share event.")
                } catch (e: IllegalArgumentException) {
                    Sentry.captureException(e)
                    Log.e("Notification Dictionary", "Error in unregistering the receiver")
                }
            }
        }

        val intentFilter = IntentFilter("com.xtreak.notificationdictionary.ACTION_SHARE")
        context.registerReceiver(notificationShare, intentFilter)

        val share = Intent("com.xtreak.notificationdictionary.ACTION_SHARE")
        val nShare =
            PendingIntent.getBroadcast(
                context,
                0,
                share,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_menu_share,
                "Share",
                nShare
            )
        )
    }

    private fun addReadButton(
        word: String,
        definition: String,
        context: Context,
        builder: NotificationCompat.Builder,
    ) {
        val notificationRead: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                TTSOnInitListener(word, definition, context)

                // unregister the receiver else they will keep adding themselves to context resulting in duplicate calls
                try {
                    context.unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Sentry.captureException(e)
                    Log.e("Notification Dictionary", "Error in unregistering the receiver")
                }
            }
        }

        val intentFilter = IntentFilter("com.xtreak.notificationdictionary.ACTION_TTS")
        context.registerReceiver(notificationRead, intentFilter)

        val read = Intent("com.xtreak.notificationdictionary.ACTION_TTS")
        val nRead =
            PendingIntent.getBroadcast(
                context,
                0,
                read,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        builder.addAction(NotificationCompat.Action(null, "Read", nRead))
    }

    private fun addFavouriteButton(
        word: String,
        context: Context,
        builder: NotificationCompat.Builder
    ) {
        val database = AppDatabase.getDatabase(this)

        val notificationFavourite: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val executor = Executors.newSingleThreadExecutor()

                // unregister the receiver else they will keep adding themselves to context resulting in duplicate calls
                try {
                    executor.execute {
                        val historyDao = database.historyDao()
                        historyDao.addFavourite(word)
                    }
                    context.unregisterReceiver(this)
                    Sentry.captureMessage("Process favourite event.")
                } catch (e: Exception) {
                    Sentry.captureException(e)
                } finally {
                    executor.shutdown()
                }
            }
        }

        val intentFilter = IntentFilter("com.xtreak.notificationdictionary.ACTION_FAVOURITE")
        context.registerReceiver(notificationFavourite, intentFilter)

        val star = Intent("com.xtreak.notificationdictionary.ACTION_FAVOURITE")
        val nStar =
            PendingIntent.getBroadcast(
                context,
                0,
                star,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        builder.addAction(NotificationCompat.Action(null, "Favourite", nStar))
    }


}


// Android needs different classes for different intent filters. So add one PROCESS_TEXT and another for VIEW

class ProcessViewActivity : ProcessIntentActivity()

class ProcessTextActivity : ProcessIntentActivity()