package hu.bme.aut.android.nativecppdemo

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.emotiv.sdk.edkJavaJNI
import hu.bme.aut.android.nativecppdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }

    fun checkBluetooth() {
        val bluetoothAdapter = (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        binding.sampleText.text = edkJavaJNI.CustomerSecurity_emotiv_func(1.0).toString()
        connect()

    }

    private fun connect() {

        //TODO("Not yet implemented")
    }

    /**
     * A native method that is implemented by the 'nativecppdemo' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    external fun otherMethod(): String

    companion object {
        const val BT_CONNECTED = "Connected"
        // Used to load the 'nativecppdemo' library on application startup.
        init {
            System.loadLibrary("nativecppdemo")
        }
    }
}