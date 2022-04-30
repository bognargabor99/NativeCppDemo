package com.emotiv.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.math.BigInteger
import java.util.*


/**
 * Don't touch this.
 * This is a helper class migrated from FrameworkV2
 */
@Suppress("PrivatePropertyName", "KotlinJniMissingFunction", "MissingPermission",
    "LocalVariableName", "FunctionName"
)
class EmotivBluetooth(context: Context) {
    /* UUIDs for Insight */
    private val DEVICE_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")

    // --Commented out by Inspection (5/16/16, 4:41 PM):private final UUID DEVICE_BATERY_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private val Serial_Characteristic_UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    private val Firmware_Characteristic_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    private val Setting_Characteristic_UUID = UUID.fromString("81072F44-9F3D-11E3-A9DC-0002A5D5C51B")

    // --Commented out by Inspection (5/16/16, 4:41 PM):private final UUID Manufac_Characteristic_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private val DATA_SERVICE_UUID = UUID.fromString("81072F40-9F3D-11E3-A9DC-0002A5D5C51B")
    private val EEG_Characteristic_UUID = UUID.fromString("81072F41-9F3D-11E3-A9DC-0002A5D5C51B")
    private val MEMS_Characteristic_UUID = UUID.fromString("81072F42-9F3D-11E3-A9DC-0002A5D5C51B")
    private val Config_Characteristic_UUID = UUID.fromString("81072F43-9F3D-11E3-A9DC-0002A5D5C51B")
    private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // --Commented out by Inspection (5/16/16, 4:41 PM):private static final long SCAN_PERIOD = 1000;
    private val STATUS_CONNECTED = 1
    private val STATUS_NOTCONNECT = 0
    private val mContext: Context
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothManager: BluetoothManager? = null
    private var scan: ScanCallback? = null
    private var mLEScanner: BluetoothLeScanner? = null
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val tHandler: Handler = Handler(Looper.getMainLooper())
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var _TypeHeadset = 0 // 0 for Insight ; 1 for EPOC
    private var bluetoothGatt: BluetoothGatt? = null
    private val list_device_epoc: MutableList<BluetoothDevice> = ArrayList()
    private val list_device_insight: MutableList<BluetoothDevice> = ArrayList()
    private val status_device_insight = HashMap<BluetoothDevice, Int>()
    private val status_device_epoc = HashMap<BluetoothDevice, Int>()
    private val signal_insight = HashMap<BluetoothDevice, Int>()
    private val signal_epoc = HashMap<BluetoothDevice, Int>()
    private val device_uuid = arrayOf(
        DEVICE_SERVICE_UUID
    )
    private var lock = false
    private var haveData = false
    private var start_counter = 0
    private var valueMode = 0
    private var testnortifi = -1.0
    private var CheckNortifiBLE = 0.0
    private var checkUser = false
    var isSettingMode = false
    private var isConnected = false
    private var isNortifyMotion = false

    // --Commented out by Inspection (5/16/16, 4:42 PM):public boolean isEnableScan = true;
    private val EEGBuffer = ByteArray(32)
    private val EEGBuffer_new_Insight = ByteArray(20)
    private val MEMSBuffer = ByteArray(20)

    private val thread_check_adapter: Runnable = object : Runnable {
        override fun run() {
            // TODO Auto-generated method stub
            try {
                if (!mBluetoothAdapter!!.isEnabled) {
                    handler.postDelayed(this, 500)
                    //Log.e("edkJavaJNI bluetooth","check enable BLE");
                } else {
                    scanLeDevice(true)
                    handler.removeCallbacks(this)
                }
            } catch (e: NullPointerException) {
                // TODO: handle exception
            }
        }
    }
    private val thread_retrive: Runnable = object : Runnable {
        override fun run() {
            try {
                retrieveConnect()
                mHandler.postDelayed(this, 500)
            } catch (e: NullPointerException) {
                // TODO: handle exception
            }
        }
    }

