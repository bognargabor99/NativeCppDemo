/*
 * SignalProcessorPipeline.h
 *
 *  Created on: 2013.03.18.
 *      Author: MegaStar
 */

#ifndef SIGNALPROCESSORPIPELINE_H_
#define SIGNALPROCESSORPIPELINE_H_

#include <string>
#include "EventTypes.h"
#include "ChannelSet.h"
#include "SignalProcessors/SignalProcessor.h"
#include "SignalProcessors/FrequencyAnalysis.h"
#include <jni.h>

using namespace std;

class SignalProcessorPipeline {
protected:
	string* usedChannelNames;
	int* usedChannelIds;
	int usedChannelsCount;
	int eegBufferSize;
	ChannelSet* eegBuffer;
	int samplingRate;
	int processorCount;
	SignalProcessor** processors;

public:
	SignalProcessorPipeline();
	virtual ~SignalProcessorPipeline();
	virtual void initialize(int eegBufferSize, int channelCount, string* channelNames, int samplingRate);
	EventTypes process(ChannelSet* eegBuffer, JNIEnv* env, jobject caller);
};

#endif /* SIGNALPROCESSORPIPELINE_H_ */
