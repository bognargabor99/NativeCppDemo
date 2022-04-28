package com.emotiv.sdk

object IEE_Event_t {
    const val IEE_UnknownEvent = 0x0000
    const val IEE_EmulatorError = 0x0001
    const val IEE_ReservedEvent = 0x0002
    const val IEE_UserAdded = 0x0010
    const val IEE_UserRemoved = 0x0020
    const val IEE_EmoStateUpdated = 0x0040
    const val IEE_ProfileEvent = 0x0080
    const val IEE_MentalCommandEvent = 0x0100
    const val IEE_FacialExpressionEvent = 0x0200
    const val IEE_InternalStateChanged = 0x0400
    const val IEE_AllEvent = 0x07F0
}