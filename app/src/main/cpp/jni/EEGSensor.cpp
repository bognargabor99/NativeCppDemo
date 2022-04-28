/*
 * EEGSensor.cpp
 *
 *  Created on: 2013.02.24.
 *      Author: MegaStar
 */

#include "EEGSensor.h"
#include "logger.h"
#include "ExamplePipeline.h"

EEGSensor::EEGSensor()
{
	currentBufferPosition = 0;
	pipelineCount=1;
	buffer=NULL;
	pipelines=NULL;

	pipelines = new SignalProcessorPipeline*[pipelineCount];
	pipelines[0] = new ExamplePipeline();
}

EEGSensor::~EEGSensor()
{
	if(buffer)
	{
		delete[] buffer;
	}
	if(pipelines)
	{
		delete[] pipelines;
	}
}

int EEGSensor::getChannelCount() const
{
	return channelCount;
}

void EEGSensor::setChannelCount(int channelCount)
{
	this->channelCount = channelCount;
}

int EEGSensor::getSampleRate() const
{
	return sampleRate;
}

void EEGSensor::setSampleRate(int sampleRate)
{
	this->sampleRate = sampleRate;
}

time_t EEGSensor::getTriggerTimeStamp() const
{
	return triggerTimeStamp;
}

void EEGSensor::setTriggerTimeStamp(time_t triggerTimeStamp)
{
	this->triggerTimeStamp = triggerTimeStamp;
}

int EEGSensor::getTriggerType() const
{
	return triggerType;
}

void EEGSensor::setTriggerType(int triggerType)
{
	this->triggerType = triggerType;
}


ChannelSet* EEGSensor::getBuffer() const
{
	return buffer;
}

void EEGSensor::setBuffer(ChannelSet* buffer)
{
	this->buffer = buffer;
}

int EEGSensor::getBufferSize() const
{
	return bufferSize;
}

void EEGSensor::setBufferSize(int bufferSize)
{
	this->bufferSize = bufferSize;
}

void EEGSensor::addToBuffer(ChannelSet channelSet, JNIEnv* env, jobject caller)
{

	ChannelSet* mySet = new ChannelSet(channelCount);
	for (int i = 0; i<channelCount;++i)
		mySet->addChannel(channelSet.getChannelValue(i),BAD);

	buffer[currentBufferPosition]= *mySet;

	currentBufferPosition++;
	if(currentBufferPosition==bufferSize)
	{
		for(int i=0;i<pipelineCount;i++)
		{
			EventTypes event =  pipelines[i]->process(buffer,env, caller);
			if(event==NoEvent)
			{
				//Call JAVA method
				LOGI("No event detected.");
			}
		}
		//delete [] buffer;
		//buffer = new ChannelSet[bufferSize];
		currentBufferPosition=0;
	}
}

void EEGSensor::initializeSensor(int type)
{
	//deviceType = type;
	switch (type)
	{
	case Simulator:
		channelCount = 1;
		sampleRate = 128;
		bufferSize = 128;
		buffer = new ChannelSet[bufferSize];
		channelNames = new string[channelCount];
		channelNames[0]="F3";
		for(int i=0;i<pipelineCount;i++)
		{
			pipelines[i]->initialize(128,1,channelNames,sampleRate);
		}
		break;
	case Emotiv:
	case EmotivEDF:
		channelCount = 14;
		sampleRate = 128;
		bufferSize =128;
		buffer = new ChannelSet[bufferSize];
		channelNames = new string[channelCount];
		channelNames[0]="F3";
		channelNames[1]="FC6";
		channelNames[2]="P7";
		channelNames[3]="T8";
		channelNames[4]="F7";
		channelNames[5]="F8";
		channelNames[6]="T7";
		channelNames[7]="P8";
		channelNames[8]="AF4";
		channelNames[9]="F4";
		channelNames[10]="AF3";
		channelNames[11]="O2";
		channelNames[12]="O1";
		channelNames[13]="FC5";
		for(int i=0;i<pipelineCount;i++)
		{
			pipelines[i]->initialize(128,14,channelNames,sampleRate);
		}
		break;
	default:
		break;
	}
}


