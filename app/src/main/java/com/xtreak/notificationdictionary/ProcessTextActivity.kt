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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import io.sentry.Sentry
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class ProcessTextActivity : AppCompatActivity() {

    private val CHANNEL_ID = "Dictionary"
    private val CHANNEL_NUMBER = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext
        val word = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString().lowercase()

        val executor = Executors.newSingleThreadExecutor()
        var definition = "No meaning found"

        // https://stackoverflow.com/questions/1250643/how-to-wait-for-all-threads-to-finish-using-executorservice
        executor.execute {
            val database = AppDatabase.getDatabase(this)
            val dao = database.dictionaryDao()
            var meaning: Word?
            try {
                meaning = dao.getMeaningsByWord(word, 1)
            } catch (e: Exception) {
                Sentry.captureException(e)
                meaning = Word(
                    1, "", "Error", 1, 1,
                    "There was an error while trying to fetch the meaning. The app tries to download the database at first launch for offline usage." +
                            "The error usually occurs if the database was not downloaded properly due to network issue during start or changing language." +
                            "Please turn on your internet connection and restart the app to download the database."
                )
            }
            definition = meaning?.definition ?: "No meaning found"
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
            .setTimeoutAfter(20000)

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
        Sentry.captureMessage("Process text event")
        val pendingIntent = stack.getPendingIntent(0, flags)

        builder.setContentIntent(pendingIntent)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(CHANNEL_NUMBER, builder.build())

        // Android intent filters should have an activity but we need to raise only a notification, so call finish
        // When the app is not open in background or actively running the white screen appears for a second or so.
        this.finish()
    }
}