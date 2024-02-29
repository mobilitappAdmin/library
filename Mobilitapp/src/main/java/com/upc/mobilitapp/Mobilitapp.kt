package com.upc.mobilitapp

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.upc.mobilitapp.multimodal.MLService
import com.upc.mobilitapp.multimodal.StopService
import com.upc.mobilitapp.multimodal.UserInfo
import com.upc.mobilitapp.sensors.SensorLoader
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.cos
import kotlin.math.sqrt


class Mobilitapp(private val context: Context, private val sensorLoader: SensorLoader, private val notificationId: Int, private val notification: Notification, private val user_id: String): Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var mlService: MLService
    private lateinit var stopService: StopService
    private lateinit var  userInfoService: UserInfo

    private var locations = ArrayList<Location>()
    private var fifoAct: LinkedList<String> = LinkedList<String>()
    private var last_accuracies: LinkedList<Int> = LinkedList<Int>()

    private lateinit var startDate: Date
    private var startLoc: Location? = null
    private var first: Boolean = true
    private var lastwindow = "-"
    private var stop: Pair<Float, Boolean> = Pair(0.0f, false)
    private var macroState = "STILL"
    private var prevMacroState = "STILL"
    private var captureHash: Int = 0
    private var othersRow: Int = 0
    private var ml_calls: Int = 0
    private var last_distance: Double = 0.0

    private fun initialize() {
        first = true
        startDate = Date()
        captureHash = Math.abs((startDate.toString() + user_id).hashCode())
        stop = Pair(0.0f, false)
        // userInfoService = UserInfo(FILEPATH, captureHash.toString()+'_'+"UserInfo.csv")
        mlService =  MLService(this)
        mlService.initialize() //load Model
        stopService = StopService(alpha = 0.2, max_radium = 30, num_points = 90, covering_threshold = 75.0F)
        stopService.initialize()
        fifoAct = LinkedList<String>()
        last_accuracies = LinkedList<Int>()
        locations = ArrayList<Location>()
        last_distance = 0.0
        othersRow = 0
        ml_calls = 0
        macroState = "STILL"
        prevMacroState = "STILL"
        lastwindow = "-"
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startAsForegroundService()
        startLocationUpdates()

        return START_REDELIVER_INTENT
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     *
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    private fun startAsForegroundService() {
        // promote service to foreground service
        startForeground(notificationId, notification)
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     */
    fun stopForegroundService() {
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()

        Toast.makeText(this, "Foreground Service created", Toast.LENGTH_SHORT).show()

        setupLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()

        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorLoader.stopCapture()
        /*
        //push server
        if (!first) {
            userInfoService.createUserInfoDataFile(
                captureHash,
                preferences.getString("gender", null)!!,
                preferences.getString("age", null)!!,
                macroState,
                arrayOf(
                    startLoc!!.longitude.toString(),
                    startLoc!!.latitude.toString()
                ),
                arrayOf(
                    locations[locations.size - 1].longitude.toString(),
                    locations[locations.size - 1].latitude.toString()
                ),
                startDate.toString(),
                Date().toString()
            )
        }
         */

        Toast.makeText(this, "Foreground Service destroyed", Toast.LENGTH_SHORT).show()
    }

    private fun setupLocationUpdates() {
        initialize()

        val intent = Intent("Mobilitapp")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.locations[locationResult.locations.size - 1]
                val accuracy = location.accuracy

                if (location != null) {
                    if (startLoc == null) {
                        startLoc = location
                    }
                    locations.add(location)
                    var activity = sensorLoader.analyseLastWindow().toString()
                    if (activity != "-") {
                        activity = activity + "last distance: " + last_distance.toString()
                    }

                    if (activity.split(',')[0] == "MOVING") {
                        if (isStill(accuracy)) {
                            activity = "STILL," + activity.split(',')[1] //HERE
                        }
                    }
                    lastwindow = activity

                    fifoAct.add(activity.split(',')[0])
                    if (fifoAct.size > 5) {
                        fifoAct.removeFirst()
                    }
                    var fifoStr = ""
                    for (a in fifoAct){
                        if (a != "-") {
                            if (fifoStr == "") {
                                fifoStr = "$fifoStr$a"
                            } else {
                                fifoStr = "$fifoStr, $a"
                            }
                        }
                    }

                    if (othersRow != 0) {
                        if ((othersRow-1)%3 == 0 && (othersRow-1) != 0)  { // Call ML periodically
                            val (prediction, summary) =
                                mlService.overallPrediction(sensorLoader.getLastWindow(fifoAct.size, fifoAct))
                            macroState = prediction
                            ++ml_calls
                        }
                        ++othersRow
                    }


                    var majority = majorityState()
                    if (majority == null) {
                        majority = macroState
                    }
                    if (majority == "MOVING") {
                        if (othersRow == 0) {
                            val (prediction, summary) =
                                mlService.overallPrediction(sensorLoader.getLastWindow(3, fifoAct))
                            macroState = prediction

                            Log.d("ML", "call")

                            ++othersRow
                            ++ml_calls
                        }
                    }
                    else {
                        macroState = majority
                        othersRow = 0
                        ml_calls = 0
                    }
                    // STOP algorithm
                    if (accuracy < 100) {
                        stop = stopService.addLocation(location)
                    }

                    intent.putExtra("macroState", macroState)
                    intent.putExtra("fifo", fifoStr)
                    intent.putExtra("activity", activity)
                    intent.putExtra("accuracy", accuracy.toString())
                    intent.putExtra("location", location.latitude.toString()+","+location.longitude.toString())
                    intent.putExtra("stop", BigDecimal(stop.first.toDouble()).setScale(2, RoundingMode.HALF_EVEN).toString() + " %, " + stopService.get_size().toString() + ", " +
                            BigDecimal(stopService.distance_to_last_location()).setScale(2, RoundingMode.HALF_EVEN).toString() + " m, " +  BigDecimal(stopService.get_current_alpha()).setScale(3, RoundingMode.HALF_EVEN).toString())
                    intent.putExtra("ml", ml_calls.toString())
                    Log.d("MA lib", "send fifo $fifoStr")

                    context.sendBroadcast(intent)

                    if (stop.second) {
                        stopForegroundService()
                    }
                }
            }
        }
        locationRequest = LocationRequest.create()
        locationRequest.interval = (30 * 1000).toLong() // 18 seconds
        locationRequest.fastestInterval = (20 * 1000).toLong() // 16 seconds
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Starts the location updates using the FusedLocationProviderClient.
     */
    private fun startLocationUpdates() {
        // Start receiving location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        // Initialize sensor data capture
        sensorLoader.initialize("Multimodal")
    }

    /**
     * Checks if the device is still based on the recent recorded locations.
     * The device is considered still if the distance between the last two recorded locations is less than 20 meters.
     *
     * @return True if the device is still, false otherwise.
     */
    private fun isStill(accuracy: Float): Boolean {
        if (locations.size >= 2 && accuracy < 100){
            val loc0 = locations[locations.size-2]
            val loc1 = locations[locations.size-1]
            val dist = computeDistance(loc0.latitude, loc0.longitude, loc1.latitude, loc1.longitude)
            last_distance = dist

            return dist < 20
        }
        return false
    }

    /**
     * Computes the distance between two geographic coordinates using the Haversine formula.
     *
     * @param lat1 Latitude of the first coordinate.
     * @param lon1 Longitude of the first coordinate.
     * @param lat2 Latitude of the second coordinate.
     * @param lon2 Longitude of the second coordinate.
     * @return The distance between the two coordinates in meters.
     */
    private fun computeDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        var lat1 = lat1
        var lon1 = lon1
        var lat2 = lat2
        var lon2 = lon2
        val radius = 6371.0
        lat1 = lat1 * Math.PI / 180
        lat2 = lat2 * Math.PI / 180
        lon1 = lon1 * Math.PI / 180
        lon2 = lon2 * Math.PI / 180
        val deltaLat = lat1 - lat2
        val deltaLon = lon1 - lon2
        val x = deltaLon * cos((lat1 + lat2) / 2)
        return radius * sqrt(x * x + deltaLat * deltaLat) * 1000
    }

    private fun majorityState(): String? {
        if (fifoAct.size >= 3) {
            var majority: String? = null
            val activities = mutableMapOf(
                "STILL" to 0,
                "WALK" to 0,
                "MOVING" to 0
            )
            var counts = 0
            for (state in fifoAct) {
                if (state in activities.keys) {
                    counts++
                    activities[state] = activities[state]!! + 1
                }
            }
            if (counts >= 3) {
                majority = activities.maxBy { it.value }.key
            }
            if (majority != null){
                if ((majority == "MOVING" && activities["MOVING"]!! >= 3) || majority != "MOVING") {
                    if (majority == "STILL") {
                        return if (fifoAct[fifoAct.size - 1] == fifoAct[fifoAct.size - 2] && fifoAct[fifoAct.size - 2] == fifoAct[fifoAct.size - 3] && fifoAct[fifoAct.size - 1] == "STILL") {
                            majority
                        } else {
                            null
                        }
                    }
                    return majority
                }
            }
        }
        return null
    }

    /**
     * Returns the FIFO (First In, First Out) activity analysis window.
     *
     * @return The FIFO activity analysis window as a string.
     */
    private fun get_fifo(): String {
        if (fifoAct.size > 0) {
            var fifoStr = ""
            for (a in fifoAct) {
                if (a != "-") {
                    if (fifoStr == "") {
                        fifoStr = "$fifoStr$a"
                    } else {
                        fifoStr = "$fifoStr, $a"
                    }
                }
            }
            return fifoStr
        }
        else {
            return "-"
        }
    }

}