    /* Bug on Android 4.4.3:
       Get disconnected event 10s after headset is turned off.
       So we must check nortifi manually and call function disconnect().
     */
    private val thread_checknortifi: Runnable = object : Runnable {
        override fun run() {
            if (haveData) {
                if (testnortifi != CheckNortifiBLE) {
                    testnortifi = CheckNortifiBLE
                } else {
                    if (bluetoothGatt != null) {
                        //  Log.e("_____BLE____","Call disconnect");
                        val thread: Thread = object : Thread() {
                            override fun run() {
                                try {
                                    bluetoothGatt!!.disconnect()
                                    haveData = false
                                } catch (e: NullPointerException) {
                                    // TODO: handle exception
                                }
                            }
                        }
                        thread.start()
                    }
                }
                tHandler.postDelayed(this, 1000)
            } else {
                if (bluetoothGatt != null) {
                    val thread: Thread = object : Thread() {
                        override fun run() {
                            try {
                                bluetoothGatt!!.disconnect()
                            } catch (e: NullPointerException) {
                                // TODO: handle exception
                            }
                        }
                    }
                    thread.start()
                }
            }
        }
    }

    /* Get number of devices with EPOC+ type */
    fun GetNumberDeviceEpocPlus(): Int {
        return list_device_epoc.size
    }

    /* Get number of devices with Insight type */
    fun GetNumberDeviceInsight(): Int {
        return list_device_insight.size
    }

    /* Get device name of EPOC+ headset */
    fun GetNameDeviceEpocPlus(index: Int): String {
        var name = ""
        try {
            if (index < list_device_epoc.size) {
                name = list_device_epoc[index].name
            }
        } catch (e: Exception) {
            // TODO: handle exception
        }
        return name
    }

    /* Get device name of Insight headset */
    fun GetNameDeviceInsight(index: Int): String {
        var name = ""
        try {
            if (index < list_device_insight.size) {
                name = list_device_insight[index].name
            }
        } catch (e: Exception) {
            // TODO: handle exception
        }
        return name
    }

    /* Feature Setting Mode when use EPOC+
     *   value = 0 -> epoc 14bit
     *   value = 1 -> epoc 16bit No motion
     *   value = 2 -> epoc 16bit 32hz motion
     *   value = 3 -> epoc 16bit 64hz motion
     */
    fun EmoSettingMode(value: Int): Int {
        if (value != 0 && value != 1 && value != 2) {
            return 0
        }
        when (value) {
            0 -> valueMode = 256
            1 -> valueMode = 384
            2 -> valueMode = 388
            3 -> valueMode = 392
            else -> {}
        }
        return 1
    }

