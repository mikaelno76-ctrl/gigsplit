package se.gigsplit.app

import kotlin.math.roundToInt

data class GigOffer(
    val sourceApp: String,
    val priceSek: Double,
    val totalMinutes: Double,
    val totalKm: Double,
    val krPerMin: Double,
    val krPerKm: Double,
    val krPerHour: Int,
    val ratingColor: String
)

object OfferParser {

    private val priceRegex = Regex("(?:(\\d+[.,]?\\d*)\\s*(?:kr|SEK))|(?:(?:kr|SEK)\\s*(\\d+[.,]?\\d*))", RegexOption.IGNORE_CASE)
    private val minRegex = Regex("(\\d+)\\s*(?:min|minuter)", RegexOption.IGNORE_CASE)
    private val kmRegex = Regex("(\\d+[.,]?\\d*)\\s*km", RegexOption.IGNORE_CASE)

    fun parseScreenNodes(packageName: String, texts: List<String>, minGoodKrPerMin: Double = 7.5, minOkKrPerMin: Double = 5.5): GigOffer? {
        val fullText = texts.joinToString(" | ")

        val prices = priceRegex.findAll(fullText).mapNotNull { match ->
            val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
            raw.replace(",", ".").toDoubleOrNull()
        }.filter { it in 25.0..5000.0 }.toList()

        if (prices.isEmpty()) return null
        val price = prices.maxOrNull() ?: return null

        val minutesList = minRegex.findAll(fullText).mapNotNull {
            it.groupValues[1].toDoubleOrNull()
        }.filter { it in 1.0..300.0 }.toList()

        val kmList = kmRegex.findAll(fullText).mapNotNull {
            it.groupValues[1].replace(",", ".").toDoubleOrNull()
        }.filter { it in 0.1..500.0 }.toList()

        if (minutesList.isEmpty() && kmList.isEmpty()) return null

        val totalMin = if (minutesList.isNotEmpty()) minutesList.sum() else 0.0
        val totalKm = if (kmList.isNotEmpty()) kmList.sum() else 0.0

        val krPerMin = if (totalMin > 0) price / totalMin else 0.0
        val krPerKm = if (totalKm > 0) price / totalKm else 0.0
        val krPerHour = (krPerMin * 60.0).roundToInt()

        val color = when {
            krPerMin >= minGoodKrPerMin -> "GREEN"
            krPerMin >= minOkKrPerMin -> "YELLOW"
            else -> "RED"
        }

        val appName = when {
            packageName.contains("uber", true) -> "Uber"
            packageName.contains("mtakso", true) || packageName.contains("bolt", true) -> "Bolt"
            packageName.contains("foodora", true) || packageName.contains("wolt", true) -> "Matbud"
            else -> "GigApp"
        }

        return GigOffer(
            sourceApp = appName,
            priceSek = price,
            totalMinutes = totalMin,
            totalKm = totalKm,
            krPerMin = krPerMin,
            krPerKm = krPerKm,
            krPerHour = krPerHour,
            ratingColor = color
        )
    }
}
