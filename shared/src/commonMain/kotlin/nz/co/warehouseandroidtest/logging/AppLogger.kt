package nz.co.warehouseandroidtest.logging

internal expect object AppLogger {
    fun debug(message: String)
    fun error(message: String)
}
