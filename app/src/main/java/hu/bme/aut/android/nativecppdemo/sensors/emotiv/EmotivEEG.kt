package hu.bme.aut.android.nativecppdemo.sensors.emotiv

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
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
import hu.bme.aut.android.nativecppdemo.MainActivity
import java.util.*
import com.emotiv.sdk.edkJavaJNI.IS_PerformanceMetricGetFocusScore as IS_PerformanceMetricGetFocusScore1

class EmotivEEG(private val activity: MainActivity) : IEmotivEEG {
    private val backgroundHandler: Handler = Handler(Looper.getMainLooper())

    private val handler: Handler = Handler(Looper.getMainLooper())

    /**
     * Performance metric names: e.g. relaxation, stress, engagement etc.
     */
    private val pmNames: Map<Int, String> = Collections.unmodifiableMap(createPmNames())

    /**
     * Performance metric getters from edkJavaJNI.kt (functions starting with 'IS_')
     * These functions are used for retrieving data from Emotiv Insight
     */
    private val pmGetters: Map<Int, PerformanceMetricGetter> = Collections.unmodifiableMap(createPmGetters())

    /**
     * Performance metric values
     */
    private val performanceMetrics: MutableMap<Int, Float>

    /**
     * Timestamps for last known metric values
     */
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

    /**
     * Which channels provide alpha, beta, gamma, delta... waves
     * DON'T TOUCH THIS
     */
    var channelList = intArrayOf(IED_AF3, IED_AF4, IED_T7, IED_T8, IED_Pz)

    /**
     * Values alpha, beta, gamma, delta... waves
     * DON'T TOUCH THIS
     */
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
        val bluetoothAdapter = (activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val bluetoothEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled
        if (!bluetoothEnabled) {
            Toast.makeText(activity, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
        }
        return bluetoothEnabled
    }

    override fun connect() {
        if (status != SensorStatus.OFFLINE) {
            return
        }
        status = SensorStatus.CONNECTING
        sendStatus()
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
            EmotivBluetooth._emobluetooth = EmotivBluetooth(activity)
        }
        connectStartTime = SystemClock.uptimeMillis()
        backgroundHandler.post { connectDevice() }
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
                backgroundHandler.postDelayed({ connectDevice() }, CONNECT_PERIOD)
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
        Log.d("EMOTIV__EVENT", IEE_EngineGetNextEvent(pEvent).toString())
        Log.d("EMOTIV__STATUS", status.toString())
        if (status !== SensorStatus.ONLINE && status !== SensorStatus.BAD_SIGNAL) return

        val timestamp = System.currentTimeMillis()
        /**
         * Setting values of the waves (Whole while loop!!!)
         */
        while (IEE_EngineGetNextEvent(pEvent) == EDK_OK) {
            val type: Int = IEE_EmoEngineEventGetType(pEvent)
            IEE_EmoEngineEventGetUserId(pEvent, userID)
            var alpha: Double
            var low_beta: Double
            var high_beta: Double
            var gamma: Double
            val theta: Double = 0.0
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

        /**
         * Map for ('Metric name' to 'Metric value')
         */
        val namedMetrics: MutableMap<String?, Float?> = HashMap()

        /**
         * Helper string for sending data to [MainActivity]
         */
        var randomString = ""
        for (key in pmGetters.keys) {
            namedMetrics[pmNames[key]] = performanceMetrics[key]
            randomString += "${pmNames[key]} = ${performanceMetrics[key]}\n"
            val updateTime = if (pmUpdateTimes.containsKey(key)) pmUpdateTimes[key]!! else 0
            if (!badSignal && timestamp - updateTime > 0) {
                badSignal = true
            }
        }
        /**
         * Setting data in MainActivity
         */
        activity.emotivData = randomString

        val prevStatus = status
        status = if (badSignal) SensorStatus.BAD_SIGNAL else SensorStatus.ONLINE
        if (status !== prevStatus) {
            sendStatus()
        }

        /**
         * Polling Emotiv EEG periodically
         */
        backgroundHandler.postDelayed({ handleEvents() }, POLL_PERIOD)
    }

    private fun updatePerformanceMetric(key: Int, timestamp: Long) {
        if (IS_PerformanceMetricIsActive(pEmoState, key) == 1) {
            //Float oldScore = performanceMetrics.get(key);
            val newScore = pmGetters[key]!!.getScore(pEmoState)
            performanceMetrics[key] = newScore
            pmUpdateTimes[key] = timestamp
        }
    }

    override fun disconnect() {
        if (status == SensorStatus.OFFLINE) {
            return
        }
        status = SensorStatus.OFFLINE
        sendStatus()
        backgroundHandler.removeCallbacksAndMessages(null)
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
    }

    private fun sendStatus() {
        // TODO: Contact Framework team
    }

    companion object {
        private const val TAG = "EmotivEEG"
        private const val DEV_ID = "EmotivApp-android"
        private const val PERMISSION_REQUEST_CODE = 0
        private const val BLUETOOTH_REQUEST_CODE = 1
        private const val CONNECT_TIMEOUT = 10000L
        private const val CONNECT_PERIOD = 10L
        private const val POLL_PERIOD = 1000L

        /**
         * Stores performance metric names into a Map
         */
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

        /**
         * Creates performance metric getters
         */
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