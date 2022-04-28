/*
 * FrequencyAnalysis.cpp
 *
 *  Created on: 2013.03.05.
 *      Author: MegaStar
 */

#include "FrequencyAnalysis.h"
#include "../Kiss_fft/kiss_fft.h"
#include "../Kiss_fft/kiss_fftr.h"
#include "../EventTypes.h"
#include <map>
#include "../logger.h"

FrequencyAnalysis::FrequencyAnalysis()
{
	usedChannelsCount = 14;
	usedChannelNames = new string[usedChannelsCount];
	usedChannelNames[0]="F3";
	usedChannelNames[1]="FC6";
	usedChannelNames[2]="P7";
	usedChannelNames[3]="T8";
	usedChannelNames[4]="F7";
	usedChannelNames[5]="F8";
	usedChannelNames[6]="T7";
	usedChannelNames[7]="P8";
	usedChannelNames[8]="AF4";
	usedChannelNames[9]="F4";
	usedChannelNames[10]="AF3";
	usedChannelNames[11]="O2";
	usedChannelNames[12]="O1";
	usedChannelNames[13]="FC5";
	type = EFrequencyAnalysis;
	usedChannelIds = new int[usedChannelsCount];
}

FrequencyAnalysis::~FrequencyAnalysis()
{
	delete[] usedChannelNames;
}

void FrequencyAnalysis::initialize(int eegBuffer,int channelCount,string* channelNames,int samplingRate)
{
	SignalProcessor::initialize(eegBuffer,channelCount,channelNames,samplingRate);
	int index =0;
	for(int i=0;i<channelCount;i++)
	{
		for(int j=0;j<usedChannelsCount;j++)
		{
			if(channelNames[i]==usedChannelNames[j])
			{
				usedChannelIds[index]=i;
				index++;
			}

		}
	}
	/*if(channelCount == 1)
		usedChannelIds[0]=0;//simulator
	else
		usedChannelIds[0] = 9;
		*/
	//usedChannelIds[0]=12;//->O1;
	//usedChannelIds[0]=9;//->F4
	LOGI("Frequency initialization done.");
}

EventTypes FrequencyAnalysis::process(ChannelSet* eegBuffer, JNIEnv* env, jobject caller)
{
		//float array[size] = {0.1, 0.6, 0.1, 0.4, 0.5, 0, 0.8, 0.7, 0.8, 0.6, 0.1, 0};
	//	kiss_fft_cpx spectrum[size];
	//	float out[size];

		double delta, theta, alpha, low_beta, midrange_beta, high_beta, gamma;

		for (int channelIterator = 0;channelIterator<usedChannelsCount;++channelIterator)
		{
			delta = 0, theta = 0, alpha = 0, low_beta = 0, midrange_beta = 0, high_beta = 0, gamma = 0;
			kiss_fft_cpx* spectrum = new kiss_fft_cpx[eegBufferSize];
			float* out = new float[eegBufferSize];
			float* in = new float[eegBufferSize];
			//LOGI("Used channel id: %d",usedChannelIds[0]);
			for(int i=0;i<eegBufferSize;i++)
			{
				//LOGE("** %f",eegBuffer[i].getChannelValue(usedChannelIds[0]));
				in[i]=eegBuffer[i].getChannelValue(usedChannelIds[channelIterator]);
			}


			double* magnitude = new double[eegBufferSize];
			double* frequency = new double[eegBufferSize];

			kiss_fftr_cfg fft = kiss_fftr_alloc(eegBufferSize,0,0,0);
			kiss_fftr_cfg ifft = kiss_fftr_alloc(eegBufferSize,1,0,0); // 1 for inverse fft

			// fft
			kiss_fftr(fft, in, spectrum);
			// ifft
			kiss_fftri(ifft, spectrum, out);

			map<float, float> psd;

			for(int k = 0; k < eegBufferSize; k++)
			{
				frequency[k] = k*samplingRate/eegBufferSize;
				magnitude[k] = sqrt(spectrum[k].r*spectrum[k].r + spectrum[k].i*spectrum[k].i);
				psd.insert(pair<float, float>(frequency[k], magnitude[k]));
			}

			//Array to store spectral energy
			double* spectralEnergy = new double[100];
			//Initializing
			for (int i=0;i<100;++i){
				spectralEnergy[i]=0;
			}

			for (map<float, float>::iterator it = psd.begin(); it != psd.end(); ++it) {

				if(it->first > 0.1 && it->first < 4)
				{
					delta += it->second;
				}
				else if(it->first >= 4 && it->first < 7)
				{
					theta += it->second;
				}
				else if(it->first >= 7 && it->first < 12)
				{
					alpha += it->second;
				}
				else if(it->first >= 12 && it->first < 15)
				{
					low_beta += it->second;
				}
				else if(it->first >= 15 && it->first < 20)
				{
					midrange_beta += it->second;
				}
				else if(it->first >= 20 && it->first < 30)
				{
					high_beta += it->second;
				}
				else if(it->first >= 30 && it->first < 100)
				{
					gamma += it->second;
				}

				//Assigning values
				for (int i=0;i<100;++i){
					if (it->first >= i && it->first < (i+1))
					spectralEnergy[i]+=it->second;
				}

			}

			kiss_fft_cleanup();
			free(fft);
			free(ifft);



			jdoubleArray outJNIArray = env->NewDoubleArray(100);  // allocate
			env->SetDoubleArrayRegion(outJNIArray,0,100,spectralEnergy);

			jclass resultClass = env->FindClass("hu/android/bme/innolearn/communication/ChannelResult");
			jmethodID contstructor = env->GetMethodID(resultClass,"<init>","(DDDDDDD[D)V");
			jobject channelResult= env->NewObject(resultClass,contstructor,delta,theta,alpha,low_beta,midrange_beta,high_beta,gamma,outJNIArray);


			jclass objectClass = env->GetObjectClass(caller);
			jmethodID objectMethod = env->GetMethodID(objectClass,"channelResultEvent","(Lhu/android/bme/innolearn/communication/ChannelResult;Ljava/lang/String;I)V");
			env->CallVoidMethod(caller,objectMethod,channelResult,env->NewStringUTF(usedChannelNames[channelIterator].data()),usedChannelIds[channelIterator]);

		}
		jclass objectClass = env->GetObjectClass(caller);
		jmethodID objectMethod = env->GetMethodID(objectClass,"frequencyAnalysisEvent","()V");
		env->CallVoidMethod(caller,objectMethod);
		return NoEvent;
}
