package com.rukavina.gymbuddy.ui.design

/**
 * Keypad text editing and parsing (Plan 7 05b; 7.6 reuses it). The text
 * keeps whatever separator was typed; parsing accepts "." and ",".
 */
object KeypadValue {

    /** Kind of value: whole numbers, decimals, or m:ss typed from the right. */
    enum class Kind { Whole, Decimal, Clock }

    private const val MAX_WHOLE = 4
    private const val MAX_DECIMALS = 2
    private const val MAX_CLOCK = 4

    /**
     * Text after [input]. [separator] is what Decimal inserts. Next and
     * Cancel don't change the text. A leading 0 is replaced, except before
     * the separator.
     */
    fun apply(text: String, input: KeypadInput, kind: Kind, separator: Char = '.'): String = when (input) {
        is KeypadInput.Digit -> digit(text, input.n, kind)
        KeypadInput.Decimal -> if (kind != Kind.Decimal || text.any { it == '.' || it == ',' }) text
            else (text.ifEmpty { "0" }) + separator
        KeypadInput.Backspace -> text.dropLast(1)
        KeypadInput.Next, KeypadInput.Cancel -> text
    }

    private fun digit(text: String, n: Int, kind: Kind): String {
        val sep = text.indexOfFirst { it == '.' || it == ',' }
        return when {
            kind == Kind.Clock -> if (text.length >= MAX_CLOCK) text else (text + n).trimStart('0')
            sep >= 0 -> if (text.length - sep - 1 >= MAX_DECIMALS) text else text + n
            text == "0" -> n.toString()
            text.length >= MAX_WHOLE -> text
            else -> text + n
        }
    }

    /** "12,5" or "12.5" → 12.5. Empty or "." → null. */
    fun parse(text: String): Float? = text.replace(',', '.').toFloatOrNull()

    /** Whole number, null when empty. */
    fun parseWhole(text: String): Int? = text.toIntOrNull()

    /** Clock digits from the right: "130" → 90 s, "5" → 5 s, "" → null. Seconds over 59 carry. */
    fun parseClock(text: String): Int? {
        val digits = text.toIntOrNull() ?: return null
        return digits / 100 * 60 + digits % 100
    }

    /** Clock digits for [seconds]: 90 → "130". */
    fun clockDigits(seconds: Int?): String =
        if (seconds == null || seconds <= 0) "" else (seconds / 60 * 100 + seconds % 60).toString()

    /** Clock digits shown as m:ss while typing: "130" → "1:30", "" → "0:00". */
    fun clockText(digits: String): String {
        val padded = digits.padStart(3, '0')
        return padded.dropLast(2).trimStart('0').ifEmpty { "0" } + ":" + padded.takeLast(2)
    }
}
