package nz.co.warehouseandroidtest.logging

import android.util.Log

/**
 * Android-specific implementation of the shared logger.
 *
 * This class satisfies the common `expect` declaration by bridging to Android's
 * logging API. The shared module can call `AppLogger.debug()` and `AppLogger.error()`
 * without depending on Android classes directly in the common code.
 */
internal actual object AppLogger {
    actual fun debug(message: String) {
        try {
            // Use the Android logcat channel so messages are visible in debug builds
            // and device logs without exposing platform-specific APIs to common code.
            Log.d("WarehouseApp", message)
        } catch (_: Throwable) {
            // Android unit-test environments may not mock Log methods.
        }
    }

    actual fun error(message: String) {
        try {
            Log.e("WarehouseApp", message)
        } catch (_: Throwable) {
            // Android unit-test environments may not mock Log methods.
        }
    }
}
