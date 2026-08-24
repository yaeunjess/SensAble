package com.sensable.app.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Region
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the app-owned braille surface and the user-enabled accessibility service.
 * Screen coordinates are copied because Region is mutable.
 */
object BraillePassthroughController {
    private val lock = Any()
    private var service: BrailleTouchAccessibilityService? = null
    private var requestedRegion: Region? = null

    private val _isPassthroughActive = MutableStateFlow(false)
    val isPassthroughActive: StateFlow<Boolean> = _isPassthroughActive.asStateFlow()

    fun isServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(context, BrailleTouchAccessibilityService::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { serviceInfo ->
                val actual = serviceInfo.resolveInfo.serviceInfo.let {
                    ComponentName(it.packageName, it.name)
                }
                actual == expected
            }
    }

    fun setBrailleRegion(region: Region) {
        val regionCopy = Region(region)
        synchronized(lock) {
            requestedRegion = regionCopy
            _isPassthroughActive.value = service?.activatePassthrough(regionCopy) == true
        }
    }

    fun clearBrailleRegion() {
        synchronized(lock) {
            requestedRegion = null
            service?.deactivatePassthrough()
            _isPassthroughActive.value = false
        }
    }

    internal fun attach(service: BrailleTouchAccessibilityService) {
        synchronized(lock) {
            this.service = service
            _isPassthroughActive.value = requestedRegion?.let(service::activatePassthrough) == true
        }
    }

    internal fun detach(service: BrailleTouchAccessibilityService) {
        synchronized(lock) {
            if (this.service === service) {
                this.service = null
                _isPassthroughActive.value = false
            }
        }
    }
}
