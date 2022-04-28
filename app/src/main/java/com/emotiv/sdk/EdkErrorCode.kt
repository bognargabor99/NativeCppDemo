package com.emotiv.sdk

object EdkErrorCode {
    const val EDK_OK = 0x0000

    // ! An internal error occurred
    const val EDK_UNKNOWN_ERROR = 0x0001

    // ! The contents of the buffer supplied to EE_SetUserProfile aren't a
    // valid; serialized EmoEngine profile.
    const val EDK_INVALID_PROFILE_ARCHIVE = 0x0101

    // ! Returned from EE_EmoEngineEventGetUserId if the event supplied contains
    // a base profile  = which isn't associated with specific user).
    const val EDK_NO_USER_FOR_BASEPROFILE = 0x0102

    // ! The EmoEngine is unable to acquire EEG data for processing.
    const val EDK_CANNOT_ACQUIRE_DATA = 0x0200

    // ! The buffer supplied to the function isn't large enough
    const val EDK_BUFFER_TOO_SMALL = 0x0300

    // ! A parameter supplied to the function is out of range
    const val EDK_OUT_OF_RANGE = 0x0301

    // ! One of the parameters supplied to the function is invalid
    const val EDK_INVALID_PARAMETER = 0x0302

    // ! The parameter value is currently locked by a running detection and
    // cannot be modified at this time.
    const val EDK_PARAMETER_LOCKED = 0x0303

    // ! The current training action is not in the list of expected training
    // actions
    const val EDK_MC_INVALID_TRAINING_ACTION = 0x0304

    // ! The current training control is not in the list of expected training
    // controls
    const val EDK_MC_INVALID_TRAINING_CONTROL = 0x0305

    // ! One of the field in the action bits vector is invalid
    const val EDK_MC_INVALID_ACTIVE_ACTION = 0x0306

    // ! The current action bits vector contains more action types than it is
    // allowed
    const val EDK_MC_EXCESS_MAX_ACTIONS = 0x0307

    // ! A trained signature is not currently available for use - addition
    // actions  = including neutral) may be required
    const val EDK_FE_NO_SIG_AVAILABLE = 0x0308

    // ! A filesystem error occurred that prevented the function from succeeding
    const val EDK_FILESYSTEM_ERROR = 0x0309

    // ! The user ID supplied to the function is invalid
    const val EDK_INVALID_USER_ID = 0x0400

    // ! The EDK needs to be initialized via EE_EngineConnect or
    // EE_EngineRemoteConnect
    const val EDK_EMOENGINE_UNINITIALIZED = 0x0500

    // ! The connection with a remote instance of the EmoEngine  = made via
    // EE_EngineRemoteConnect) has been lost
    const val EDK_EMOENGINE_DISCONNECTED = 0x0501

    // ! The API was unable to establish a connection with a remote instance of
    // the EmoEngine.
    const val EDK_EMOENGINE_PROXY_ERROR = 0x0502

    // ! There are no new EmoEngine events at this time
    const val EDK_NO_EVENT = 0x0600

    // ! The gyro is not calibrated. Ask the user to stay still for at least
    // 0.5s
    const val EDK_GYRO_NOT_CALIBRATED = 0x0700

    // ! Operation failure due to optimization
    const val EDK_OPTIMIZATION_IS_ON = 0x0800

    // ! Reserved return value
    const val EDK_RESERVED1 = 0x0900
}