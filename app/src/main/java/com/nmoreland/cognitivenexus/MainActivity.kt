package com.nmoreland.cognitivenexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nmoreland.cognitivenexus.ui.CognitiveNexusApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CognitiveNexusApp() }
    }
}
