package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ParkingScreen
import com.example.ui.ParkingViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val parkingViewModel: ParkingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ParkingScreen(viewModel = parkingViewModel)
            }
        }
    }

    // Live updates from neighbours only while the app is on screen (saves battery and traffic)
    override fun onStart() {
        super.onStart()
        parkingViewModel.setAppInForeground(true)
    }

    override fun onStop() {
        parkingViewModel.setAppInForeground(false)
        super.onStop()
    }
}
