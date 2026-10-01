package com.example.shopeecoin

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class CoinClaimService : AccessibilityService() {

    private var lastClickAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { scan() }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() !in SHOPEE_PACKAGES) return
        // Throttle with a trailing scan so the final state after a burst of updates is always checked.
        if (!handler.hasCallbacks(scanRunnable)) handler.postDelayed(scanRunnable, SCAN_DELAY_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        super.onDestroy()
    }

    private fun scan() {
        val sinceLastClick = SystemClock.uptimeMillis() - lastClickAt
        if (sinceLastClick < CLICK_INTERVAL_MS) {
            handler.postDelayed(scanRunnable, CLICK_INTERVAL_MS - sinceLastClick)
            return
        }

        val roots = windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(rootInActiveWindow) }
            .filter { it.packageName?.toString() in SHOPEE_PACKAGES }
        val target = roots.firstNotNullOfOrNull(::findClaimButton) ?: return

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

    private fun collectMatches(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        // Exact label match so "已領取" / "明天可領取" are skipped.
        if (node.isVisibleToUser && node.isEnabled && labelOf(node) in KEYWORDS) out += node
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

        val SHOPEE_PACKAGES = setOf("com.shopee.tw")

        // Ordered from most to least specific.
        private val KEYWORDS = listOf(
            "領取",
        )
    }
}
