package nz.co.warehouseandroidtest.logging

import android.util.Log

internal actual object AppLogger {
    actual fun debug(message: String) {
        try {
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
