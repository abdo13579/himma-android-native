package com.himma.app.domain.text

/**
 * Offline Arabic & mixed-script search normalizer.
 *
 * Strips Arabic diacritics (tashkeel) and tatweel, and normalizes hamza/alef/ya/taa-marbuta
 * variants (`أ/إ/آ -> ا`, `ى/ئ -> ي`, `ة -> ه`, `ؤ -> و`) so offline Room queries match
 * regardless of spelling style or diacritic marks.
 */
object ArabicTextNormalizer {

    private val TASHKEEL_AND_TATWEEL = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val MULTI_SPACE = Regex("\\s+")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val withoutDiacritics = TASHKEEL_AND_TATWEEL.replace(input, "")
        val builder = StringBuilder(withoutDiacritics.length)
        for (ch in withoutDiacritics) {
            builder.append(
                when (ch) {
                    '\u0623', '\u0625', '\u0622', '\u0671' -> '\u0627' // أ إ آ ٱ -> ا
                    '\u0649', '\u0626' -> '\u064A' // ى ئ -> ي
                    '\u0629' -> '\u0647' // ة -> ه
                    '\u0624' -> '\u0648' // ؤ -> و
                    else -> ch.lowercaseChar()
                },
            )
        }
        return MULTI_SPACE.replace(builder.toString(), " ").trim()
    }
}
