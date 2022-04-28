@file:Suppress("unused")

package com.emotiv.sdk

@Suppress("FunctionName", "KotlinJniMissingFunction")
object edkJavaJNI {
    init {
        System.loadLibrary("edk")
    }

    external fun IEE_EngineConnect__SWIG_0(strDevID: String?): Int

    external fun IEE_EngineDisconnect(): Int

    external fun IEE_GetSecurityCode(): Double

    external fun IEE_CheckSecurityCode(securityCode: Double): Int

    external fun IEE_EmoEngineEventCreate(): Long

    external fun IEE_EmoStateCreate(): Long

    external fun IEE_EmoEngineEventFree(hEvent: Long)

    external fun IEE_EmoStateFree(hEmoState: Long)

    external fun IEE_EngineGetNextEvent(hEvent: Long): Int

    external fun IEE_EmoEngineEventGetType(hEvent: Long): Int

    external fun IEE_EmoEngineEventGetEmoState(hEvent: Long, hEmoState: Long): Int

    external fun IEE_EmoEngineEventGetUserId(hEvent: Long, hUserIdOut: Int): Int

    external fun IEE_GetAverageBandPowers(
        userId: Int,
        channel: Int,
        theta: Double,
        alpha: Double,
        low_beta: Double,
        high_beta: Double,
        gamma: Double
    ): Int

    external fun IS_GetContactQuality(hEmoState: Long, inputChannel: Int): Int

    external fun IS_PerformanceMetricIsActive(hEmoState: Long, type: Int): Int

    external fun IS_PerformanceMetricGetExcitementLongTermScore(hEmoState: Long): Float

    external fun IS_PerformanceMetricGetInstantaneousExcitementScore(hEmoState: Long): Float

    external fun IS_PerformanceMetricGetRelaxationScore(hEmoState: Long): Float

    external fun IS_PerformanceMetricGetStressScore(hEmoState: Long): Float

    external fun IS_PerformanceMetricGetEngagementBoredomScore(hEmoState: Long): Float

    external fun IS_PerformanceMetricGetInterestScore(hEmoState: Long): Float

    external fun IS_PerformanceMetricGetFocusScore(hEmoState: Long): Float

    external fun CustomerSecurity_emotiv_func(securityCode: Double): Double
}