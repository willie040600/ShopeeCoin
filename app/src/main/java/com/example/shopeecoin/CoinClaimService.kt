package com.example.shopeecoin

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

@SuppressLint("AccessibilityPolicy")
class CoinClaimService : AccessibilityService() {

    private var lastClickAt = 0L
    private var noCoinSince = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { scan() }

    private var badge: TextView? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString()
        if (pkg == packageName) return
        if (pkg !in SHOPEE_PACKAGES) {
            hideBadge()
            return
        }
        // Throttle with a trailing scan so the final state after a burst of updates is always checked.
        if (!handler.hasCallbacks(scanRunnable)) handler.postDelayed(scanRunnable, SCAN_DELAY_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        hideBadge()
        super.onDestroy()
    }

    private fun showBadge() {
        if (badge != null) return
        val view = TextView(this).apply {
            text = "● 已偵測到直播"
            setTextColor(Color.WHITE)
            textSize = 12f
            setBackgroundColor(Color.argb(180, 0, 150, 0))
            setPadding(24, 8, 24, 8)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }
        getSystemService(WindowManager::class.java).addView(view, params)
        badge = view
    }

    private fun hideBadge() {
        badge?.let { getSystemService(WindowManager::class.java).removeView(it) }
        badge = null
    }

    private fun scan() {
        val roots = windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(rootInActiveWindow) }
            .filter { it.packageName?.toString() in SHOPEE_PACKAGES }
        val inLiveRoom = isInLiveRoom(roots)
        if (inLiveRoom) showBadge() else hideBadge()
        if (!inLiveRoom) {
            noCoinSince = 0L
            return
        }

        val sinceLastClick = SystemClock.uptimeMillis() - lastClickAt
        if (sinceLastClick < CLICK_INTERVAL_MS) {
            handler.postDelayed(scanRunnable, CLICK_INTERVAL_MS - sinceLastClick)
            return
        }

        val target = roots.firstNotNullOfOrNull(::findClaimButton)
        if (target == null) {
            maybeSwipeUp(roots)
            return
        }
        noCoinSince = 0L

        val label = labelOf(target)
        if (clickNode(target)) {
            lastClickAt = SystemClock.uptimeMillis()
            Log.i(TAG, "Clicked: $label")
            Toast.makeText(this, "已自動點擊：$label", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findClaimButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        collectMatches(root, matches)
        return KEYWORDS.firstNotNullOfOrNull { kw -> matches.firstOrNull { labelOf(it) == kw } }
    }

    private fun inScanRegion(node: AccessibilityNodeInfo, region: RectF = SCAN_REGION): Boolean {
        val m = resources.displayMetrics
        val b = Rect().also(node::getBoundsInScreen)
        return region.contains(
            b.exactCenterX() / m.widthPixels,
            b.exactCenterY() / m.heightPixels
        )
    }

    private fun anyNode(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (predicate(node)) return true
        for (i in 0 until node.childCount) {
            if (node.getChild(i)?.let { anyNode(it, predicate) } == true) return true
        }
        return false
    }

    private fun isInLiveRoom(roots: List<AccessibilityNodeInfo>): Boolean =
        roots.any { root ->
            anyNode(root) {
                it.isVisibleToUser && labelOf(it)?.contains(LIVE_ROOM_LABEL) == true && inScanRegion(it, LIVE_ROOM_REGION)
            }
        }

    private fun maybeSwipeUp(roots: List<AccessibilityNodeInfo>) {
        val hasLiveCoin = roots.any { root ->
            anyNode(root) { it.isVisibleToUser && labelOf(it)?.contains(LIVE_COIN_LABEL) == true && inScanRegion(it) }
        }
        if (!isInLiveRoom(roots) || hasLiveCoin) {
            noCoinSince = 0L
            return
        }

        val now = SystemClock.uptimeMillis()
        if (noCoinSince == 0L) noCoinSince = now
        val remaining = NO_COIN_WAIT_MS - (now - noCoinSince)
        if (remaining > 0) {
            // The screen may stay static, so schedule a recheck instead of waiting for an event.
            handler.postDelayed(scanRunnable, remaining)
            return
        }

        noCoinSince = 0L
        Log.i(TAG, "No live coin widget for ${NO_COIN_WAIT_MS}ms, swiping up")
        swipeUp()
    }

    private fun swipeUp() {
        val m = resources.displayMetrics
        val x = m.widthPixels * 0.5f
        val path = Path().apply {
            moveTo(x, m.heightPixels * 0.85f)
            lineTo(x, m.heightPixels * 0.15f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
            .build()
        dispatchGesture(gesture, null, null)
    }


    private fun collectMatches(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isVisibleToUser && node.isEnabled && labelOf(node) in KEYWORDS && inScanRegion(node)) out += node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectMatches(it, out) }
        }
    }

    private fun labelOf(node: AccessibilityNodeInfo): String? =
        (node.text?.takeIf { it.isNotBlank() } ?: node.contentDescription)
            ?.toString()?.filterNot(Char::isWhitespace)

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = current.parent
        }
        // WebView / custom-drawn buttons often aren't marked clickable; fall back to a tap gesture.
        val bounds = Rect().also(node::getBoundsInScreen)
        if (bounds.isEmpty) return false
        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val TAG = "CoinClaimService"
        private const val CLICK_INTERVAL_MS = 5_000L
        private const val SCAN_DELAY_MS = 1000L
        private const val NO_COIN_WAIT_MS = 3_000L
        private val SCAN_REGION = RectF(0.64f, 0.27f, 1.0f, 0.5f)
        private val LIVE_ROOM_REGION = RectF(0.64f, 0.0f, 1.0f, 0.25f)
        private const val LIVE_ROOM_LABEL = "看更多"
        private const val LIVE_COIN_LABEL = "直播間蝦幣"

        val SHOPEE_PACKAGES = setOf("com.shopee.tw")

        // Ordered from most to least specific.
        private val KEYWORDS = listOf(
            "領取",
        )
    }
}
