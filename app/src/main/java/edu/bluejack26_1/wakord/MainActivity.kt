package edu.bluejack26_1.wakord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import edu.bluejack26_1.wakord.ui.navigation.WakApp
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WAKordTheme {
                WakApp()
            }
        }
    }
}
