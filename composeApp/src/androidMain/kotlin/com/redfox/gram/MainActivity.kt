package com.redfox.gram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.redfox.gram.engine.RedFoxTdEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appFilesPath = filesDir.absolutePath
        RedFoxTdEngine.start(dbDirectory = "$appFilesPath/redfox_tdlib")
        setContent {
            App(androidFilesDir = appFilesPath)
        }
    }
}
