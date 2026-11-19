package com.cockroachcommunity.app.data.content

object Campaign {
    const val EVENT_DATE = "2026-10-10"
    const val EVENT_DATE_LABEL = "ONGOING 24/7"
    const val VENUE = "Jantar Mantar, New Delhi"
    const val TIMING = "24/7 — continuous peaceful presence"
    const val HEADLINE = "JANTAR MANTAR: JOIN THE PROTEST"
    const val SUBHEAD =
        "Demand CEC accountability. Stand for CJP’s five non-negotiable demands. Join the protest at Jantar Mantar — in person or virtually."
    const val COLLECTION = "protest_registrations_oct10"
    const val CAMPAIGN_ID = "oct10-jantar-mantar"
}

data class Demand(val number: String, val title: String, val text: String)

val CjpDemands = listOf(
    Demand("1", "Jobs & Accountability", "Real data on unemployment. Real action — not rebrand slogans on ₹10 crore billboards funded by our taxes."),
    Demand("2", "Education Reform", "Full accountability on NEET paper leaks. Resign, Education Minister. Scrap CBSE rechecking fees immediately."),
    Demand("3", "Press Freedom", "A PM who faces real press. 12 years. Zero press conferences. Democracy means answering questions you didn’t write."),
    Demand("4", "Judicial Dignity", "Citizens are not cockroaches. Not parasites. Unemployment is not a character flaw. Dignity is constitutional."),
    Demand("5", "Transparency Now", "Where did PM CARES funds go? We ask loudly, in writing, repeatedly, with RTIs — until someone answers honestly.")
)

data class VisionCard(val title: String, val text: String)

val CjpVision = listOf(
    VisionCard("1. Reclaim the Insult", "They called us cockroaches. We embraced it. We are the resilient majority that survives every crisis, only to be ignored when the dust settles."),
    VisionCard("2. Political Satire", "When the reality of Indian politics is a joke, the only serious response is satire. We mock the system because crying hasn't worked."),
    VisionCard("3. Real Demands", "Behind the satire lies genuine anger and five non-negotiable demands that address the root rot in our political, judicial, and economic structures.")
)

data class Faq(val question: String, val answer: String)

val HomeFaqs = listOf(
    Faq("What is Cockroach Janta Party (CJP)?", "Cockroach Janta Party is a satirical people’s movement for India’s overqualified, underemployed youth. Five non-negotiable demands. Zero sponsors. Main Bhi Cockroach."),
    Faq("When and where is the CJP protest?", "${Campaign.EVENT_DATE_LABEL} at ${Campaign.VENUE}. Timing: ${Campaign.TIMING}. Register on the protests page for in-person or virtual participation."),
    Faq("What are CJP’s five demands?", CjpDemands.joinToString(" ") { "${it.number}. ${it.title}: ${it.text}" }),
    Faq("How do I report police brutality to CJP?", "Open the police complaint form, choose assault, harassment, or misbehavior, upload photo or video proof, and submit name, phone, email, and social media privately for follow-up."),
    Faq("Is the Jantar Mantar protest peaceful?", "Yes. CJP organises peaceful civic presence only. Follow local laws and police directions. Participation is voluntary.")
)
