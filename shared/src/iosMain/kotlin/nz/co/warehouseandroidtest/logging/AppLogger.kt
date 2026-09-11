package nz.co.warehouseandroidtest.logging

import platform.Foundation.NSLog

internal actual object AppLogger {
    actual fun debug(message: String) {
        NSLog("[WarehouseApp] $message")
    }

    actual fun error(message: String) {
        NSLog("[WarehouseApp] ERROR: $message")
    }
}
