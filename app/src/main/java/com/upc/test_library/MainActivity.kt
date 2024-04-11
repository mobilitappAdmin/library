package com.upc.test_library

import MainViewModel
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.upc.mobilitapp.Mobilitapp
import com.upc.test_library.ui.theme.TestlibraryTheme
import java.util.*

val CHANNEL_ID = "Mobilitapp_default_channel"

open class MainActivity : ComponentActivity() {

    private fun requestPermissions() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                } else -> {
                // No location access granted.
            }
            }
        }

        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // if permission was denied, the service can still run only the notification won't be visible
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // permission already granted
                }

                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Check if the API level is 26 (Android 8.0) or higher, as NotificationChannel class is new to API 26.
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val name: String =CHANNEL_ID
            val importance =
                NotificationManager.IMPORTANCE_DEFAULT // This determines how to interrupt the user for any notification belonging to this channel.
            val channel = NotificationChannel(name, name, importance)
            // Register the channel with the system. You can't change the importance or other notification behaviors after this.
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestPermissions()

        // Instantiate your ViewModel
        val viewModel: MainViewModel by viewModels()

        setContent {
            TestlibraryTheme {
                Surface(color = MaterialTheme.colors.background) {
                    MyAppUI(viewModel)
                }
            }
        }
    }

}

@Composable
fun MyAppUI(viewModel: MainViewModel) {
    // This will observe dataState and recompose whenever dataState changes.
    val dataState = viewModel.dataState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(PaddingValues(16.dp))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(16.dp))
        ) {
            // Start service button
            Button(onClick = {
                // Start the foreground service
                context.startMobilitAppService()
            }) {
                Text("Start Service")
            }

            // Stop service button
            Button(onClick = {
                // Stop the foreground service
                context.stopMobilitAppService()
            }) {
                Text("Stop Service")
            }
            Text(text = dataState.value, modifier = Modifier.padding(8.dp))
        }

    }
}

// Extension functions to cleanly call service operations from Compose.
fun Context.startMobilitAppService() {
    val intent = Intent(this, Mobilitapp::class.java)
    //add info to the library
    intent.putExtra("userId", "3efds234r")
    intent.putExtra("NotificationTitle", "MobilitApp Service")
    intent.putExtra("NotificationDescription", "A multimodal trip is being captured using MobilitApp.")
    intent.putExtra("NotificationChannel", CHANNEL_ID)

    this.startForegroundService(intent)


}

fun Context.stopMobilitAppService() {
    stopService(Intent(this, Mobilitapp::class.java))
}