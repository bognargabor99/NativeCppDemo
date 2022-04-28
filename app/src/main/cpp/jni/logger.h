/*
 * logger.h
 *
 *  Created on: 2013.03.18.
 *      Author: MegaStar
 */

#ifndef LOGGER_H_
#define LOGGER_H_

#include <android/log.h>
#define LOG_TAG "NDK Debug message"

int semmi(int prio, const char *tag,  const char *fmt, ...);

#define  LOGI(...)semmi(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)__android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)



#endif /* LOGGER_H_ */
