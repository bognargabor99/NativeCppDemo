#include <jni.h>
#include <stdio.h>
#include <math.h>
#include "Kiss_fft/kiss_fft.h"
#include "Kiss_fft/kiss_fftr.h"
#include "hu_bme_innolearn_hrm_HRMSignalProcessor.h"
#include "helpers.h"
/*
 * Class:     com_example_hellondk_HeartRateMonitorSignalProcessor
 * Method:    getMFPowerSpectrum
 * Signature: ([D)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_hu_bme_aut_adapted_framework_sensors_zephyr_HRMSignalProcessor_nativeCalcPSD
  (JNIEnv* pEnv, jobject thisObj, jdoubleArray inJNIArray) {

	// convert jni array to c array
	jboolean isCopy;
	jdouble* inCArray = pEnv->GetDoubleArrayElements(inJNIArray, &isCopy);
	jdoubleArray ret = NULL;

	// check if the conversion was successful
	if (inCArray != NULL) {

		// get the array length
		jint length = pEnv->GetArrayLength(inJNIArray);

		// do the processing of the signal
		jdouble* windowedValues = applyWindow(inCArray, length);
		jdouble* transformedValues = applyFFT(windowedValues, length);

		// set the return value
		ret = pEnv->NewDoubleArray(length);
		pEnv->SetDoubleArrayRegion(ret, 0, length, transformedValues);

		// release resources
		delete windowedValues;
		delete transformedValues;
		pEnv->ReleaseDoubleArrayElements(inJNIArray, inCArray, JNI_ABORT);

	}
	return ret;
}

jdouble* applyWindow(jdouble* values, jint len) {
	jdouble* ret = new jdouble[len];
	int i;
	if (len > 1) {
		for (i = 0; i < len; i++) {
			// Hanning windowing function
			ret[i] = values[i] * (0.54 - 0.46 * cos(2 * M_PI * i / (len - 1)));
		}
	}
	return ret;
}

jdouble* applyFFT(jdouble* values, jint len) {
	float* cValues = new float[len];
	for (int i = 0; i < len; i++) {
		cValues[i] = (float) values[i];
	}

	// prepare
	kiss_fft_cpx* spectrum = new kiss_fft_cpx[(int) len];
	kiss_fftr_cfg fft = kiss_fftr_alloc((int) len, 0, 0, 0);

	// fft
	kiss_fftr(fft, cValues, spectrum);

	// create return array
	jdouble* ret = new jdouble[len];
	//int k;
	for (int k = 0; k < len; k++) {
		ret[k] = sqrt(spectrum[k].r * spectrum[k].r + spectrum[k].i * spectrum[k].i);
	}
	return ret;
}
