package se.gigsplit.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GigAccessibilityService : AccessibilityService() {

    private var lastOfferSignature: String = ""
    private var lastOfferTimestamp: Long = 0L

    private val targetPackages = setOf(
        "com.ubercab.driver",
        "ee.mtakso.driver",
        "com.foodora.courier",
        "com.wolt.courierapp"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        val prefs = getSharedPreferences("gigsplit_prefs", Context.MODE_PRIVATE)
        val testEverywhere = prefs.getBoolean("test_everywhere", true)

        if (pkg == packageName) return
        if (!testEverywhere && !targetPackages.contains(pkg)) return

        val rootNode = rootInActiveWindow ?: return
        val extractedTexts = mutableListOf<String>()
        collectNodeTexts(rootNode, extractedTexts)

        if (extractedTexts.isEmpty()) return

        val greenLimit = prefs.getFloat("green_limit", 7.5f).toDouble()
        val yellowLimit = prefs.getFloat("yellow_limit", 5.5f).toDouble()

        val offer = OfferParser.parseScreenNodes(pkg, extractedTexts, greenLimit, yellowLimit) ?: return

        val sig = "${offer.priceSek}_${offer.totalMinutes}_${offer.totalKm}"
        val now = System.currentTimeMillis()
        if (sig == lastOfferSignature && (now - lastOfferTimestamp) < 8000) {
            return
        }

        lastOfferSignature = sig
        lastOfferTimestamp = now

        prefs.edit()
            .putFloat("last_driver_price", offer.priceSek.toFloat())
            .putFloat("last_driver_min", offer.totalMinutes.toFloat())
            .putFloat("last_driver_km", offer.totalKm.toFloat())
            .putString("last_source_app", offer.sourceApp)
            .apply()

        OverlayController.showOfferBanner(applicationContext, offer)
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { list.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { list.add(it) }
        for (i in 0 until node.childCount) {
            collectNodeTexts(node.getChild(i), list)
        }
    }

    override fun onInterrupt() {}
}
