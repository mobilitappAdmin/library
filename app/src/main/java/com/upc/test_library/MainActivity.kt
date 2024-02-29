package com.upc.test_library

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.upc.test_library.ui.theme.TestlibraryTheme
import java.text.SimpleDateFormat
import java.util.*
import com.upc.mobilitapp.Mobilitapp


open class MainActivity : ComponentActivity() {

    private var ServiceState by mutableStateOf(false)
    private var feedback by mutableStateOf("-")

    /**
     * Check for notification permission before starting the service so that the notification is visible
     */
    private fun checkAndRequestNotificationPermission() {
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
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // if permission was denied, the service can still run only the notification won't be visible
        }


    private val objReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, p1: Intent?) {


        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        val intentFilter: IntentFilter = IntentFilter("Mobilitapp")
        registerReceiver(objReciever, intentFilter)

        setContent {
            TestlibraryTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    Test_button(this)
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun Test_button(c: Context) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    startForegroundService()
                },
                enabled=true
            ) {
                Text("Start Activity")
            }

        }
        Spacer(modifier = Modifier.height(300.dp))

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        // start the service
        val intent = Intent(this, Mobilitapp::class.java)
        this.startForegroundService(intent)
    }
}