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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class RoomAdapter(data: List<Word>, context: Context) :
    RecyclerView.Adapter<RoomAdapter.ViewHolder>() {
    private val meaningList: List<Word> = data
    private val context: Context = context

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var lexicalCategory: TextView = itemView.findViewById(R.id.lexicalCategory)
        var wordMeaning: TextView = itemView.findViewById(R.id.wordMeaning)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_layout, parent, false)

        return ViewHolder(view)
    }

    private fun formatHtml(content: String): Spanned? {
        return Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.lexicalCategory.text = formatHtml("<b> ${meaningList[position].lexicalCategory}")
        holder.wordMeaning.text = formatHtml("${meaningList[position].definition} <br>")

        // https://stackoverflow.com/questions/13941093/how-to-share-entire-android-app-with-share-intent
        holder.wordMeaning.setOnLongClickListener(View.OnLongClickListener { view ->
            val sharingIntent = Intent(Intent.ACTION_SEND)
            // https://stackoverflow.com/questions/3918517/calling-startactivity-from-outside-of-an-activity-context
            sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            sharingIntent.type = "text/plain"
            val shareBody =
                "${
                    meaningList[position].word!!.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                } \n\n${meaningList[position].definition} \n\nSent via Notification Dictionary (https://play.google.com/store/apps/details?id=com.xtreak.notificationdictionary)"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Meaning")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
            // https://stackoverflow.com/questions/62166087/using-intent-createchooser-and-getting-error-calling-startactivity-from-outsi
            val chooserIntent = Intent.createChooser(
                sharingIntent,
                "Share via"
            )
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.applicationContext.startActivity(
                chooserIntent
            )
            true // Satisfy type checker
        })

        // https://stackoverflow.com/questions/43262912/copy-to-clipboard-the-content-of-a-cardview
        holder.wordMeaning.setOnClickListener(View.OnClickListener { view ->
            val myClipboard =
                view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("label", meaningList[position].definition)
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(view.context, "Copied", Toast.LENGTH_SHORT).show()
        })
    }

    override fun getItemCount(): Int {
        return meaningList.size
    }

}