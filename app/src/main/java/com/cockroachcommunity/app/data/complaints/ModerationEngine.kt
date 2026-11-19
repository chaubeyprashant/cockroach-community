package com.cockroachcommunity.app.data.complaints

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/** Kotlin port of the website's lib/moderation.js. Advisory only: it flags, never blocks. */
@Singleton
class ModerationEngine @Inject constructor() {

    private val abusiveTerms = listOf("idiot", "stupid", "hate", "kill", "bloody", "moron", "scamster", "harami", "madarchod", "bhosdike", "chutiya")
    private val fraudTerms = listOf("send money", "upi id", "paytm", "whatsapp me", "guaranteed job", "lottery", "crypto giveaway", "click here", "free money")
    private val spamPatterns = listOf(
        Regex("http[s]?://", RegexOption.IGNORE_CASE),
        Regex("free money", RegexOption.IGNORE_CASE),
        Regex("click here", RegexOption.IGNORE_CASE),
        Regex("(.)\\1{6,}"),
        Regex("\\b\\d{10}\\b"),
        Regex("@[a-z0-9._%+-]+\\.[a-z]{2,}", RegexOption.IGNORE_CASE)
    )
    private val categoryKeywords = mapOf(
        "Corruption" to listOf("corruption", "rishwat", "bribe", "ghoos", "kickback", "भ्रष्टाचार"),
        "Bribery" to listOf("bribe", "ghoos", "cash demand", "रिश्वत"),
        "Potholes" to listOf("pothole", "khadda", "road pit", "गड्ढा"),
        "Road Damage" to listOf("road crack", "road damage", "broken road", "damaged road"),
        "Water Leakage" to listOf("water leak", "pipeline leak", "sewage leak", "पानी लीक"),
        "Garbage Issue" to listOf("garbage", "trash", "kuda", "waste dump", "कचरा"),
        "Construction Defect" to listOf("construction defect", "poor material", "collapsed wall"),
        "Government Negligence" to listOf("negligence", "official ignored", "no action"),
        "Public Misbehavior" to listOf("harassment", "public abuse", "fight"),
        "Police Misconduct" to listOf("police misconduct", "extortion", "custody abuse", "पुलिस"),
        "Police Assault / Beating" to listOf("beaten", "beating", "lathi", "assault", "थप्पड़", "मारपीट", "police beat"),
        "Police Sexual Harassment" to listOf("molest", "harass", "sexual", "touch", "छेडछाड़", "दुष्कर्म"),
        "Police Misbehavior" to listOf("misbehave", "rude police", "abusive police", "गाली", "बदतमीजी"),
        "Electricity Issue" to listOf("power cut", "electricity", "voltage", "transformer"),
        "Internet/Utility Issue" to listOf("internet down", "fiber cut", "utility failure")
    )

    private fun countMatches(text: String, items: List<String>) = items.count { text.contains(it) }

    private fun tokenize(text: String): Set<String> =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").split(Regex("\\s+")).filter { it.length > 2 }.toSet()

    fun similarity(a: String, b: String): Double {
        val left = tokenize(a)
        val right = tokenize(b)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val overlap = left.count { it in right }
        return overlap.toDouble() / maxOf(left.size, right.size)
    }

    private fun suggest(issueType: String, title: String, description: String): Pair<String, Double> {
        val source = "$title $description".lowercase()
        var best = issueType
        var bestScore = 0
        categoryKeywords.forEach { (type, words) ->
            val score = countMatches(source, words)
            if (score > bestScore) {
                best = type
                bestScore = score
            }
        }
        return best to min(0.95, bestScore * 0.22)
    }

    fun moderate(draft: ComplaintDraft, existing: List<Complaint>): Map<String, Any?> {
        val title = draft.title.trim().lowercase()
        val description = draft.description.trim().lowercase()
        val text = "$title $description".trim()
        val city = draft.city.lowercase()

        val abusive = countMatches(text, abusiveTerms)
        val fraud = countMatches(text, fraudTerms)
        val spamHits = spamPatterns.count { it.containsMatchIn(text) }

        var bestScore = 0.0
        var duplicateOf: String? = null
        existing.forEach { row ->
            if (city.isNotEmpty() && row.city.lowercase() != city) return@forEach
            val score = similarity(text, "${row.title} ${row.description}")
            if (score > bestScore) {
                bestScore = score
                duplicateOf = row.referenceId.ifBlank { row.id }
            }
        }
        val duplicate = bestScore >= 0.55
        val tooShort = title.length < 5 || description.length < 20
        val (suggested, confidence) = suggest(draft.issueType, draft.title, draft.description)
        val risk = min(
            1.0,
            abusive * 0.25 + fraud * 0.3 + spamHits * 0.2 + (if (duplicate) 0.35 else 0.0) + (if (tooShort) 0.15 else 0.0)
        )
        val flags = buildList {
            if (abusive > 0) add("abusive_language")
            if (fraud > 0) add("possible_fraud")
            if (spamHits > 1) add("spam_patterns")
            if (duplicate) add("likely_duplicate")
            if (tooShort) add("thin_content")
        }
        fun round2(v: Double) = Math.round(v * 100) / 100.0
        return mapOf(
            "abusiveCount" to abusive,
            "fraudCount" to fraud,
            "spamScore" to min(1.0, spamHits * 0.3),
            "duplicate" to duplicate,
            "duplicateScore" to round2(bestScore),
            "duplicateOf" to duplicateOf,
            "suggestedIssueType" to suggested,
            "confidence" to confidence,
            "riskScore" to round2(risk),
            "flags" to flags,
            "needsReview" to (flags.isNotEmpty() || risk >= 0.35)
        )
    }
}
