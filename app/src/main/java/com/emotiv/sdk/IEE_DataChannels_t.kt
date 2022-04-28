package com.emotiv.sdk

object IEE_DataChannels_t {
    const val IED_COUNTER = 0 //!< Sample counter
    const val IED_INTERPOLATED = 1 //!< Indicate if data is interpolated
    const val IED_RAW_CQ = 2 //!< Raw contact quality value
    const val IED_AF3 = 3 //!< Channel AF3
    const val IED_F7 = 4 //!< Channel F7
    const val IED_F3 = 5 //!< Channel F3
    const val IED_FC5 = 6 //!< Channel FC5
    const val IED_T7 = 7 //!< Channel T7
    const val IED_P7 = 8 //!< Channel P7
    const val IED_Pz = 9 //!< Channel Pz
    const val IED_O1 = 9 //!< Channel O1 = Pz
    const val IED_O2 = 10 //!< Channel O2
    const val IED_P8 = 11 //!< Channel P8
    const val IED_T8 = 12 //!< Channel T8
    const val IED_FC6 = 13 //!< Channel FC6
    const val IED_F4 = 14 //!< Channel F4
    const val IED_F8 = 15 //!< Channel F8
    const val IED_AF4 = 16 //!< Channel AF4
    const val IED_GYROX = 17 //!< Gyroscope X-axis
    const val IED_GYROY = 18 //!< Gyroscope Y-axis
    const val IED_TIMESTAMP = 19 //!< System timestamp
    const val IED_MARKER_HARDWARE = 20 //!< Marker from extender
    const val IED_ES_TIMESTAMP = 21 //!< EmoState timestamp
    const val IED_FUNC_ID = 11 //!< Reserved function id
    const val IED_FUNC_VALUE = 23 //!< Reserved function value
    const val IED_MARKER = 24 //!< Marker value from hardware
    const val IED_SYNC_SIGNAL = 25 //!< Synchronisation signal
}