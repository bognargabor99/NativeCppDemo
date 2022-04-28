package hu.bme.aut.android.nativecppdemo.sensors.emotiv

import android.Manifest.permission
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorEvent
import android.hardware.SensorPrivacyManager.Sensors
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Contacts
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import com.emotiv.SensorStatus
import com.emotiv.bluetooth.EmotivBluetooth
import com.emotiv.sdk.EdkErrorCode.EDK_OK
import com.emotiv.sdk.IEE_DataChannels_t.IED_AF3
import com.emotiv.sdk.IEE_DataChannels_t.IED_AF4
import com.emotiv.sdk.IEE_DataChannels_t.IED_Pz
import com.emotiv.sdk.IEE_DataChannels_t.IED_T7
import com.emotiv.sdk.IEE_DataChannels_t.IED_T8
import com.emotiv.sdk.IEE_Event_t.IEE_EmoStateUpdated
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_ENGAGEMENT
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_EXCITEMENT
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_FOCUS
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_INTEREST
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_RELAXATION
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_STRESS
import com.emotiv.sdk.edkJavaJNI.CustomerSecurity_emotiv_func
import com.emotiv.sdk.edkJavaJNI.IEE_CheckSecurityCode
import com.emotiv.sdk.edkJavaJNI.IEE_EmoEngineEventCreate
import com.emotiv.sdk.edkJavaJNI.IEE_EmoEngineEventFree
import com.emotiv.sdk.edkJavaJNI.IEE_EmoEngineEventGetEmoState
import com.emotiv.sdk.edkJavaJNI.IEE_EmoEngineEventGetType
import com.emotiv.sdk.edkJavaJNI.IEE_EmoEngineEventGetUserId
import com.emotiv.sdk.edkJavaJNI.IEE_EmoStateCreate
import com.emotiv.sdk.edkJavaJNI.IEE_EmoStateFree
import com.emotiv.sdk.edkJavaJNI.IEE_EngineConnect__SWIG_0
import com.emotiv.sdk.edkJavaJNI.IEE_EngineDisconnect
import com.emotiv.sdk.edkJavaJNI.IEE_EngineGetNextEvent
import com.emotiv.sdk.edkJavaJNI.IEE_GetAverageBandPowers
import com.emotiv.sdk.edkJavaJNI.IEE_GetSecurityCode
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetEngagementBoredomScore
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetInstantaneousExcitementScore
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetInterestScore
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetRelaxationScore
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetStressScore
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricIsActive
import hu.bme.aut.adapted.commonlib.util.GsonHelper.gson
import hu.bme.aut.adapted.framework.model.event.sensors.emotiv.PerformanceMetricUpdatedGameEvent
import hu.bme.aut.adapted.framework.service.event.FrameworkEvent
import hu.bme.aut.adapted.framework.util.RequestPermissionActivity
import hu.bme.aut.adapted.framework.util.ResultActivity
import hu.bme.aut.adapted.framework.util.ResultActivity.EXTRA_ACTION
import java.util.*
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetFocusScore as IS_PerformanceMetricGetFocusScore1

class EmotivEEG(private val context: Context) : IEmotivEEG {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val pmNames: Map<Int, String> = Collections.unmodifiableMap(createPmNames())
    private val pmGetters: Map<Int, PerformanceMetricGetter> = Collections.unmodifiableMap(createPmGetters())
    private val performanceMetrics: MutableMap<Int, Float>
    private val pmUpdateTimes: MutableMap<Int, Long>

    @Volatile
    private var status = SensorStatus.OFFLINE
    private var connectStartTime: Long = 0
    private var engineConnected = false
    private var pEvent: Long = 0
    private var pEmoState: Long = 0
    private val userID = 0

