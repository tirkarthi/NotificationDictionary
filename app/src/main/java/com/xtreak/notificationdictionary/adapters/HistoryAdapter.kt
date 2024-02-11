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
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xtreak.notificationdictionary.History
import com.xtreak.notificationdictionary.HistoryDao
import com.xtreak.notificationdictionary.R


class HistoryAdapter(data: List<HistoryDao.WordWithMeaning>, context: Context) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private val meaningList: List<HistoryDao.WordWithMeaning> = data
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

    override fun getItemCount(): Int {
        return meaningList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.lexicalCategory.text = formatHtml("<b> ${meaningList[position].word}")
        holder.wordMeaning.text = formatHtml("${meaningList[position].definition } <br>")
    }
}
