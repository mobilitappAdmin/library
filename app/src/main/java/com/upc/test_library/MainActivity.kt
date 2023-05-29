package com.upc.test_library

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.upc.test_library.ui.theme.TestlibraryTheme
import java.text.SimpleDateFormat
import java.util.*
import com.upc.walkdetection.walkDetector


open class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Array<Double>? = null
    private var initDate: Date? = null
    private lateinit var walkDetector: walkDetector
    var capturing = mutableStateOf(false)
    var feedback = mutableStateOf("-")

    private val objReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, p1: Intent?) {

            val currDate = Date()
            val currDateStr = SimpleDateFormat("yyyy-LL-dd HH:mm:ss").format(currDate)
            val initDateStr = SimpleDateFormat("yyyy-LL-dd HH:mm:ss").format(initDate!!)
            val delay =  ((currDate.time - initDate!!.time)/1000)

            getLastLocation(fusedLocationClient, context!!)

            val elat = currentLocation?.get(0)!!.toString()
            val elon = currentLocation?.get(1)!!.toString()

            capturing.value = false

            displayDialog("Walk detected: \n From: $initDateStr  \n To: $currDateStr \n Delay: $delay seconds \n Lat -> $elat , Lon -> $elon.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        walkDetector = walkDetector(this)

        val intentFilter: IntentFilter = IntentFilter("walkDetected")
        registerReceiver(objReciever, intentFilter)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation(fusedLocationClient, this)

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


    @Composable
    fun Test_button(c: Context) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    capturing.value = true
                    initDate = Date()
                    walkDetector.beginDetection()
                    Log.d("Library", walkDetector.getState().toString())
                },
                enabled=!capturing.value
            ) {
                Text("Start Activity")
            }

        }
        Spacer(modifier = Modifier.height(300.dp))
        if (capturing.value) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Activity...")

                Button(
                    onClick = {
                        feedback.value = walkDetector.getFeedback()
                    },
                    enabled=true
                ) {
                    Text("Get feedback")
                }

                Text(text = feedback.value)
            }



        }
    }

    private fun getLastLocation(fusedLocationClient: FusedLocationProviderClient, context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101);
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener(context as Activity) { location ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val endLocx = location.longitude
                    val endLocy = location.latitude
                    currentLocation = arrayOf(endLocy, endLocx)
                }
            }
            .addOnFailureListener(context) {
                Toast.makeText(this, "GPS failure.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayDialog(text: String){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text).setCancelable(
            false
        ).setPositiveButton(
            "Accept"
        ) { dialog, id -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

}