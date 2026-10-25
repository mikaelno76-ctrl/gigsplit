package se.gigsplit.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(AndroidBridge(this@MainActivity), "AndroidBridge")
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onResume() {
        super.onResume()
        webView.evaluateJavascript("if(window.refreshFromNative) window.refreshFromNative();", null)
    }

    inner class AndroidBridge(private val context: Context) {

        @JavascriptInterface
        fun hasOverlayPermission(): Boolean {
            return Settings.canDrawOverlays(context)
        }

        @JavascriptInterface
        fun openOverlaySettings() {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        @JavascriptInterface
        fun openAccessibilitySettings() {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        @JavascriptInterface
        fun getLastCapturedOfferJson(): String {
            val prefs = getSharedPreferences("gigsplit_prefs", Context.MODE_PRIVATE)
            val price = prefs.getFloat("last_driver_price", 175f)
            val min = prefs.getFloat("last_driver_min", 18f)
            val km = prefs.getFloat("last_driver_km", 9.5f)
            val app = prefs.getString("last_source_app", "Uber") ?: "Uber"
            return "{\"price\":$price,\"min\":$min,\"km\":$km,\"app\":\"$app\"}"
        }

        @JavascriptInterface
        fun testOverlayDemo() {
            val demoOffer = GigOffer(
                sourceApp = "Uber",
                priceSek = 185.0,
                totalMinutes = 19.0,
                totalKm = 11.2,
                krPerMin = 185.0 / 19.0,
                krPerKm = 185.0 / 11.2,
                krPerHour = 584,
                ratingColor = "GREEN"
            )
            OverlayController.showOfferBanner(context, demoOffer)
        }
    }
}
