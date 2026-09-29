// App entry point: hosts the Compose UI and switches between the main screens.
package com.example.geoseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.geoseek.screens.CollectionScreen
import com.example.geoseek.screens.HomeScreen
import com.example.geoseek.screens.HuntScreen
import com.example.geoseek.screens.ProfileScreen

enum class Screen { Home, Hunt, Collection, Profile }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GeoSeekApp()
            }
        }
    }
}

@Composable
fun GeoSeekApp() {
    var current by remember { mutableStateOf(Screen.Home) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            when (current) {
                Screen.Home -> HomeScreen()
                Screen.Hunt -> HuntScreen()
                Screen.Collection -> CollectionScreen()
                Screen.Profile -> ProfileScreen()
            }
        }
        // TODO: Replace with a proper bottom navigation bar
        Row {
            Screen.entries.forEach { screen ->
                TextButton(onClick = { current = screen }) { Text(screen.name) }
            }
        }
    }
}
