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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suddenh4x.ratingdialog.AppRating
import com.suddenh4x.ratingdialog.preferences.RatingThreshold
import java.io.File
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var progress_dialog: ProgressDialog
    private val CHANNEL_ID = "Dictionary"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        val package_data_directory =
            Environment.getDataDirectory().absolutePath + "/data/" + packageName
        val file = File("$package_data_directory/databases/dictionary.db")

        if (!file.exists()) {
            initialize_database()
        }

        val mRecyclerView = findViewById<RecyclerView>(R.id.meaningRecyclerView)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = linearLayoutManager

        onNewIntent(intent)

        AppRating.Builder(this)
            .setMinimumLaunchTimes(10)
            .setMinimumDays(2)
            .setMinimumLaunchTimesToShowAgain(15)
            .setMinimumDaysToShowAgain(10)
            .setRatingThreshold(RatingThreshold.FIVE)
            .useGoogleInAppReview()
            .showIfMeetsConditions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_us -> {
                val about_activity = Intent(applicationContext, AboutActivity::class.java)
                startActivityForResult(about_activity, 0)
            }
            R.id.license -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
            }
        }
        return true
    }

    private fun initialize_database() {
        // declare the dialog as a member field of your activity
        // ProgressDialog is deprecated in documentation to use ProgressBar.
        // But we don't want the user to cancel this. It's one time and takes a couple of seconds
        val mProgressDialog: ProgressDialog

        mProgressDialog = ProgressDialog(this@MainActivity)
        mProgressDialog.setMessage("Initializing database. This will take a few seconds.")
        mProgressDialog.isIndeterminate = true
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog.setCancelable(false)
        mProgressDialog.show()

        // AsyncTask is deprecated : https://stackoverflow.com/questions/58767733/android-asynctask-api-deprecating-in-android-11-what-are-the-alternatives
        // https://www.simplifiedcoding.net/android-asynctask/
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            // A hack to copy database file from assets as part of DB initialization.
            // This should ideally be a setup time task to copy file instead of doing it as a workaround.
            val database = AppDatabase.getDatabase(this@MainActivity)
            val dao = database.dictionaryDao()
            dao.getAllMeaningsByWord("Hello")

            handler.post {
                mProgressDialog.dismiss()
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        // Launch the activity from notification with word filled if present.
        // Fresh start of app won't have NotificationWord value since it's only
        // set as part of notification creation.
        super.onNewIntent(intent)
        val extras = intent.extras

        if (extras != null) {
            val word = extras.getString("NotificationWord")
            if (word != null) {
                val wordEdit = findViewById<EditText>(R.id.wordInput)
                val searchButton = findViewById<TextView>(R.id.searchButton)

                // Fill the text box with word and emulate click to get all meanings
                wordEdit.setText(word)
                searchButton.performClick()
            }
        }
    }

    // https://developer.android.com/training/notify-user/build-notification#kotlin
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendMessage(view: View) {
        val wordEdit = findViewById<EditText>(R.id.wordInput)

        // https://stackoverflow.com/questions/18414804/android-edittext-remove-focus-after-clicking-a-button
        wordEdit.clearFocus()
        wordEdit.isEnabled = false
        wordEdit.isEnabled = true

        val word = wordEdit.text.toString().lowercase()

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val database = AppDatabase.getDatabase(this)
            val dao = database.dictionaryDao()
            val meanings = dao.getAllMeaningsByWord(word)

            handler.post {
                val mRecyclerView = findViewById<RecyclerView>(R.id.meaningRecyclerView)
                var mListadapter =
                    RoomAdapter(listOf(Word(1, "", "Unknown", 1, 1, "No meaning found")), this)

                if (meanings.isNotEmpty()) {
                    mListadapter = RoomAdapter(meanings, this)
                }

                mRecyclerView.adapter = mListadapter
                mListadapter.notifyItemRangeChanged(1, 100)
            }
        }
    }
}