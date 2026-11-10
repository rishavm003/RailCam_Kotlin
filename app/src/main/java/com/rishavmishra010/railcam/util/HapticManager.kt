package com.rishavmishra010.railcam.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Centralised haptic feedback helper for RailCam.
 *
 * Two distinct effects are provided:
 *  - [levelSnap]  — 20 ms, default amplitude. Fired once when the phone reaches
 *                   perfect level (|tilt| ≤ 0.5°) so the engineer can level by feel alone.
 *  - [focusLock]  — 40 ms, amplitude 80.  Fired when the tap-to-focus ring appears,
 *                   confirming AF + AE + AWB have been commanded to lock on the tapped point.
 *
 * The VIBRATE permission is already declared in AndroidManifest.xml.
 */
object HapticManager {

    /** Short, crisp "snap" felt when the phone is perfectly level. */
    fun levelSnap(context: Context) = vibrate(context, durationMs = 35, amplitude = 120)

    /** Slightly longer tick confirming focus + exposure lock on the tapped rail area. */
    fun focusLock(context: Context) = vibrate(context, durationMs = 50, amplitude = 160)

    // -------------------------------------------------------------------------

    @android.annotation.SuppressLint("MissingPermission")
    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = manager?.defaultVibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                    return
                }
            }

            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
