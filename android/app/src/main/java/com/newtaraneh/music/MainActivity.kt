package com.newtaraneh.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.newtaraneh.music.ui.NewTaranehApp
import com.newtaraneh.music.ui.theme.NewTaranehTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewTaranehTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NewTaranehApp()
                }
            }
        }
    }
}
