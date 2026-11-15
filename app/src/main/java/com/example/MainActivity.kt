package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.LocalNotifier
import com.example.ui.FoodBridgeApp
import com.example.ui.theme.FoodBridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalNotifier.ensureChannel(this)
        enableEdgeToEdge()
        setContent {
            FoodBridgeTheme {
                FoodBridgeApp()
            }
        }
    }
}
