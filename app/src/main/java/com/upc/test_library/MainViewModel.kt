import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _dataState = MutableStateFlow("Waiting for data...")
    val dataState: StateFlow<String> = _dataState.asStateFlow()

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = StringBuilder().apply {
                append("MacroState: ${intent?.getStringExtra("macroState")}\n")
                append("MicroStates: ${intent?.getStringExtra("microStates")}\n")
                //append("Activity: ${intent?.getStringExtra("activity")}\n")
                append("Location Accuracy: ${intent?.getStringExtra("location accuracy")}\n")
                append("Location: ${intent?.getStringExtra("location")}\n")
                append("Stop Information: ${intent?.getStringExtra("stop information")}\n")
                append("ML Calls: ${intent?.getStringExtra("ml calls")}")
            }.toString()

            _dataState.value = data
        }
    }

    init {
        val filter = IntentFilter("Mobilitapp")
        application.registerReceiver(dataReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(dataReceiver)
    }
}

