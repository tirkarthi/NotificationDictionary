/*
 * Copyright (c) 2023, Karthikeyan Singaravelan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xtreak.notificationdictionary

import android.util.Log
import java.lang.Exception
import java.util.*

/**
 * Enrich meanings with the original word meaning in case of plural and different tense
 * Plural of apple => Plural of apple (A common, round fruit)
 */
fun resolveRedirectMeaning(
    meanings: List<Word>,
    dao: DictionaryDao
) {
    val redirectRegex = """(singular form|plural|present participle|past participle) of (\w+)""".toRegex(RegexOption.IGNORE_CASE)

    for (meaning in meanings) {
        var definition = meaning.definition
        redirectRegex.find(definition.toString())?.let {
            val (_, singularWord) = it.destructured
            dao.getMeaningsByWord(singularWord.toString().trim().lowercase(Locale.getDefault()), 1)?.let {
                val singularDefinition = it.definition!!
                meaning.definition = meaning.definition?.replace(
                    singularWord,
                    "$singularWord ($singularDefinition)"
                )
            }
        }
    }
}

fun addHistoryEntry(
    historyDao: HistoryDao,
    word: String
) {

    val historyEntry = historyDao.getHistory(word)

    try {
        if (historyEntry != null) {
            historyDao.updateHistory(
                word = word,
                lastAccessedAt = System.currentTimeMillis()
            )
        } else {
            historyDao.insertHistory(
                History(
                    id = null,
                    word = word,
                    isFavourite = 0,
                    lastAccessedAt = System.currentTimeMillis()
                )
            )
        }
    } catch (e: Exception) {
    }

}

// If the last or first letter is a punctuation then remove it.
fun removePunctuation(word: String): String {
    var word = word;

    if (!word.last().isLetter()) {
        word = word.substring(0, word.length - 1)
    }

    if (!word.first().isLetter()) {
        word = word.substring(1, word.length - 1)
    }

    return word
}