    @Volatile
    private var deviceConnected = false
    var channelList = intArrayOf(IED_AF3, IED_AF4, IED_T7, IED_T8, IED_Pz)
    var bands = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
    )

    private interface PerformanceMetricGetter {
        fun getScore(hEmoState: Long): Float
    }

    private fun checkBluetooth(): Boolean {
        val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val bluetoothEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled
        if (!bluetoothEnabled) {
            Toast.makeText(context, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
        }
        return bluetoothEnabled
    }

    override fun connect() {
        if (status != SensorStatus.OFFLINE) {
            return
        }
        status = SensorStatus.CONNECTING
        sendStatus()
        //bus.register(this)
        if (checkBluetooth()) {
            connectEngine()
        }
    }

    private fun connectEngine() {
        if (IEE_EngineConnect__SWIG_0(DEV_ID) != EDK_OK) {
            Log.e(TAG, "engine connect failed")
            disconnect()
            return
        }
        Log.d(TAG, "engine connect ok")
        if (IEE_CheckSecurityCode(CustomerSecurity_emotiv_func(IEE_GetSecurityCode())) != EDK_OK) {
            Log.e(TAG, "security code check failed")
            disconnect()
            return
        }
        Log.d(TAG, "security code check ok")
        engineConnected = true
        pEvent = IEE_EmoEngineEventCreate()
        pEmoState = IEE_EmoStateCreate()
        if (EmotivBluetooth._emobluetooth == null) {
            EmotivBluetooth._emobluetooth = EmotivBluetooth(context)
        }
        connectStartTime = SystemClock.uptimeMillis()
        backgroundHandler!!.post { connectDevice() }
    }

    private fun connectDevice() {
        if (status !== SensorStatus.CONNECTING) {
            return
        }
        val deviceCount = EmotivBluetooth._emobluetooth!!.GetNumberDeviceInsight()
        if (deviceCount == 0 ||
            !EmotivBluetooth._emobluetooth!!.EmoConnectDevice(0, deviceCount - 1)
        ) {
            if (SystemClock.uptimeMillis() - connectStartTime < CONNECT_TIMEOUT) {
                backgroundHandler!!.postDelayed({ connectDevice() }, CONNECT_PERIOD)
            } else {
                Log.e(TAG, "device connect failed")
                handler.post { disconnect() }
            }
            return
        }
        Log.d(TAG, "device connect ok")
        deviceConnected = true
        status = SensorStatus.ONLINE
        sendStatus()
        handleEvents()
    }

    private fun handleEvents() {
        if (status !== SensorStatus.ONLINE && status !== SensorStatus.BAD_SIGNAL) {
            return
        }
        val timestamp = System.currentTimeMillis()
        while (IEE_EngineGetNextEvent(pEvent) == EDK_OK) {
            val type: Int = IEE_EmoEngineEventGetType(pEvent)
            IEE_EmoEngineEventGetUserId(pEvent, userID)
            var alpha: Double
            var low_beta: Double
            var high_beta: Double
            var gamma: Double
            var theta: Double
            theta = 0.0
            gamma = theta
            high_beta = gamma
            low_beta = high_beta
            alpha = low_beta
            Log.d("Insight", type.toString())
            if (type == IEE_EmoStateUpdated) {
                val getEmoStateResult: Int = IEE_EmoEngineEventGetEmoState(pEvent, pEmoState)
                if (getEmoStateResult == EDK_OK) {
                    for (key in pmGetters.keys) {
                        updatePerformanceMetric(key, timestamp)
                    }
                }
            }
            for (i in channelList.indices) {
                Log.d("Insight", type.toString())
                val bandPowers: Int = IEE_GetAverageBandPowers(
                    userID,
                    channelList[i], theta, alpha,
                    low_beta, high_beta, gamma
                )
                if (bandPowers == EDK_OK) {
                    bands[channelList[i]][0] = theta
                    bands[channelList[i]][1] = alpha
                    bands[channelList[i]][2] = low_beta
                    bands[channelList[i]][3] = high_beta
                    bands[channelList[i]][4] = gamma
                }
            }
        }
        var badSignal = false
        val namedMetrics: MutableMap<String?, Float?> = HashMap()
        for (key in pmGetters.keys) {
            namedMetrics[pmNames[key]] = performanceMetrics[key]
            val updateTime = if (pmUpdateTimes.containsKey(key)) pmUpdateTimes[key]!! else 0
            if (!badSignal && timestamp - updateTime > 0) {
                badSignal = true
            }
        }
        val event = PerformanceMetricUpdatedGameEvent(gson.toJson(namedMetrics))
        bus.post(FrameworkEvent(event, timestamp, false))

        /*PerformanceMetricUpdatedSvmEvent svmEvent = new PerformanceMetricUpdatedSvmEvent(namedMetrics);
        bus.post(new FrameworkEvent(svmEvent, timestamp, true));*/
        val prevStatus: Status = status
        status = if (badSignal) SensorStatus.BAD_SIGNAL else SensorStatus.ONLINE
        if (status !== prevStatus) {
            sendStatus()
        }
        backgroundHandler!!.postDelayed({ handleEvents() }, POLL_PERIOD)
    }

    private fun updatePerformanceMetric(key: Int, timestamp: Long) {
        if (IS_PerformanceMetricIsActive(pEmoState, key) == 1) {
            //Float oldScore = performanceMetrics.get(key);
            val newScore = pmGetters[key]!!.getScore(pEmoState)
            performanceMetrics[key] = newScore
            pmUpdateTimes[key] = timestamp

            /*
            if (oldScore == null || oldScore != newScore) {
                PerformanceMetricUpdatedGameEvent event =
                        new PerformanceMetricUpdatedGameEvent(pmNames.get(key), newScore);
                bus.post(new FrameworkEvent(event, timestamp, true));

                Log.d(TAG, event.getMetric() + ": " + event.getScore());
            }
            */
        }
    }

    override fun disconnect() {
        if (status == Contacts.PresenceColumns.OFFLINE) {
            return
        }
        status = Contacts.PresenceColumns.OFFLINE
        sendStatus()
        backgroundHandler!!.removeCallbacksAndMessages(null)
        if (deviceConnected) {
            EmotivBluetooth._emobluetooth!!.DisconnectHeadset()
            deviceConnected = false
        }
        if (pEvent != 0L) {
            IEE_EmoEngineEventFree(pEvent)
            pEvent = 0
        }
        if (pEmoState != 0L) {
            IEE_EmoStateFree(pEmoState)
            pEmoState = 0
        }
        if (engineConnected) {
            IEE_EngineDisconnect()
            engineConnected = false
        }
        //bus.unregister(this)
    }

    /*@Subscribe(threadMode = MAIN)
    fun onRequestPermissionEvent(event: RequestPermissionEvent) {
        if (event.getRequestCode() == PERMISSION_REQUEST_CODE) {
            if (event.hasGranted(permission.ACCESS_FINE_LOCATION)) {
                if (checkBluetooth()) {
                    connectEngine()
                }
            } else {
                disconnect()
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onActivityResultEvent(event: ActivityResultEvent) {
        if (event.getRequestCode() == BLUETOOTH_REQUEST_CODE) {
            if (event.getResultCode() == Activity.RESULT_OK) {
                connectEngine()
            } else {
                disconnect()
            }
        }
    }*/

    private fun sendStatus() {
        // TODO
    }

    companion object {
        private const val TAG = "EmotivEEG"
        private const val DEV_ID = "EmotivApp-android"
        private const val PERMISSION_REQUEST_CODE = 0
        private const val BLUETOOTH_REQUEST_CODE = 1
        private const val CONNECT_TIMEOUT = 10000L
        private const val CONNECT_PERIOD = 10L
        private const val POLL_PERIOD = 1000L
        private fun createPmNames(): Map<Int, String> {
            val pmNames: MutableMap<Int, String> = HashMap()
            pmNames[PM_EXCITEMENT] = "excitement"
            pmNames[PM_RELAXATION] = "relaxation"
            pmNames[PM_STRESS] = "stress"
            pmNames[PM_ENGAGEMENT] = "engagement"
            pmNames[PM_INTEREST] = "interest"
            pmNames[PM_FOCUS] = "focus"
            return pmNames
        }

        private fun createPmGetters(): Map<Int, PerformanceMetricGetter> {
            val pmGetters: MutableMap<Int, PerformanceMetricGetter> = HashMap()
            pmGetters[PM_EXCITEMENT] = object : PerformanceMetricGetter {
                    override fun getScore(hEmoState: Long): Float = IS_PerformanceMetricGetInstantaneousExcitementScore(hEmoState)
                }
            pmGetters[PM_RELAXATION] = object : PerformanceMetricGetter {
                    override fun getScore(hEmoState: Long): Float = IS_PerformanceMetricGetRelaxationScore(hEmoState)
                }
            pmGetters[PM_STRESS] = object : PerformanceMetricGetter {
                    override fun getScore(hEmoState: Long): Float = IS_PerformanceMetricGetStressScore(hEmoState)
                }
            pmGetters[PM_ENGAGEMENT] = object : PerformanceMetricGetter {
                    override fun getScore(hEmoState: Long): Float = IS_PerformanceMetricGetEngagementBoredomScore(hEmoState)
                }
            pmGetters[PM_INTEREST] = object : PerformanceMetricGetter {
                    override fun getScore(hEmoState: Long): Float = IS_PerformanceMetricGetInterestScore(hEmoState)
                }
            pmGetters[PM_FOCUS] = object : PerformanceMetricGetter {
                    override fun getScore(hEmoState: Long): Float = IS_PerformanceMetricGetFocusScore1(hEmoState)
                }
            return pmGetters
        }
    }

    init {
        performanceMetrics = HashMap()
        pmUpdateTimes = HashMap()
    }
}