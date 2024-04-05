package com.upc.test_library

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
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.upc.mobilitapp.Mobilitapp
import com.upc.test_library.ui.theme.TestlibraryTheme
import java.util.*


open class MainActivity : ComponentActivity() {

    private var ServiceState by mutableStateOf(false)
    private var feedback by mutableStateOf("-")

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }


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

    private fun createNotificationChannel() {
        // Check if the API level is 26 (Android 8.0) or higher, as NotificationChannel class is new to API 26.
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val name: String ="Mobilitapp_notification_channel"
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

    private val objReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, p1: Intent?) {


        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //checkAndRequestNotificationPermission()
        createNotificationChannel()
        requestPermissions()

        setContent {
            TestlibraryTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    MyAppUI()
                }
            }
        }
    }

    private fun startMobilitAppService() {
        val intent = Intent(this, Mobilitapp::class.java)
        this.startForegroundService(intent)
        intent.putExtra("userId", "3efds234r")
    }

    private fun stopMobilitAppService() {
        stopService(Intent(this, Mobilitapp::class.java))
    }

}

@Composable
fun MyAppUI() {
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(PaddingValues(16.dp))) {
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
    }
}

// Extension functions to cleanly call service operations from Compose.
fun Context.startMobilitAppService() {
    val intent = Intent(this, Mobilitapp::class.java)
    this.startForegroundService(intent)
    intent.putExtra("userId", "3efds234r")
}

fun Context.stopMobilitAppService() {
    stopService(Intent(this, Mobilitapp::class.java))
}