package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CalculatorViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.NexoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 9 and older need runtime permission to write screenshots to Pictures/DCalc.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            val viewModel: CalculatorViewModel = viewModel()
            val settings by viewModel.userSettings.collectAsState()
            NexoraTheme(themeName = settings.themeColor) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