    /* Connecting device */
    fun EmoConnectDevice(idDevice: Int, indexDevice: Int): Boolean {
        var device: BluetoothDevice? = null
        bluetoothGatt = null
        when (idDevice) {
            0 -> if (!list_device_insight.isEmpty() && indexDevice < list_device_insight.size) {
                device = list_device_insight[indexDevice]
            }
            1 -> if (!list_device_epoc.isEmpty() && indexDevice < list_device_epoc.size) {
                device = list_device_epoc[indexDevice]
            }
            else -> {}
        }
        if (device != null && !isConnected) {
            mBluetoothAdapter!!.cancelDiscovery()
            bluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback)
        }
        return if (bluetoothGatt != null) {
            scanLeDevice(false)
            list_device_epoc.clear()
            list_device_insight.clear()
            true
        } else {
            false
        }
    }

    /*Disconnect current device*/
    fun DisconnectHeadset(): Boolean {
        if (bluetoothGatt != null) {
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        bluetoothGatt!!.disconnect()
                    } catch (e: NullPointerException) {
                        // TODO: handle exception
                    }
                }
            }
            thread.start()
            return true
        }
        return false
    }

    /* Get signal strength from Insight */
    fun GetSignalStrengthBLEInsight(index: Int): Int {
        return if (index < 0 || index >= list_device_insight.size) 0 else signal_insight[list_device_insight[index]]!!
    }

    /* Get signal strength from EPOC+ */
    fun GetSignalStrengthBLEEPOCPLUS(index: Int): Int {
        return if (index < 0 || index >= list_device_epoc.size) 0 else signal_epoc[list_device_epoc[index]]!!
    }

    fun GetStatusDeviceInsight(index: Int): Int {
        return if (index < 0 || index >= list_device_insight.size) -1 else status_device_insight[list_device_insight[index]]!!
    }

    fun GetStatusDeviceEpoc(index: Int): Int {
        return if (index < 0 || index >= list_device_epoc.size) -1 else status_device_epoc[list_device_epoc[index]]!!
    }

    fun SettingModeForHeadset(value: Int): Int {
        valueMode = if (list_device_epoc.isEmpty()) {
            return 0
        } else {
            value
        }
        return 1
    }

    fun RefreshScanDevice() {
        try {
            for (i in list_device_insight.indices) {
                val device = list_device_insight[i]
                if (status_device_insight[device] == STATUS_NOTCONNECT) {
                    signal_insight.remove(device)
                    status_device_insight.remove(device)
                    list_device_insight.remove(device)
                }
            }
            for (i in list_device_epoc.indices) {
                val device = list_device_epoc[i]
                if (status_device_epoc[device] == STATUS_NOTCONNECT) {
                    signal_epoc.remove(device)
                    status_device_epoc.remove(device)
                    list_device_epoc.remove(device)
                }
            }
            if (!mBluetoothAdapter!!.isEnabled) return
            if (mLEScanner != null) {
                InitCallbackFunction()
                mLEScanner!!.stopScan(scan)
            }
            if (mLEScanner != null) {
                InitCallbackFunction()
                mLEScanner!!.startScan(scan)
            }
        } catch (e: NullPointerException) {
            // TODO: handle exception
        }
    }

    /*Retrieve Connect Device*/
    private fun retrieveConnect() {
        val devicelist: List<BluetoothDevice> = bluetoothManager!!.getConnectedDevices(BluetoothProfile.GATT)
        if (devicelist.isNotEmpty()) {
            for (i in list_device_insight.indices) {
                val insight_device = list_device_insight[i]
                if (!devicelist.contains(insight_device) && status_device_insight[insight_device] == STATUS_CONNECTED) {
                    list_device_insight.remove(insight_device)
                    status_device_insight.remove(insight_device)
                    signal_insight.remove(insight_device)
                }
            }
            for (i in list_device_epoc.indices) {
                val epoc_device = list_device_epoc[i]
                if (!devicelist.contains(epoc_device) && status_device_epoc[epoc_device] == STATUS_CONNECTED) {
                    list_device_epoc.remove(epoc_device)
                    status_device_epoc.remove(epoc_device)
                    signal_epoc.remove(epoc_device)
                }
            }
            for (i in devicelist.indices) {
                val device = devicelist[i]
                if (device.name.contains("Insight")) {
                    if (!list_device_insight.contains(device)) {
                        list_device_insight.add(device)
                    }
                    signal_insight[device] = 0
                    status_device_insight[device] = STATUS_CONNECTED
                } else if (device.name.contains("EPOC+")) {
                    if (!list_device_epoc.contains(device)) {
                        list_device_epoc.add(device)
                    }
                    status_device_epoc[device] = STATUS_CONNECTED
                    signal_epoc[device] = 0
                }
            }
        } else {
            for (i in list_device_insight.indices) {
                val device = list_device_insight[i]
                if (status_device_insight[device] == STATUS_CONNECTED) {
                    list_device_insight.remove(device)
                    status_device_insight.remove(device)
                    signal_insight.remove(device)
                }
            }
            for (i in list_device_epoc.indices) {
                val device = list_device_epoc[i]
                if (status_device_epoc[device] == STATUS_CONNECTED) {
                    list_device_epoc.remove(device)
                    status_device_epoc.remove(device)
                    signal_epoc.remove(device)
                }
            }
        }
    }

    /* Scan BTLE devices */
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            //Log.e("EmotivBluetooth","Scan device");
            mHandler.postDelayed(thread_retrive, 500)
            //isScanDevice = true;
            try {
                val scanFilter: List<ScanFilter> = ArrayList()
                val scanSettingBuilder = ScanSettings.Builder()
                //scanSettingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                val scanSettings = scanSettingBuilder.build()
                mLEScanner = mBluetoothAdapter!!.bluetoothLeScanner
                if (mLEScanner != null) {
                    InitCallbackFunction()
                    mLEScanner!!.startScan(scanFilter, scanSettings, scan)
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            //isScanDevice = false;
            mHandler.removeCallbacks(thread_retrive)
            try {
                if (mLEScanner != null) {
                    InitCallbackFunction()
                    mLEScanner!!.stopScan(scan)
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }

    /* Callback for the BTLE device scan */
    private val mLeScanCallback =
        LeScanCallback { device, rssi, scanRecord ->
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        if (device.name.contains("Insight")) {
                            //Log.e("EmoBluetooth", "add Insight");
                            //SignalStrengthBLE[0] = rssi;
                            //CounterScanInsight++;
                            if (!list_device_insight.contains(device)) {
                                list_device_insight.add(device)
                            }
                            status_device_insight[device] = STATUS_NOTCONNECT
                            signal_insight[device] = rssi
                        }
                        if (device.name.contains("EPOC+")) {
                            //SignalStrengthBLE[1] = rssi;
                            //CounterScanEpoc++;
                            if (!list_device_epoc.contains(device)) {
                                list_device_epoc.add(device)
                            }
                            status_device_epoc[device] = STATUS_NOTCONNECT
                            signal_epoc[device] = rssi
                        }
                    } catch (e: NullPointerException) {
                        // TODO: handle exception
                    }
                }
            }
            thread.start()
        }

    private fun InitCallbackFunction() {
        if (scan == null) scan = object : ScanCallback() {
            override fun onBatchScanResults(results: List<ScanResult>) {
                Log.i("bluetooth", "The batch result is " + results.size)
            }

            @SuppressLint("NewApi")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val thread: Thread = object : Thread() {
                    override fun run() {
                        try {
                            val device = result.device
                            if (device.name.contains("Insight")) {
                                //SignalStrengthBLE[0] = result.getRssi();
                                //CounterScanInsight++;
                                //	Log.e("EmoBluetooth", "Insight device");
                                if (!list_device_insight.contains(device)) {
                                    list_device_insight.add(device)
                                }
                                signal_insight[device] = result.rssi
                                status_device_insight[device] = STATUS_NOTCONNECT
                            }
                            if (device.name.contains("EPOC+")) {
                                //SignalStrengthBLE[1] = result.getRssi();
                                //CounterScanEpoc++;
                                if (!list_device_epoc.contains(device)) {
                                    list_device_epoc.add(device)
                                }
                                signal_epoc[device] = result.rssi
                                status_device_epoc[device] = STATUS_NOTCONNECT
                            }
                        } catch (e: NullPointerException) {
                            // TODO: handle exception
                        }
                    }
                }
                thread.start()
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
            }
        }
    }

    /* Callback for services discovery */
    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(100)
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                    if (_TypeHeadset == 0) {
                        readValueForCharacteristec(
                            bluetoothGatt,
                            DEVICE_SERVICE_UUID,
                            Firmware_Characteristic_UUID
                        )
                    }
                    if (_TypeHeadset == 1) {
                        if (!isSettingMode) {
                            readValueForCharacteristec(
                                bluetoothGatt,
                                DATA_SERVICE_UUID,
                                Setting_Characteristic_UUID
                            )
                        } else {
                            SetNortifyValue(bluetoothGatt, 2)
                        }
                    }
                }
            }
            thread.start()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            if (characteristic.uuid == Serial_Characteristic_UUID) {
                val serial = characteristic.value
                if (serial != null && serial.isNotEmpty()) {
                    SendSerialNumber(serial)
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
                if (!checkUser) {
                    tHandler.postDelayed(thread_checknortifi, 1000)
                    checkUser = true
                    SetNortifyValue(gatt, 0)
                }
            } else if (characteristic.uuid == Setting_Characteristic_UUID) {
                val setting = characteristic.value
                if (setting != null && setting.isNotEmpty()) {
                    ReadUserConfig(characteristic.value)
                }
                readValueForCharacteristec(gatt, DEVICE_SERVICE_UUID, Firmware_Characteristic_UUID)
            } else if (characteristic.uuid == Firmware_Characteristic_UUID) {
                val firmware = characteristic.value
                if (firmware != null && firmware.isNotEmpty()) {
                    isConnected = true
                    SendFirmWareVersion(characteristic.value)
                }
                readValueForCharacteristec(gatt, DEVICE_SERVICE_UUID, Serial_Characteristic_UUID)
            }
        }

        /* Callback for getting data from BTLE */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val oldData = characteristic.value
            if (characteristic.uuid == EEG_Characteristic_UUID) {
                haveData = true
                CheckNortifiBLE++
                if (!isNewDataFormat) {
                    if (!lock) {
                        start_counter = oldData[0].toInt()
                        lock = true
                        val chunk_start = oldData[1].toInt()
                        if (chunk_start == 1) System.arraycopy(oldData, 2, EEGBuffer, 0, 16)
                    }
                    if (start_counter == oldData[0].toInt()) {
                        // receive 1 packet
                        System.arraycopy(oldData, 2, EEGBuffer, 16 * (oldData[1].toInt() - 1), 16)
                        if (oldData[1].toInt() == 2) {
                            WriteEEG(EEGBuffer)
                            //                            if (start_counter >= 127) {
//                                start_counter = 0;
//                            } else {
//                                start_counter++;
//                            }
                        }
                    } else {
                        //if ((int) oldData[0] < 128 && (int) oldData[0] >= 0) {
                        start_counter = oldData[0].toInt()
                        System.arraycopy(oldData, 2, EEGBuffer, 16 * (oldData[1].toInt() - 1), 16)
                        //}
                    }
                } else {
                    System.arraycopy(oldData, 0, EEGBuffer_new_Insight, 0, oldData.size)
                    WriteEEG(EEGBuffer_new_Insight)
                }
            } else if (characteristic.uuid == MEMS_Characteristic_UUID) {
                System.arraycopy(oldData, 0, MEMSBuffer, 0, oldData.size)
                WriteMEMS(MEMSBuffer)
                Arrays.fill(MEMSBuffer, 0.toByte())
            }
        }

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int, newState: Int
        ) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    bluetoothGatt!!.disconnect()
                    bluetoothGatt!!.close()
                    list_device_insight.clear()
                    list_device_epoc.clear()
                } else {
                    /*this callback not stable . So, isConnected = true only set when read success an characteristic*/
                    //isConnected = true;
                    try {
                        if (gatt.device.name.contains("Insight")) {
                            SetTypeHeadset(0)
                            _TypeHeadset = 0
                        }
                        if (gatt.device.name.contains("EPOC+")) {
                            SetTypeHeadset(1)
                            _TypeHeadset = 1
                        }
                    } catch (e: NullPointerException) {
                        // TODO: handle exception
                    }
                    lock = false
                    list_device_insight.clear()
                    list_device_epoc.clear()
                    val thread: Thread = object : Thread() {
                        override fun run() {
                            try {
                                sleep(200)
                            } catch (e: InterruptedException) {
                                // TODO Auto-generated catch block
                                e.printStackTrace()
                            }
                            gatt.discoverServices()
                        }
                    }
                    thread.start()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isConnected = false
                //Log.e("EmotivBluetooth","device disconnected");
                val thread: Thread = object : Thread() {
                    override fun run() {
                        try {
                            sleep(50)
                        } catch (e: InterruptedException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }
                        gatt.close()
                        bluetoothGatt = null
                        if (checkUser) {
                            checkUser = false
                            DisconnectDevice()
                            tHandler.removeCallbacks(thread_checknortifi)
                        }
                        if (mBluetoothAdapter!!.isEnabled) {
                            scanLeDevice(true)
                        } else {
                            handler.postDelayed(thread_check_adapter, 500)
                        }
                    }
                }
                thread.start()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (!isSettingMode) {
                if (_TypeHeadset != 0 && !isNortifyMotion) {
                    SetNortifyValue(gatt, 1)
                    isNortifyMotion = true
                }
            } else {
                WriteValueForCharacteristic(gatt, valueMode)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val result = characteristic.value
                if (result[0].toInt() == 0x01) {
                    isSettingMode = false
                    readValueForCharacteristec(gatt, DATA_SERVICE_UUID, Setting_Characteristic_UUID)
                }
            }
        }

        /* Setting read charesteristic */
        private fun readValueForCharacteristec(
            gatt: BluetoothGatt?,
            serviceUUID: UUID,
            characteristicUUID: UUID
        ) {
            try {
                val characteristic =
                    gatt!!.getService(serviceUUID).getCharacteristic(characteristicUUID)
                gatt.readCharacteristic(characteristic)
            } catch (e: NullPointerException) {
                // TODO: handle exception
            }
        }

        /* Setting write charesteristic */
        private fun WriteValueForCharacteristic(gatt: BluetoothGatt, value: Int) {
            try {
                val characteristic = gatt.getService(DATA_SERVICE_UUID)
                    .getCharacteristic(Config_Characteristic_UUID)
                val data = BigInteger.valueOf(value.toLong())
                if (characteristic != null) {
                    characteristic.value = data.toByteArray()
                    gatt.writeCharacteristic(characteristic)
                }
            } catch (e: NullPointerException) {
                // TODO: handle exception
            }
        }

        /* Setting notifiy charesteristic */
        private fun SetNortifyValue(gatt: BluetoothGatt?, state: Int) {
            val characteristic: BluetoothGattCharacteristic = when (state) {
                0 -> {
                    gatt!!.getService(DATA_SERVICE_UUID)
                        .getCharacteristic(EEG_Characteristic_UUID)
                }
                1 -> {
                    gatt!!.getService(DATA_SERVICE_UUID)
                        .getCharacteristic(MEMS_Characteristic_UUID)
                }
                2 -> {
                    gatt!!.getService(DATA_SERVICE_UUID)
                        .getCharacteristic(Config_Characteristic_UUID)
                }
                else -> return
            }

            // Enable local notifications
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            gatt.setCharacteristicNotification(characteristic, true)

            // Enabled remote notifications
            val desc = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(desc)
        }
    }

    /* Native function call in C++ */
    private external fun SendSerialNumber(serial: ByteArray)
    private external fun SendFirmWareVersion(value: ByteArray)
    private external fun WriteEEG(serial: ByteArray)
    private external fun WriteMEMS(serial: ByteArray)
    private external fun SetTypeHeadset(type: Int)
    private external fun ReadUserConfig(data: ByteArray)
    private external fun DisconnectDevice()
    private val isNewDataFormat: Boolean
        external get

    companion object {
        var _emobluetooth: EmotivBluetooth? = null

        init {
            System.loadLibrary("edk")
        }
    }

    init {
        isConnected = false
        mContext = context
        val toast = Toast.makeText(mContext, "Bluetooth not enabled", Toast.LENGTH_SHORT)
        val hasBLE = mContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (!hasBLE) {
            Toast.makeText(mContext, "Bluetooth LE not supported on this device", Toast.LENGTH_SHORT).show()
        } else {
            bluetoothManager = mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBluetoothAdapter = bluetoothManager!!.adapter
            // Checks if Bluetooth is supported on the device.
            if (mBluetoothAdapter != null) {
                handler.postDelayed(thread_check_adapter, 500)
                if (!mBluetoothAdapter!!.isEnabled) {
                    toast.show()
                }
            }
        }
    }
}