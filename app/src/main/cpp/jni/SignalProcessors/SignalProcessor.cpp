/*
 * SignalProcessor.cpp
 *
 *  Created on: 2013.02.25.
 *      Author: MegaStar
 */

#include "SignalProcessor.h"
#include "../logger.h"

SignalProcessor::SignalProcessor() {
	// TODO Auto-generated constructor stub
	this->eegBufferSize=0;
	processorBuffer=NULL;

}

SignalProcessor::~SignalProcessor() {
	if(processorBuffer)
		delete [] processorBuffer;
}

void SignalProcessor::initialize(int eegBufferSize,int channelCount, string* channelNames, int samplingRate)
{
	this->eegBufferSize=eegBufferSize;
	this->samplingRate = samplingRate;
	LOGI("Signal processor base class initialization done.");
}

EventTypes SignalProcessor::process(ChannelSet* eegBuffer, JNIEnv* env, jobject caller)
{
	LOGI("Signal processor base class process");
	return NoEvent;
}

SignalProcessorType SignalProcessor::getType()
{
	return type;
}

