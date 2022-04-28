package hu.bme.aut.android.nativecppdemo.sensors.emotiv

import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_ENGAGEMENT
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_EXCITEMENT
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_FOCUS
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_INTEREST
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_RELAXATION
import com.emotiv.sdk.IEE_PerformanceMetricAlgo_t.PM_STRESS

interface IEmotivEEG {
    fun connect()
    fun disconnect()

    companion object {
        fun createPmNames(): Map<Int, String>? {
            val pmNames: MutableMap<Int, String> = HashMap()
            pmNames[PM_EXCITEMENT] = "excitement"
            pmNames[PM_RELAXATION] = "relaxation"
            pmNames[PM_STRESS] = "stress"
            pmNames[PM_ENGAGEMENT] = "engagement"
            pmNames[PM_INTEREST] = "interest"
            pmNames[PM_FOCUS] = "focus"
            return pmNames
        }
    }
}