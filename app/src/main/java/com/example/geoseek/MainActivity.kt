// App entry point: hosts the Compose UI. Navigation and sign-in live in GeoSeekRoot.
package com.example.geoseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.geoseek.screens.GeoSeekRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GeoSeekRoot()
            }
        }
    }
}
