package com.yourname.healthtrackerwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yourname.healthtrackerwear.data.MedicineRepository
import com.yourname.healthtrackerwear.presentation.HealthTrackerWearApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Populate the in-memory StateFlow from SharedPreferences so MedScreen
        // has the last-known list immediately on cold start, before the phone
        // has a chance to respond to the /med-list-request we'll send it.
        MedicineRepository.loadFromPrefs(this)
        setContent {
            HealthTrackerWearApp()
        }
    }
}
