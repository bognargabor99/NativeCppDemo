/*
 * FrequencyAnalysis.h
 *
 *  Created on: 2013.03.05.
 *      Author: MegaStar
 */

#ifndef FREQUENCYANALYSIS_H_
#define FREQUENCYANALYSIS_H_

#include "SignalProcessor.h"
#include <jni.h>

using namespace std;


class FrequencyAnalysis: public SignalProcessor {
public:
	FrequencyAnalysis();
	virtual ~FrequencyAnalysis();
	EventTypes process(ChannelSet* eegBuffer, JNIEnv* env, jobject caller);
	void initialize(int eegBufferSize, int channelCount, string* channelNames, int samplingRate);
};

#endif /* FREQUENCYANALYSIS_H_ */
