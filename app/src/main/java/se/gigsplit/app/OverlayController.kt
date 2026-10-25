package se.gigsplit.app

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

object OverlayController {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    fun showOfferBanner(context: Context, offer: GigOffer) {
        if (!Settings.canDrawOverlays(context)) return

        handler.post {
            removeBannerImmediate()

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(36, 24, 36, 24)

                val bg = GradientDrawable().apply {
                    cornerRadius = 32f
                    val bgColor = when (offer.ratingColor) {
                        "GREEN" -> Color.parseColor("#E6065F46")
                        "YELLOW" -> Color.parseColor("#E678350F")
                        else -> Color.parseColor("#E67F1D1D")
                    }
                    setColor(bgColor)
                    setStroke(4, Color.WHITE)
                }
                background = bg
            }

            val titleView = TextView(context).apply {
                val badge = when (offer.ratingColor) {
                    "GREEN" -> "🟢 TA DEN!"
                    "YELLOW" -> "🟡 GRÄNSFALL"
                    else -> "🔴 DÅLIGT BETALT"
                }
                text = String.format(Locale.US, "%s • %.2f kr/min (%d kr/h)", badge, offer.krPerMin, offer.krPerHour)
                setTextColor(Color.WHITE)
                textSize = 18f
                paint.isFakeBoldText = true
            }

            val subView = TextView(context).apply {
                text = String.format(
                    Locale.US,
                    "%s: %.0f kr | Totalt %.0f min & %.1f km (%.1f kr/km)",
                    offer.sourceApp, offer.priceSek, offer.totalMinutes, offer.totalKm, offer.krPerKm
                )
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 13f
            }

            layout.addView(titleView)
            layout.addView(subView)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 140
            }

            layout.setOnTouchListener(object : View.OnTouchListener {
                private var initialY = 0
                private var touchY = 0f
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialY = params.y
                            touchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.y = initialY + (event.rawY - touchY).toInt()
                            wm.updateViewLayout(layout, params)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (kotlin.math.abs(event.rawY - touchY) < 15) {
                                removeBannerImmediate()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            wm.addView(layout, params)
            overlayView = layout

            hideRunnable = Runnable { removeBannerImmediate() }
            handler.postDelayed(hideRunnable!!, 12000)
        }
    }

    private fun removeBannerImmediate() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}
