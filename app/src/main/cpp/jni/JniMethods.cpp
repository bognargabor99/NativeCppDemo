/*
 * JniMethods.cpp
 *
 *  Created on: 2013.03.06.
 *      Author: MegaStar
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include "ChannelQualities.h"
#include "ChannelSet.h"
#include "EEGSensor.h"
#include "EmotivDriver/emokit.h"
#include "logger.h"

struct emokit_device* device = NULL;
EEGSensor* eegSensor = NULL;
jclass emotivClass; // Class instance
jmethodID emotivConstructor;

extern "C"
{
jstring Java_hu_bme_aut_adapted_usbdrivers_UsbDriver_initEmotivDriver(JNIEnv* env, jobject object, jbyteArray serial)

{
	device = emokit_create();
	signed char* tmp = (signed char*)malloc(16) ;
	env->GetByteArrayRegion(serial,0,16,tmp);
	jsize l = env->GetArrayLength(serial);
	int i;
	for(i=0;i<l;i++)
	{
		device->serial[i] = tmp[i];
	}
	emokit_init_crypto(device,1);
	free(tmp); // Release temp
	//return  env->NewStringUTF(device->key);
	LOGI("Emotiv driver crypt initializtion done.");
	return  env->NewStringUTF("");
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv *env;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK)
		return -1;
	//emotivClass = env->FindClass("hu/bme/aut/adapted/usbdrivers/EmotivFrame");
	//emotivConstructor = env->GetMethodID(emotivClass, "<init>", "(IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII)V");
	return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_hu_bme_aut_adapted_usbdrivers_UsbDriver_initializeSensor(JNIEnv* env, jobject object, jint type)
{
	eegSensor = new EEGSensor();
	switch (type) {
	case 0:
		LOGI("Init simulator.");
		eegSensor->initializeSensor(Simulator);
		break;
	case 1:
		LOGI("Init emotiv driver.");
		eegSensor->initializeSensor(Emotiv);
		break;
	default:
		break;
	}
}

JNIEXPORT void JNICALL Java_hu_bme_adapted_offlineprocessor_OfflineProcessor_initializeEDF(JNIEnv* env, jobject object, jint type)
{
	eegSensor = new EEGSensor();
	LOGI("Init emotiv driver for offline EDF processing.");
	eegSensor->initializeSensor(EmotivEDF);
}

void setEmotivData(JNIEnv* env, jobject object, jbyteArray rawData)
{
	signed char* tmp = (signed char*)malloc(32);
	env->GetByteArrayRegion(rawData,0,32,tmp);
	jsize l = env->GetArrayLength(rawData);
	int i;
	for(i=0;i<l;i++)
	{
		device->raw_frame[i] = tmp[i];
	}
	free(tmp); // Release tmp
	emokit_get_next_raw(device);
	device->current_frame=emokit_get_next_frame(device);

	// convert frame to java object
	//jobject retObj;

	//retObj = env->NewObject(emotivClass, emotivConstructor, device->current_frame.counter, device->current_frame.F3, device->current_frame.FC6, device->current_frame.P7, device->current_frame.T8, device->current_frame.F7,
	//		device->current_frame.F8, device->current_frame.T7, device->current_frame.P8, device->current_frame.AF4, device->current_frame.F4, device->current_frame.AF3, device->current_frame.O2, device->current_frame.O1,
	//	device->current_frame.FC5, device->current_frame.gyroX, device->current_frame.gyroY, device->current_frame.battery, device->current_frame.cq.F3, device->current_frame.cq.FC6,
	//device->current_frame.cq.P7, device->current_frame.cq.T8, device->current_frame.cq.F7, device->current_frame.cq.F8, device->current_frame.cq.T7, device->current_frame.cq.P8, device->current_frame.cq.AF4,
	//device->current_frame.cq.F4, device->current_frame.cq.AF3, device->current_frame.cq.O2, device->current_frame.cq.O1, device->current_frame.cq.FC5);
	//int x = eegSensor->currentBufferPosition;

	ChannelSet* channels = new ChannelSet(eegSensor->getChannelCount());
	channels->addChannel(device->current_frame.F3,BAD);
	channels->addChannel(device->current_frame.FC6,BAD);
	channels->addChannel(device->current_frame.P7,BAD);
	channels->addChannel(device->current_frame.T8,BAD);
	channels->addChannel(device->current_frame.F7,BAD);
	channels->addChannel(device->current_frame.F8,BAD);
	channels->addChannel(device->current_frame.T7,BAD);
	channels->addChannel(device->current_frame.P8,BAD);
	channels->addChannel(device->current_frame.AF4,BAD);
	channels->addChannel(device->current_frame.F4,BAD);
	channels->addChannel(device->current_frame.AF3,BAD);
	channels->addChannel(device->current_frame.O2,BAD);
	channels->addChannel(device->current_frame.O1,BAD);
	channels->addChannel(device->current_frame.FC5,BAD);

	if(device->current_frame.counter==0) // Send a channel quality event
	{
		jclass objectClass = env->GetObjectClass(object);
		jmethodID objectMethod = env->GetMethodID(objectClass,"emotivChannelsQuality","(IIIIIIIIIIIIII)V");
		env->CallVoidMethod(object,objectMethod,
				device->last_quality.F3,
				device->last_quality.FC6,
				device->last_quality.P7,
				device->last_quality.T8,
				device->last_quality.F7,
				device->last_quality.F8,
				device->last_quality.T7,
				device->last_quality.P8,
				device->last_quality.AF4,
				device->last_quality.F4,
				device->last_quality.AF3,
				device->last_quality.O2,
				device->last_quality.O1,
				device->last_quality.FC5);
		LOGI("Quality event sent");
	}
	eegSensor->addToBuffer(*channels,env, object);
}

//Fill EDF read data into processing environment
void setEmotivEDFData(JNIEnv* env, jobject object, jdoubleArray edfData)
{
	if (eegSensor->getChannelCount()!=env->GetArrayLength(edfData))
		return;

	ChannelSet* channels = new ChannelSet(eegSensor->getChannelCount());

	double* data = env->GetDoubleArrayElements(edfData,NULL);

	for(int i = 0; i<eegSensor->getChannelCount();++i){
		channels->addChannel(data[i],BAD);
	}

	eegSensor->addToBuffer(*channels,env, object);
}

int tmp = 0;

void setSimulatorData(JNIEnv* env, jobject object, jbyteArray rawData)
{

	ChannelSet* channels = new ChannelSet(eegSensor->getChannelCount());
	tmp++;
	if(tmp%10>5)
	{
		channels->addChannel(1,GOOD);
	}
	else {
		channels->addChannel(0,GOOD);
	}
	eegSensor->addToBuffer(*channels, env, object);
}

JNIEXPORT void JNICALL Java_hu_bme_aut_adapted_usbdrivers_UsbDriver_setNewData(JNIEnv* env, jobject object, jbyteArray rawData)
{
	switch (eegSensor->deviceType) {
	case Simulator:
		setSimulatorData(env,object,rawData);
		break;
	case Emotiv:
		setEmotivData(env,object,rawData);
		break;
	default:
		break;
	}
}

JNIEXPORT void JNICALL Java_hu_bme_adapted_offlineprocessor_OfflineProcessor_setNewEDFData(JNIEnv* env, jobject object, jdoubleArray edfData)
{
	if (eegSensor->deviceType==EmotivEDF){
		setEmotivEDFData(env,object,edfData);
	}
}

JNIEXPORT jstring JNICALL Java_hu_bme_aut_adapted_usbdrivers_UsbDriver_valami( JNIEnv* env,
		jobject thiz )
{
	jobject retObj;
	jmethodID constructor;
	jobject cls;
	retObj = env->NewObject(emotivClass, emotivConstructor, 0,0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0,
			0,0, 0, 0, 0);

	return env->NewStringUTF("Teszt");
}
}
