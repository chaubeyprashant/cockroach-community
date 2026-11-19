package com.cockroachcommunity.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(date: Date): String = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)

/** **bold** spans, same as the website's mini-markdown. */
fun boldSpans(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}

/** Renders the subset the website supports: "### heading", numbered items, **bold**. */
@Composable
fun MiniMarkdown(content: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.lines().filter { it.isNotBlank() }.forEach { line ->
            when {
                line.startsWith("###") -> Text(
                    line.removePrefix("###").trim().uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
                line.matches(Regex("^\\d+\\..*")) -> Text(
                    boldSpans(line),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
                else -> Text(boldSpans(line), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
