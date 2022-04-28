/*
 * EEGSensor.h
 *
 *  Created on: 2013.02.24.
 *      Author: MegaStar
 */

#ifndef EEGSENSOR_H_
#define EEGSENSOR_H_

//#include "EmotivDriver/emokit.h"
#include <string.h>
#include <stdio.h>
#include <math.h>
#include "ChannelSet.h"
#include <string>
#include "SignalProcessorPipeline.h"
#include <map>

extern "C" {
  #include "EmotivDriver/emokit.h"
}

using namespace std;

enum DeviceType
{
	Simulator,
	Emotiv,
	EmotivEDF
};

class EEGSensor {
private:
	int sampleRate;
	time_t triggerTimeStamp;
	int triggerType;
	int channelCount;
	int bufferSize;
	ChannelSet* buffer;
	string* channelNames;
public:
	int currentBufferPosition;
	SignalProcessorPipeline** pipelines;
	int pipelineCount;

public:
	EEGSensor();
	~EEGSensor();
	int getChannelCount() const;
	void setChannelCount(int channelCount);
	int getSampleRate() const;
	void setSampleRate(int sampleRate);
	time_t getTriggerTimeStamp() const;
	void setTriggerTimeStamp(time_t triggerTimeStamp);
	int getTriggerType() const;
	void setTriggerType(int triggerType);
	
	void addToBuffer(ChannelSet channelSet, JNIEnv* env, jobject caller);
	ChannelSet* getBuffer() const;
	void setBuffer(ChannelSet* buffer);
	int getBufferSize() const;
	void setBufferSize(int bufferSize);

	void initializeSensor(int type);
	DeviceType deviceType;
}
;

#endif /* EEGSENSOR_H_ */
