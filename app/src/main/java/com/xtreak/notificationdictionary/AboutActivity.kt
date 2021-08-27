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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element


class AboutActivity : AppCompatActivity() {
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
}