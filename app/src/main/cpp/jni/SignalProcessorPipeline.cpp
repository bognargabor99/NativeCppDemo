/*
 * SignalProcessorPipeline.cpp
 *
 *  Created on: 2013.03.18.
 *      Author: MegaStar
 */

#include "SignalProcessorPipeline.h"
#include "logger.h"

SignalProcessorPipeline::SignalProcessorPipeline()
{
	eegBuffer=NULL;
	processors=NULL;
}

SignalProcessorPipeline::~SignalProcessorPipeline()
{
	if(eegBuffer)
	{
		delete[] eegBuffer;
	}
	if(processors)
	{
		delete[] processors;
	}
}

void SignalProcessorPipeline::initialize(int eegBufferSize,int channelCount, string* channelNames, int samplingRate)
{
	this->eegBufferSize = eegBufferSize;
	this->samplingRate = samplingRate;
	if(eegBuffer==NULL)
	{
		eegBuffer = new ChannelSet[eegBufferSize];
	}
	LOGI("Pipeline base class initialized.");
}

EventTypes SignalProcessorPipeline::process(ChannelSet* eegBuffer, JNIEnv* env, jobject caller)
{
	EventTypes event;
	//Copy original data to buffer
	for(int i=0;i<eegBufferSize;i++)
	{
		this->eegBuffer[i] = eegBuffer[i];
	}
	// Process data in buffer
	for(int i=0;i<processorCount;i++)
	{
			event = processors[i]->process(this->eegBuffer,env,caller);
	}
	LOGI("Base pipeline process method done.");
	return event;
}

