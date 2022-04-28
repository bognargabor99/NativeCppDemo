/*
 * ExamplePipeline.h
 *
 *  Created on: 2013.03.18.
 *      Author: MegaStar
 */

#ifndef EXAMPLEPIPELINE_H_
#define EXAMPLEPIPELINE_H_

#include "SignalProcessorPipeline.h"
#include "SignalProcessors/FrequencyAnalysis.h"

class ExamplePipeline: public SignalProcessorPipeline {
public:
	ExamplePipeline();
	virtual ~ExamplePipeline();
	void initialize(int eegBufferSize,int channelCount,string* channelNames,int samplingRate);
};

#endif /* EXAMPLEPIPELINE_H_ */
