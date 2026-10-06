package com.example.util

import com.example.data.model.Item
import java.util.Locale

object ItemSearchMatcher {

    /**
     * Normalizes text by expanding common unit abbreviations and normalizing spaces.
     * e.g. "20l" -> "20ltr 20l 20 liter"
     */
    fun normalizeQuery(query: String): String {
        var q = query.lowercase(Locale.ROOT).trim()

        // Normalize units attached or separated
        q = q.replace(Regex("(\\d+)\\s*(ltrs?|liters?|litres?|l)\\b"), "$1ltr $1l")
        q = q.replace(Regex("(\\d+)\\s*(kgs?|kilos?|kilograms?)\\b"), "$1kg")
        q = q.replace(Regex("(\\d+)\\s*(gms?|grams?|g)\\b"), "$1gm")
        q = q.replace(Regex("(\\d+)\\s*(mls?|milliliters?)\\b"), "$1ml")
        q = q.replace(Regex("(\\d+)\\s*(mtrs?|meters?|metres?|m)\\b"), "$1mtr")
        q = q.replace(Regex("(\\d+)\\s*(inch|inches|\")\\b"), "$1inch")

        return q
    }

    /**
     * Normalizes spoken voice query to improve hardware/paint recognition.
     * e.g. "ab 2 20 ltr" -> "ab2 20ltr"
     */
    fun normalizeVoiceInput(spoken: String): String {
        var text = spoken.trim()
        // Join letter prefix with numbers: "ab 2" -> "ab2", "ab 6" -> "ab6"
        text = text.replace(Regex("(?i)\\b([a-z]{1,4})\\s+(\\d+)\\b"), "$1$2")
        // Standardize 20 l / 20 ltr -> 20ltr
        text = text.replace(Regex("(?i)\\b(\\d+)\\s+(ltr|l|liter|litres|litre)\\b"), "$1ltr")
        return text
    }

    /**
     * Builds a comprehensive search haystack for an Item containing all fields
     * and their normalized variants (spaces, combined letters+numbers, units).
     */
    fun buildItemSearchHaystack(item: Item): String {
        val sb = StringBuilder()
        val nameLower = item.name.lowercase(Locale.ROOT)
        sb.append(nameLower).append(" ")
        // Name without spaces/punctuation: e.g. "AB 2" -> "ab2"
        sb.append(nameLower.replace(Regex("[^a-z0-9]"), "")).append(" ")
        // Name with letter-number split: e.g. "AB2" -> "ab 2"
        val splitName = nameLower.replace(Regex("([a-z]+)(\\d+)"), "$1 $2")
        sb.append(splitName).append(" ")

        // Size & Unit variants
        val sizeLower = item.size.lowercase(Locale.ROOT)
        if (sizeLower.isNotBlank()) {
            sb.append(sizeLower).append(" ")
            sb.append(sizeLower.replace(Regex("[^a-z0-9]"), "")).append(" ")
            sb.append(normalizeQuery(sizeLower)).append(" ")
            val sizeSplit = sizeLower.replace(Regex("(\\d+)([a-z]+)"), "$1 $2")
            sb.append(sizeSplit).append(" ")

            // If size is e.g. "20ltr" or "20 ltr", also inject "20l", "20 l", "20 liter"
            val literMatch = Regex("(\\d+)\\s*(l|ltr|liter|litres?)").find(sizeLower)
            if (literMatch != null) {
                val n = literMatch.groupValues[1]
                sb.append("${n}l ${n}ltr $n l $n ltr $n liter ")
            }
        }

        sb.append(item.brand.lowercase(Locale.ROOT)).append(" ")
        sb.append(item.type.lowercase(Locale.ROOT)).append(" ")
        sb.append(item.code.lowercase(Locale.ROOT)).append(" ")
        sb.append(item.barcode.lowercase(Locale.ROOT)).append(" ")
        sb.append(item.aliases.lowercase(Locale.ROOT)).append(" ")
        sb.append(item.unit.lowercase(Locale.ROOT)).append(" ")
        if (item.mrp != null) sb.append(item.mrp.toString()).append(" ")

        return sb.toString()
    }

    /**
     * Calculates relevance score (0 = no match, >0 = matches).
     * Handles compound searches across name + size (e.g. "ab2 20l", "ab6 4ltr", "asian paints 20l").
     */
    fun matchScore(item: Item, rawQuery: String): Int {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return 1

        val nameLower = item.name.lowercase(Locale.ROOT)
        val codeLower = item.code.lowercase(Locale.ROOT)
        val barcodeLower = item.barcode.lowercase(Locale.ROOT)
        val hay = buildItemSearchHaystack(item)

        // Exact match on code, barcode, or exact name
        if (codeLower == q || barcodeLower == q || nameLower == q) return 1000

        // Exact prefix match on name
        if (nameLower.startsWith(q)) return 800

        // Split query into words
        val words = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 1

        val cleanHay = hay.replace(Regex("[^a-z0-9]"), "")

        // Check if all words match the item
        var allWordsMatch = true
        for (w in words) {
            val isMatched = hay.contains(w) ||
                    (w.endsWith("l") && (hay.contains("${w}tr") || hay.contains("${w}iter"))) ||
                    (w.endsWith("ltr") && (hay.contains(w.removeSuffix("tr")) || hay.contains(w.removeSuffix("ltr") + "l"))) ||
                    (Regex("^([a-z]+)(\\d+)$").matches(w) && hay.contains(w.replace(Regex("([a-z]+)(\\d+)"), "$1 $2"))) ||
                    (cleanHay.contains(w.replace(Regex("[^a-z0-9]"), "")))

            if (!isMatched) {
                allWordsMatch = false
                break
            }
        }

        if (allWordsMatch) {
            val firstWord = words.first()
            val bonus = if (nameLower.startsWith(firstWord) || nameLower.contains(firstWord)) 300 else 100
            return 500 + bonus - (item.name.length.coerceAtMost(40))
        }

        // Clean query matching (ignoring punctuation/spaces) e.g. "ab6" matches "AB 6" or "ab 6" matches "AB6"
        val cleanQ = q.replace(Regex("[^a-z0-9]"), "")
        if (cleanQ.isNotEmpty() && cleanHay.contains(cleanQ)) {
            val bonus = if (nameLower.replace(Regex("[^a-z0-9]"), "").startsWith(cleanQ)) 200 else 50
            return 300 + bonus
        }

        // Single word fallback matching
        if (words.size == 1) {
            val single = words.first()
            if (hay.contains(single)) return 200
        }

        return 0
    }
}
