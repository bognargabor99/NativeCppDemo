package hu.bme.aut.android.nativecppdemo

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.emotiv.bluetooth.EmotivBluetooth
import com.emotiv.sdk.edkJavaJNI
import hu.bme.aut.android.nativecppdemo.databinding.ActivityMainBinding
import hu.bme.aut.android.nativecppdemo.sensors.emotiv.EmotivEEG

class MainActivity : AppCompatActivity() {
    private lateinit var emotivEEG: EmotivEEG
    private lateinit var binding: ActivityMainBinding
    var emotivData: String = ""
        set(value) {
            field = value
            binding.sampleText.text = field
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = "I'm alive"

        binding.mainbutton.setOnClickListener {
            binding.mainbutton.text = "BT_CONNECTED"
            checkBluetooth()
        }

        emotivEEG = EmotivEEG(this)
    }

    fun checkBluetooth() {
        val bluetoothAdapter = (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        //binding.sampleText.text = edkJavaJNI.CustomerSecurity_emotiv_func(1.0).toString()
        connect()

    }

    private fun connect() {
        emotivEEG.connect()
    }

    companion object {
        const val BT_CONNECTED = "Connected"
        var emotivData: String = ""
        // Used to load the 'nativecppdemo' library on application startup.
        init {
            System.loadLibrary("nativecppdemo")
        }
    }
}