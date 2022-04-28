/*
 * ExamplePipeline.cpp
 *
 *  Created on: 2013.03.18.
 *      Author: MegaStar
 */

#include "ExamplePipeline.h"
#include "logger.h"

ExamplePipeline::ExamplePipeline()
{
	// TODO Auto-generated constructor stub
	LOGI("Example pipeline constructor");
}

ExamplePipeline::~ExamplePipeline()
{
	// TODO Auto-generated destructor stub
}

void ExamplePipeline::initialize(int eegBufferSize,int channelCount,string* channelNames,int samplingRate)
{
	SignalProcessorPipeline::initialize(eegBufferSize,channelCount,channelNames,samplingRate);
	processorCount=1;
	processors = new SignalProcessor*[processorCount];
	processors[0] = new FrequencyAnalysis();
	for(int i=0; i<processorCount;i++)
	{
		processors[i]->initialize(eegBufferSize,channelCount,channelNames,samplingRate);
	}
	LOGI("Example pipeline initialized");
}
