package com.cockroachcommunity.app.data.articles

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class Article(
    val id: String,
    val title: String,
    val excerpt: String = "",
    val imageUrl: String = "",
    val content: String = "",
    val publishedAt: Date = Date(),
    val authorName: String = ""
)

private fun daysAgo(days: Int) = Date(System.currentTimeMillis() - days * 86_400_000L)

// Same two articles the website falls back to when the collection is empty
private val StaticArticles = listOf(
    Article(
        id = "vision-2026",
        title = "The Cockroach Vision: Surviving the Apocalypse of Corruption",
        excerpt = "Our founding manifesto detailing how we plan to eradicate corruption and rebuild our cities from the ground up, much like a cockroach survives a nuclear fallout.",
        imageUrl = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80",
        publishedAt = daysAgo(5),
        authorName = "Prashant Chaubey",
        content = """
Our political system is broken. It has been nuked by corruption, greed, and nepotism. But you know what survives a nuclear fallout? **Cockroaches.**

We are the Cockroach Janta Party. We are resilient, we are everywhere, and we refuse to die out. For decades, the elite have tried to stamp us out, treating the common citizen like pests. Well, it's time the pests took over the house.

### Our 3-Point Manifesto

1. **Radical Transparency**: Every rupee spent by the government will be tracked on a public blockchain ledger. If a road costs 50 lakhs, you will know exactly whose pocket every single rupee went into.
2. **Citizen Audits**: No project is passed until a random lottery of local citizens signs off on the quality. If the bridge collapses in a month, the contractor doesn't just lose their license—they lose their assets.
3. **Pest Control**: We will fumigate the political system. Term limits for all, no more dynastic politics, and a strict ban on politicians with serious criminal records holding office.

Join us. We are indestructible. We are the future.
        """.trimIndent()
    ),
    Article(
        id = "infrastructure-decay",
        title = "Why Our Roads Look Like The Moon (And How To Fix It)",
        excerpt = "An in-depth analysis of the current infrastructure decay, contractor mafias, and our 3-step plan to ensure roads last more than a single monsoon.",
        imageUrl = "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=1200&q=80",
        publishedAt = daysAgo(2),
        authorName = "Prashant Chaubey",
        content = """
Have you ever driven down the street and wondered if you suddenly joined the Apollo space program? The craters on our roads are not natural phenomena; they are the result of a deeply entrenched contractor mafia.

### The Problem

Every monsoon, the roads wash away. Not because water is magic, but because the tar used is mixed with inferior materials. The contractor saves money, the inspector gets a bribe, and you get a busted suspension.

### The Cockroach Solution

1. **Warranty Periods**: Any contractor building a road must provide a 5-year mandatory warranty. If a pothole appears within 5 years, they fix it out of their own pocket within 48 hours.
2. **Open Source Materials Testing**: We will establish independent, citizen-led testing labs. Before a bill is cleared, the core sample of the road must pass a live-streamed stress test.

We can't keep paying taxes for roads that wash away like chalk. It's time to build infrastructure that survives, just like us.
        """.trimIndent()
    )
)

@Singleton
class ArticlesRepository @Inject constructor(private val firestore: FirebaseFirestore) {

    suspend fun list(): List<Article> {
        val remote = runCatching {
            firestore.collection("articles").orderBy("publishedAt", Query.Direction.DESCENDING).get().await()
                .documents.mapNotNull { it.toArticle() }
        }.getOrDefault(emptyList())
        return remote.ifEmpty { StaticArticles }
    }

    suspend fun get(id: String): Article? {
        val remote = runCatching { firestore.collection("articles").document(id).get().await().toArticle() }.getOrNull()
        return remote ?: StaticArticles.firstOrNull { it.id == id }
    }

    private fun DocumentSnapshot.toArticle(): Article? {
        if (!exists()) return null
        val title = getString("title") ?: return null
        val published = when (val raw = get("publishedAt")) {
            is Timestamp -> raw.toDate()
            is String -> runCatching {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(raw.take(19))
            }.getOrNull() ?: Date()
            else -> Date()
        }
        return Article(
            id = id,
            title = title,
            excerpt = getString("excerpt").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
            content = getString("content").orEmpty(),
            publishedAt = published,
            authorName = getString("authorName").orEmpty()
        )
    }
}
