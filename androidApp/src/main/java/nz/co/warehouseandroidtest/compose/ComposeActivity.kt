package nz.co.warehouseandroidtest.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import nz.co.warehouseandroidtest.MainView

/**
 * Android entry point for the Compose-based app shell.
 *
 * This activity hosts the shared KMP Compose UI and wires it into the platform lifecycle.
 */
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainView()
        }
    }
}
