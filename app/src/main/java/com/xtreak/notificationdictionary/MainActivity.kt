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
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.huxq17.download.Pump
import com.huxq17.download.config.DownloadConfig
import com.huxq17.download.core.DownloadListener
import com.mikepenz.aboutlibraries.LibsBuilder
import de.cketti.library.changelog.ChangeLog
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.zip.ZipFile


class MainActivity : AppCompatActivity() {

    private lateinit var progress_dialog: ProgressDialog
    private val CHANNEL_ID = "Dictionary"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        val default_database_key = getString(R.string.default_database)
        val selected_language_key = getString(R.string.selected_language)
        var default_language_value = "en"
        var default_database_value = "dictionary.db"
        val selected_theme = "selected_theme"

        var selected_language = sharedPref.getString(selected_language_key, "UNSET") as String
        val theme = sharedPref.getInt(selected_theme, R.style.Theme_NotificationDictionary)


        // https://stackoverflow.com/questions/4212320/get-the-current-language-in-device
        val current_locale = Locale.getDefault().language

        // On first run if the current locale is one of supported language then use it for
        // better onboarding experience. Example french users on start will have french selected.
        // Current locale might be fr but user might have selected english. In that case check for
        // preference to be UNSET
        if (selected_language == "UNSET") {
            if (current_locale.startsWith(
                    "fr",
                    ignoreCase = true
                )
            ) {
                default_language_value = current_locale
                default_database_value = "dictionary_fr.db"
            } else if (current_locale.startsWith(
                    "de",
                    ignoreCase = true
                )
            ) {
                default_language_value = current_locale
                default_database_value = "dictionary_de.db"
            }
            // Set values here so that
            with(sharedPref.edit()) {
                putString(default_database_key, default_database_value)
                putString(selected_language_key, default_language_value)
                apply()
                commit()
            }
        }

        selected_language =
            sharedPref.getString(selected_language_key, default_language_value) as String
        val database_name =
            sharedPref.getString(default_database_key, default_database_value) as String

        val package_data_directory =
            Environment.getDataDirectory().absolutePath + "/data/" + packageName
        val file = File("$package_data_directory/databases/$database_name")

        if (theme == R.style.Theme_NotificationDictionary) {
            setTheme(R.style.Theme_NotificationDictionary)
        } else {
            setTheme(R.style.Theme_NotificationDictionary_Dark)
        }

        setContentView(R.layout.activity_main)
        setLocale(selected_language)
        setIMEAction()
        createNotificationChannel()

        if (!file.exists()) {
            initialize_database(database_name)
        }

        val mRecyclerView = findViewById<RecyclerView>(R.id.meaningRecyclerView)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = linearLayoutManager

        val mListadapter =
            RoomAdapter(
                listOf(
                    Word(
                        1,
                        "",
                        "Thanks for the support",
                        1,
                        1,
                        """The application is open source and free to use. The development is
                                done in my free time apart from my day job along with download costs for database files 
                                from CDN. If you find the app useful please leave a review in Play store and share the 
                                app with your friends. It will help and encourage me in maintaining the app and adding more features. 
                                Thanks."""
                    )
                ), this
            )
        mRecyclerView.adapter = mListadapter

