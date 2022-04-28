/*
 * ChannelSet.h
 *
 *  Created on: 2013.02.25.
 *      Author: MegaStar
 */

#ifndef CHANNELSET_H_
#define CHANNELSET_H_

#include "ChannelQualities.h"
#include <stdio.h>


class ChannelSet
{

private:
	double* channelValue;
	ChannelQuality* channelQuality;
	int current;
	int size;
public:
	ChannelSet(int size=1);
	virtual ~ChannelSet();

	void addChannel(double value, ChannelQuality quality);
	double getChannelValue(int id);
};

#endif /* CHANNELSET_H_ */
