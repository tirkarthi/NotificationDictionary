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

import androidx.room.*

@Dao
interface HistoryDao {
    @Query("SELECT * from history where word = :word")
    fun getHistory(word: String): History?

    @Upsert
    fun insertHistory(history: History)

    @Query("UPDATE history set last_accessed_at = :lastAccessedAt where word = :word")
    fun updateHistory(lastAccessedAt: Long, word: String)

    @Query("DELETE from history where word = :word")
    fun deleteHistory(word: String)

    @Query("SELECT * from history")
    fun getAllEntries(): List<History>

    @Query("select h.word, h.is_favourite as isFavourite, h.last_accessed_at as lastAccessedAt, d.definition, d.lexical_category as lexicalCategory from history as h join dictionary as d where h.word = d.word group by h.word order by h.last_accessed_at desc;")
    fun getAllEntriesWithMeaning(): List<WordWithMeaning>

    @Query("select h.word, h.is_favourite as isFavourite, h.last_accessed_at as lastAccessedAt, d.definition, d.lexical_category as lexicalCategory from history as h join dictionary as d where h.word = d.word and h.is_favourite = 1 group by h.word order by h.last_accessed_at desc;")
    fun getAllFavouriteEntriesWithMeaning(): List<WordWithMeaning>

    @Query("UPDATE history set is_favourite = 1 where word = :word")
    fun addFavourite(word: String)

    @Query("UPDATE history set is_favourite = 0 where word = :word")
    fun removeFavourite(word: String)

    @Query("SELECT * from history where is_favourite = 1")
    fun getFavouriteEntries(): List<History>

    class WordWithMeaning(var isFavourite: Int? = 0, var word: String? = "",
                          var definition: String? = ""
    ) {
        var lastAccessedAt: Int? = 0
        var lexicalCategory: String? = ""
    }
}
