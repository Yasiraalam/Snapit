package com.yasir.iustthread.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object NavigationUtils {
    
    /**
     * Detects if the device is using gesture navigation using multiple methods
     */
    @SuppressLint("InternalInsetResource")
    fun isGestureNavigation(context: Context): Boolean {
        return try {
            // Method 1: Check for Android 10+ gesture navigation setting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val gestureNavigationEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    "navigation_mode",
                    0
                )
                // 2 = gesture navigation, 0 = three-button navigation
                if (gestureNavigationEnabled == 2) {
                    return true
                }
            }
            
            // Method 2: Check navigation bar height
            val resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                val navigationBarHeight = resources.getDimensionPixelSize(resourceId)
                // Gesture navigation typically has a smaller or zero navigation bar height
                if (navigationBarHeight < 48) {
                    return true
                }
            }
            
            // Method 3: Check for Samsung One UI gesture navigation
            val samsungGestureEnabled = Settings.Secure.getInt(
                context.contentResolver,
                "navigationbar_hide_bar_enabled",
                0
            )
            if (samsungGestureEnabled == 1) {
                return true
            }
            
            // Method 4: Check for MIUI gesture navigation
            val miuiGestureEnabled = Settings.Secure.getInt(
                context.contentResolver,
                "force_fsg_nav_bar",
                0
            )
            if (miuiGestureEnabled == 1) {
                return true
            }
            
            // Default to three-button navigation for safety
            false
        } catch (e: Exception) {
            // If there's any error, assume three-button navigation for safety
            false
        }
    }
    
    /**
     * Returns appropriate bottom padding based on navigation type
     */
    fun getBottomPadding(context: Context): Int {
        return if (isGestureNavigation(context)) {
            0 // No extra padding for gesture navigation
        } else {
            48 // Add padding for three-button navigation (increased for better visibility)
        }
    }
    
    /**
     * Returns the navigation bar height if available
     */
    @SuppressLint("InternalInsetResource")
    fun getNavigationBarHeight(context: Context): Int {
        return try {
            val resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}

/**
 * Composable function to get bottom padding based on navigation type
 */
@Composable
fun rememberBottomPadding(): Int {
    val context = LocalContext.current
    return remember(context) {
        NavigationUtils.getBottomPadding(context)
    }
} 