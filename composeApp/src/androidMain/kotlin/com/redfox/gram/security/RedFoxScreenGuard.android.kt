package com.redfox.gram.security

import android.app.Activity
import android.view.WindowManager

actual fun applyScreenAntiScreenshot(enabled: Boolean) {
    runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val thread = activityThread.getMethod("currentActivityThread").invoke(null)
        val activitiesField = activityThread.getDeclaredField("mActivities").apply { isAccessible = true }
        val activities = activitiesField.get(thread) as? Map<*, *> ?: return
        for (record in activities.values) {
            if (record == null) continue
            val actField = record.javaClass.getDeclaredField("activity").apply { isAccessible = true }
            val activity = actField.get(record) as? Activity ?: continue
            activity.runOnUiThread {
                if (enabled) {
                    activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }
}