        initialize_spinner(database_name)
        // show_changelog()
        onNewIntent(intent)
    }

    private fun setIMEAction() {
        val wordEdit = findViewById<EditText>(R.id.wordInput)
        wordEdit.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                sendMessage(v)
                return@OnEditorActionListener true
            }
            false
        })
    }

    fun initialize_spinner(database_name: String) {
        val spinner = findViewById<View>(R.id.spinner) as Spinner
        val languages = arrayOf("English", "French", "German")
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this@MainActivity,
            android.R.layout.simple_spinner_item, languages
        )


        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set spinner selection after setting adapter. https://stackoverflow.com/a/1484546/2610955
        // Pass animated as false so that callback is not triggered. https://stackoverflow.com/a/17336944/2610955
        if (database_name == "dictionary_fr.db") {
            spinner.setSelection(1, false)
        } else if (database_name == "dictionary_de.db") {
            spinner.setSelection(2, false)
        } else {
            spinner.setSelection(0, false)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            //https://stackoverflow.com/questions/31497712/get-previously-selected-item-from-spinner-onitemselectedlistener-event
            var previous: Int = spinner.selectedItemPosition
            var startup_selected = spinner.selectedItem

            // https://stackoverflow.com/questions/5124835/spinner-onitemselected-called-erroneously-without-user-action/10102356#10102356
            // Show dialog initially. Then on clicking no set it false so that it doesn't trigger next time during which reset to true.
            var show_dialog: Boolean = true

            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View?,
                arg2: Int,
                arg3: Long
            ) {
                // animate false doesn't work in oreo. So compare selection and don't trigger
                // This handles startup dialog issue. Then set previous_selected as null so that
                // it's not used for later stages in app lifecycle
                val current_item = spinner.selectedItem
                if (current_item == startup_selected) {
                    startup_selected = null
                    return
                }

                if (show_dialog) {
                    val item = spinner.selectedItem.toString()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.confirmation))
                        .setMessage(getString(R.string.change_confirmation))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                            DialogInterface.OnClickListener { dialog, whichButton ->

                                val sharedPref = applicationContext.getSharedPreferences(
                                    getString(R.string.preference_file_key), Context.MODE_PRIVATE
                                )
                                val default_database_key = getString(R.string.default_database)
                                val selected_language_key = getString(R.string.selected_language)

                                var database_name = "database_en.db"
                                var selected_language = "en"

                                // TODO: Need to organize mapping somewhere. This is not scalable on introducing new languages.
                                if (item == "English") {
                                    database_name = "dictionary.db"
                                    selected_language = "en"
                                    setLocale("en")
                                } else if (item == "French") {
                                    database_name = "dictionary_fr.db"
                                    selected_language = "fr"
                                    setLocale("fr")
                                } else if (item == "German") {
                                    database_name = "dictionary_de.db"
                                    selected_language = "de"
                                }

                                with(sharedPref.edit()) {
                                    putString(default_database_key, database_name)
                                    putString(selected_language_key, selected_language)
                                    apply()
                                    commit()
                                }

                                // As soon as the preference is changed if the file doesn't exist then download
                                val package_data_directory =
                                    Environment.getDataDirectory().absolutePath + "/data/" + packageName
                                val file = File("$package_data_directory/databases/$database_name")

                                if (!file.exists()) {
                                    initialize_database(database_name)
                                }
                                previous = spinner.selectedItemPosition
                            }
                        )
                        .setNegativeButton(android.R.string.no,
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                spinner.setSelection(previous, false)
                                show_dialog = false
                            }).show()
                } else {
                    show_dialog = true
                }

            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    // https://stackoverflow.com/questions/2900023/change-app-language-programmatically-in-android
    fun setLocale(languageCode: String?) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = this.resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Setting locale changes the values only on startup. We need to call
        // recreate() but it can end in a loop as we do it in app startup.
        // Call these manually to refresh but this needs a fix.
        val wordEdit = findViewById<EditText>(R.id.wordInput)
        val searchButton = findViewById<TextView>(R.id.searchButton)
        val word: String? = intent?.extras?.getString("NotificationWord")

        searchButton.text = getString(R.string.search)
    }

    fun show_changelog() {
        val changelog = ChangeLog(this)
        if (changelog.isFirstRun) {
            changelog.logDialog.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)

        val menuItem = menu!!.findItem(R.id.switch_theme)
        val view = MenuItemCompat.getActionView(menuItem)
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        val switch = view.findViewById<View>(R.id.theme_switch_button) as Switch
        switch.isChecked = sharedPref.getInt(
            "selected_theme",
            R.style.Theme_NotificationDictionary
        ) == R.style.Theme_NotificationDictionary_Dark

        // https://stackoverflow.com/questions/32091709/how-to-get-set-action-event-in-android-actionbar-switch
        // https://stackoverflow.com/questions/8811594/implementing-user-choice-of-theme
        // https://stackoverflow.com/questions/2482848/how-to-change-current-theme-at-runtime-in-android
        // recreate needs to be called as per stackoverflow answers after initial theme is set though it's not documented.
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                with(sharedPref.edit()) {
                    putInt("selected_theme", R.style.Theme_NotificationDictionary_Dark)
                    apply()
                    commit()
                }
                setTheme(R.style.Theme_NotificationDictionary_Dark)
                recreate()
            } else {
                with(sharedPref.edit()) {
                    putInt("selected_theme", R.style.Theme_NotificationDictionary)
                    apply()
                    commit()
                }
                setTheme(R.style.Theme_NotificationDictionary)
                recreate()
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_us -> {
                val about_activity = Intent(applicationContext, AboutActivity::class.java)
                startActivityForResult(about_activity, 0)
            }
            R.id.license -> {
                LibsBuilder()
                    .withActivityTitle("Open Source Licenses")
                    .withLicenseShown(true)
                    .start(this)
            }
        }
        return true
    }


    private fun initProgressDialog(): ProgressDialog {
        progress_dialog = ProgressDialog(this)
        progress_dialog.setTitle("Downloading database for initial offline usage")
        progress_dialog.progress = 0
        progress_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progress_dialog.setCancelable(false)
        return progress_dialog
    }

    private fun initialize_database(database_name: String) {
        // declare the dialog as a member field of your activity
        // ProgressDialog is deprecated in documentation to use ProgressBar.
        // But we don't want the user to cancel this. It's one time and takes a couple of seconds

        // TODO: Make this configurable based on environment?
        val url = "https://xtreak.sfo3.cdn.digitaloceanspaces.com/dictionaries/$database_name.zip"
        // val url = "http://192.168.0.105:8000/$database_name.zip" // for local mobile testing
        // val url = "http://10.0.2.2:8000/$database_name.zip" // for local emulator testing

        val progressDialog = initProgressDialog()
        val package_data_directory =
            Environment.getDataDirectory().absolutePath + "/data/" + packageName
        val zip_path = File("$package_data_directory/$database_name.zip").absolutePath

        // https://github.com/huxq17/Pump/blob/master/kotlin_app/src/main/java/com/huxq17/download/demo/MainActivity.kt
        DownloadConfig.newBuilder()
            .setMaxRunningTaskNum(1)
            .setMinUsableStorageSpace(140 * 1024L * 1024) // 140MB as per database size
            .build()
        progressDialog.progress = 0
        progressDialog.show()
        Pump.newRequest(url, zip_path)
            .listener(object : DownloadListener() {

                override fun onProgress(progress: Int) {
                    progressDialog.progress = progress
                }

                fun copy_and_unzip(source: String, destination: String) {
                    val zipfile = ZipFile(source)
                    val entry = zipfile.entries().toList().first()

                    // The zip file only has one entry which is the database. So use it as an
                    // input stream and copy the unzipped file to output stream. Delete the source
                    // zip file to save space.
                    val input_stream = zipfile.getInputStream(entry)
                    val output_stream = FileOutputStream(destination)
                    input_stream.copyTo(output_stream, 1024 * 1024 * 2)
                    File(zip_path).delete()
                }

                override fun onSuccess() {
                    val destination_folder = File("$package_data_directory/databases")
                    val destination_path =
                        File("$package_data_directory/databases/$database_name").absolutePath
                    val source_path = downloadInfo.filePath

                    if (!destination_folder.exists()) {
                        destination_folder.mkdirs()
                    }

                    copy_and_unzip(source_path, destination_path)
                    progressDialog.dismiss()
                    Snackbar.make(
                        findViewById(R.id.mainLayout),
                        "Download finished",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                override fun onFailed() {
                    progressDialog.dismiss()
                    Snackbar.make(
                        findViewById(R.id.mainLayout),
                        "Download failed. Please check your internet connection and relaunch the app.",
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            })
            .forceReDownload(false)
            .threadNum(3)
            .setRetry(3, 200)
            .submit()
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.cacheDir.deleteRecursively() // Delete cache on exit
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

        val word = wordEdit.text.toString().trim().lowercase()

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val database = AppDatabase.getDatabase(this)
            val dao = database.dictionaryDao()
            var meanings: List<Word>

            try {
                meanings = dao.getAllMeaningsByWord(word)
            } catch (e: Exception) {
                meanings = listOf(
                    Word(
                        1, "", "Error", 1, 1,
                        "There was an error while trying to fetch the meaning. The app tries to download the database at first launch for offline usage." +
                                "The error usually occurs if the database was not downloaded properly due to network issue during start or changing language." +
                                "Please turn on your internet connection and restart the app to download the database."
                    )
                )
            }

            try {
                resolveRedirectMeaning(meanings, dao)
            } catch (e: Exception) {
            }

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
