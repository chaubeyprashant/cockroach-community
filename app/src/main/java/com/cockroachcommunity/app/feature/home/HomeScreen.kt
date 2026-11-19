package com.cockroachcommunity.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cockroachcommunity.app.core.designsystem.components.CjpButton
import com.cockroachcommunity.app.core.designsystem.components.CjpOutlinedButton
import com.cockroachcommunity.app.core.designsystem.components.SectionTitle
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.Gold
import com.cockroachcommunity.app.core.designsystem.theme.Ink
import com.cockroachcommunity.app.core.designsystem.theme.Paper
import com.cockroachcommunity.app.core.designsystem.theme.PaperSoft
import com.cockroachcommunity.app.data.content.Campaign
import com.cockroachcommunity.app.data.content.CjpDemands
import com.cockroachcommunity.app.data.content.CjpVision
import com.cockroachcommunity.app.data.content.HomeFaqs

@Composable
fun HomeScreen(onJoinProtest: () -> Unit, onReportPolice: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Protest campaign banner
        Column(
            Modifier.fillMaxWidth().background(Gold).padding(Dimens.Space16),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
        ) {
            Text(Campaign.HEADLINE, style = MaterialTheme.typography.headlineSmall, color = Ink)
            Text("${Campaign.VENUE} · ${Campaign.TIMING}. Stand for CJP’s five demands.", color = Ink, style = MaterialTheme.typography.bodyMedium)
            CjpButton("Register now", onClick = onJoinProtest, modifier = Modifier.fillMaxWidth())
        }

        // Police brutality banner
        Column(
            Modifier.fillMaxWidth().background(Ink).padding(Dimens.Space16),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
        ) {
            Text("REPORT POLICE BRUTALITY", style = MaterialTheme.typography.headlineSmall, color = Gold)
            Text(
                "Beaten, molested, or mistreated by police? File a complaint with photo/video proof — plus your name, phone and email for private follow-up.",
                color = Paper, style = MaterialTheme.typography.bodyMedium
            )
            Text("Your contact stays private. We verify and escalate with evidence.", color = Gold, style = MaterialTheme.typography.bodySmall)
            CjpButton("Submit proof", onClick = onReportPolice, gold = true, modifier = Modifier.fillMaxWidth())
        }

        // Hero
        Column(Modifier.fillMaxWidth().padding(Dimens.Space24), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            Text("THE SYSTEM\nIS BROKEN.", style = MaterialTheme.typography.displayLarge)
            Text("JOIN THE SWARM.", style = MaterialTheme.typography.displayMedium, color = Gold)
            Text("Cockroach Janta Party · Jantar Mantar · Main Bhi Cockroach", style = MaterialTheme.typography.titleSmall)
            Text(
                "For the overqualified, underemployed, and politically frustrated youth of India. Join the protest, stand for CJP’s five demands, and report police brutality with proof. Main bhi Cockroach.",
                style = MaterialTheme.typography.bodyLarge
            )
            CjpButton("Join the protest →", onClick = onJoinProtest, gold = true, modifier = Modifier.fillMaxWidth())
        }

        // Vision
        Column(Modifier.fillMaxWidth().background(Ink).padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space16)) {
            Text("THE VISION", style = MaterialTheme.typography.headlineMedium, color = Gold)
            CjpVision.forEach { card ->
                Surface(color = Color.Transparent, border = BorderStroke(1.dp, Paper.copy(alpha = 0.4f)), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        Text(card.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = Gold)
                        Text(card.text, style = MaterialTheme.typography.bodyMedium, color = Paper)
                    }
                }
            }
        }

        // Manifesto
        Column(Modifier.fillMaxWidth().padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space16)) {
            SectionTitle("The Manifesto")
            Text("Five demands. Non-negotiable. Same as CJP.", style = MaterialTheme.typography.bodyMedium)
            CjpDemands.forEach { demand ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space16), verticalAlignment = Alignment.Top) {
                    Text(demand.number, style = MaterialTheme.typography.displayMedium, color = Gold)
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
                        Text(demand.title.uppercase(), style = MaterialTheme.typography.titleMedium)
                        Text(demand.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // FAQ
        Column(Modifier.fillMaxWidth().background(PaperSoft).padding(Dimens.Space16), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            SectionTitle("Frequently Asked")
            HomeFaqs.forEach { faq ->
                var open by remember { mutableStateOf(false) }
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().clickable { open = !open }
                ) {
                    Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        Text((if (open) "− " else "+ ") + faq.question, style = MaterialTheme.typography.titleSmall)
                        if (open) Text(faq.answer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            CjpOutlinedButton("Report police brutality →", onClick = onReportPolice, modifier = Modifier.fillMaxWidth())
        }

        Text(
            "Cockroach Janta Party is a satirical civic movement. Participation is voluntary.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(Dimens.Space16)
        )
    }
}
