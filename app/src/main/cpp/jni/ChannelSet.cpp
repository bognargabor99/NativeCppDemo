/*
 * ChannelSet.cpp
 *
 *  Created on: 2013.02.25.
 *      Author: MegaStar
 */

#include "ChannelSet.h"

ChannelSet::ChannelSet(int setSize) {
	size=setSize;
	current=0;
	channelValue=NULL;
	channelQuality=NULL;
	channelValue = new double[size];
	channelQuality = new ChannelQuality[size];
}

ChannelSet::~ChannelSet() {
	// TODO Auto-generated destructor stub
	if(channelValue)
	{
	delete [] channelValue;
	}
	if(channelQuality)
	{
	delete [] channelQuality;
	}
}

void ChannelSet::addChannel(double value, ChannelQuality quality) {
	channelValue[current]=value;
	channelQuality[current]=quality;
	current++;
}

double ChannelSet::getChannelValue(int id)
{
	return channelValue[id];
}
