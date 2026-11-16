package com.lumian.morningup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.ui.MorningUpApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MorningTheme {
                MorningUpApp()
            }
        }
    }
}
