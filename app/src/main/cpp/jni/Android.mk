# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := .

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 

LOCAL_MODULE    := InnolearnSensorsLib
LOCAL_SRC_FILES := $(call all-java-files-under, java) $(call all-Iaidl-files-under, java) hrm/hu_bme_innolearn_hrm_HRMSignalProcessor.cpp logger.cpp EmotivDriver/emokit.c ChannelSet.cpp  EEGSensor.cpp  SignalProcessors/SignalProcessor.cpp  Kiss_fft/kiss_fastfir.c Kiss_fft/kiss_fft.c Kiss_fft/kiss_fftr.c SignalProcessors/FrequencyAnalysis.cpp JniMethods.cpp SignalProcessorPipeline.cpp ExamplePipeline.cpp
LOCAL_AIDL_INCLUDES := $(call all-Iaidl-files-under, java)
include $(BUILD_SHARED_LIBRARY)
