/*
 * Copyright (c) 2024, Karthikeyan Singaravelan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xtreak.notificationdictionary.adapters

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.xtreak.notificationdictionary.AppDatabase
import com.xtreak.notificationdictionary.HistoryDao
import com.xtreak.notificationdictionary.MainActivity
import com.xtreak.notificationdictionary.R
import java.util.concurrent.Executors


class HistoryAdapter(
    data: MutableList<HistoryDao.WordWithMeaning>,
    context: Context,
    favourite_page: Boolean
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private val meaningList: MutableList<HistoryDao.WordWithMeaning> = data
    private val context: Context = context
    private val favourite_page: Boolean = favourite_page

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var lexicalCategory: TextView = itemView.findViewById(R.id.lexicalCategoryhistory)
        var wordMeaning: TextView = itemView.findViewById(R.id.wordMeaninghistory)
        var deleteEntry: ImageButton = itemView.findViewById(R.id.deleteEntryhistory)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_layout, parent, false)

        return ViewHolder(view)
    }

    private fun formatHtml(content: String): Spanned? {
        return Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
    }

    override fun getItemCount(): Int {
        return meaningList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position > meaningList.size) {
            return
        }

        val word = meaningList[position].word!!
        holder.lexicalCategory.text = formatHtml("<b> ${word}")
        holder.wordMeaning.text = formatHtml("${meaningList[position].definition} <br>")

        val listener = {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("NotificationWord", word)
            startActivity(context, intent, null)
        }

        holder.wordMeaning.setOnClickListener {
            listener()
        }

        holder.lexicalCategory.setOnClickListener {
            listener()
        }

        holder.deleteEntry.setOnClickListener {
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {

                // Reuse such that if it's history page remove the word and if it's favourite then toggle
                // favourite column. Maybe refactor this to a method and have a base class to override the
                // action instead of a toggle here.
                if (!this.favourite_page) {
                    val database = AppDatabase.getDatabase(context)
                    val historyDao = database.historyDao()
                    historyDao.deleteHistory(word)
                } else {
                    val database = AppDatabase.getDatabase(context)
                    val historyDao = database.historyDao()
                    historyDao.removeFavourite(word)
                }
                meaningList.removeAt(position)

                handler.post {
                    this.notifyDataSetChanged()
                }
            }
        }
    }
}
