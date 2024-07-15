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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.PackageInfoCompat

data class AppVersion(
    val versionName: String,
    val versionNumber: Long,
)
/*class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aboutPage: View = AboutPage(this)
            .isRTL(false)
            .setImage(R.mipmap.ic_launcher)
            .addItem(Element().setTitle("Version : " + BuildConfig.VERSION_NAME))
            .addGroup("Author : Karthikeyan Singaravelan")
            .addGroup("License : MIT License")
            .addEmail("tir.karthi@gmail.com")
            .addTwitter("tirkarthi")
            .addPlayStore(applicationContext.packageName)
            .addGitHub("tirkarthi/NotificationDictionary")
            .setDescription(getString(R.string.about_description))
            .create()

        setContentView(aboutPage)
    }
}*/
class AboutActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            AboutScreen()
        }
    }
}
@Composable
@Preview
fun AboutScreen(){
    MaterialTheme{
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primaryContainer) {
            Column{
            //Icon(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = null )
            Text(text = "Version : unknown" /*getAppVersion(LocalContext.current)*/)
            Text(text = "Author : Karthikeyan Singaravelan")
            Text(text = "License : MIT License")
                val uriHandler = LocalUriHandler.current
                Row{
                    SendEmailButton()
                    Button(onClick = {uriHandler.openUri("https://x.com/tirkarthi")}){
                        Text(text = "Find me on X")
                    }
                }
                Button(onClick = {uriHandler.openUri("https://github.com/tirkarthi/NotificationDictionary")}){
                    Text(text = "Find my project on GitHub")
                }
           Button(onClick = { uriHandler.openUri("https://play.google.com/store/apps/details?id=com.xtreak.notificationdictionary") }) {
            Text(text = "Rate me on Google Play")}
            Text(text = stringResource(id = R.string.about_description), style = MaterialTheme.typography.bodyLarge)
             }
        }
    }
}
fun getAppVersion(context: Context): AppVersion {
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode: Long = PackageInfoCompat.getLongVersionCode(packageInfo)
        return AppVersion(versionName, versionCode)
    } catch (e: PackageManager.NameNotFoundException ) {
        e.printStackTrace()
        // Fallback version in case of error
        return AppVersion("Unknown", 0)
    }
}
@Composable
fun SendEmailButton() {
    val context = LocalContext.current
    val openAlertDialog = remember { mutableStateOf(false) }
        Button(onClick = {
            try{
            context.sendMail(to = "tir.karthi@gmail.com", subject = "")
            } catch (e: ActivityNotFoundException) {
                openAlertDialog.value = true
            }
        }
        ) {
            Text(text = "Email me")
        }
    if (openAlertDialog.value){
    AlertDialog(
        onDismissRequest = {
            openAlertDialog.value = false },
        confirmButton = {
            TextButton(onClick = { openAlertDialog.value = false }) {
            Text("OK")
            } },
        title= { Text(text = "No email app") },
        text = { Text(text = "You need an email app to perform this action") },
        icon = {Icons.Default.Info}
    )
    }
}

fun Context.sendMail(to: String, subject: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "vnd.android.cursor.item/email" // or "message/rfc822"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        throw e
    } catch (t: Throwable) {
        // TODO: Handle potential other type of exceptions
    }
}