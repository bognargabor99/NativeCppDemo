/*
 * SignalProcessor.h
 *
 *  Created on: 2013.02.25.
 *      Author: MegaStar
 */

#ifndef SIGNALPROCESSOR_H_
#define SIGNALPROCESSOR_H_


#include "../ChannelSet.h"
#include "../EventTypes.h"
#include "SignalProcessorType.h"
#include <jni.h>
#include <string>

using namespace std;


class SignalProcessor {


protected:
	string* usedChannelNames;
	int* usedChannelIds;
	int usedChannelsCount;
	int processorBufferSize;
	int eegBufferSize;
	ChannelSet* processorBuffer;
	int samplingRate;
	SignalProcessorType type;

public:
	SignalProcessor();
	virtual ~SignalProcessor();
	virtual void initialize(int eegBufferSize, int channelCount, string* channelNames, int samplingRate);
	virtual EventTypes process(ChannelSet* eegBuffer, JNIEnv* env, jobject caller);
	SignalProcessorType getType();
};

#endif /* SIGNALPROCESSOR_H_ */
