package com.rukavina.gymbuddy.domain.search

import java.text.Normalizer

/**
 * Search that forgives typos (Figma app frame "Search forgives typos",
 * Plan 7 05b decision 8). Shared by the exercise picker and, in 7.7,
 * the Exercises list.
 *
 * Every query word must match the text: as a substring first, else a
 * word of the text (or its start) within edit distance 1. Words under
 * 3 letters must be substrings (one typo in "ab" matches everything).
 * Case and accents are ignored.
 */
object TypoMatcher {

    /** Rank for sorting: lower is better. */
    enum class Match { Exact, Typo }

    private const val MIN_TYPO_LENGTH = 3

    /** Null: no match. A blank query matches everything as [Match.Exact]. */
    fun match(query: String, text: String): Match? {
        val words = words(query)
        if (words.isEmpty()) return Match.Exact
        val haystack = normalize(text)
        val textWords = words(text)
        var typo = false
        for (word in words) {
            if (haystack.contains(word)) continue
            if (word.length < MIN_TYPO_LENGTH || textWords.none { closeTo(word, it) }) return null
            typo = true
        }
        return if (typo) Match.Typo else Match.Exact
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()

    private fun words(text: String): List<String> =
        normalize(text).split(SEPARATORS).filter { it.isNotEmpty() }

    /** [word] is within one edit of [target] or of a prefix of it (typing in progress). */
    private fun closeTo(word: String, target: String): Boolean {
        if (withinOne(word, target)) return true
        for (len in (word.length - 1)..(word.length + 1)) {
            if (len in 1 until target.length && withinOne(word, target.substring(0, len))) return true
        }
        return false
    }

    /** Levenshtein distance ≤ 1, in one pass. */
    internal fun withinOne(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++; j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        edits += (a.length - i) + (b.length - j)
        return edits <= 1
    }

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
}
