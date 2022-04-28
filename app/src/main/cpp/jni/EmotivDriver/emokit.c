/* Copyright (c) 2010, Daeken and Skadge
 * Copyright (c) 2011-2012, OpenYou Organization (http://openyou.org)
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <string.h>
#include "emokit.h"
#include "rijndael-128.c"

#define MAX_STR 255

#define EMOKIT_CONSUMER 0
#define EMOKIT_RESEARCH 1

/* ID of the feature report we need to identify the device
   as consumer/research */
#define EMOKIT_REPORT_ID 0
#define EMOKIT_REPORT_SIZE 9


const unsigned char F3_MASK[14] = {10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
const unsigned char FC6_MASK[14] = {214, 215, 200, 201, 202, 203, 204, 205, 206, 207, 192, 193, 194, 195};
const unsigned char P7_MASK[14] = {84, 85, 86, 87, 72, 73, 74, 75, 76, 77, 78, 79, 64, 65};
const unsigned char T8_MASK[14] = {160, 161, 162, 163, 164, 165, 166, 167, 152, 153, 154, 155, 156, 157};
const unsigned char F7_MASK[14] = {48, 49, 50, 51, 52, 53, 54, 55, 40, 41, 42, 43, 44, 45};
const unsigned char F8_MASK[14] = {178, 179, 180, 181, 182, 183, 168, 169, 170, 171, 172, 173, 174, 175};
const unsigned char T7_MASK[14] = {66, 67, 68, 69, 70, 71, 56, 57, 58, 59, 60, 61, 62, 63};
const unsigned char P8_MASK[14] = {158, 159, 144, 145, 146, 147, 148, 149, 150, 151, 136, 137, 138, 139};
const unsigned char AF4_MASK[14] = {196, 197, 198, 199, 184, 185, 186, 187, 188, 189, 190, 191, 176, 177};
const unsigned char F4_MASK[14] = {216, 217, 218, 219, 220, 221, 222, 223, 208, 209, 210, 211, 212, 213};
const unsigned char AF3_MASK[14] = {46, 47, 32, 33, 34, 35, 36, 37, 38, 39, 24, 25, 26, 27};
const unsigned char O2_MASK[14] = {140, 141, 142, 143, 128, 129, 130, 131, 132, 133, 134, 135, 120, 121};
const unsigned char O1_MASK[14] = {102, 103, 88, 89, 90, 91, 92, 93, 94, 95, 80, 81, 82, 83};
const unsigned char FC5_MASK[14] = {28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22, 23, 8, 9};
const unsigned char QUALITY_MASK[14]={99,100,101,102,103,104,105,106,107,108,109,110,111,112};



struct emokit_device* emokit_create()
{
	struct emokit_device* s = (struct emokit_device*)malloc(sizeof(struct emokit_device));
	memset(s,0,sizeof(struct emokit_device));
	s->_is_open = 0;
	s->_is_inited = 0;
	s->_is_inited = 1;
	return s;
}

EMOKIT_DECLSPEC int emokit_init_crypto(struct emokit_device* s, int dev_type) {
	emokit_get_crypto_key(s, dev_type);

	s->blocksize = _mcrypt_get_block_size(); //should return a 16bits blocksize

	s->block_buffer = (unsigned char *)malloc(s->blocksize);

	//rijandael initialization
	_mcrypt_set_key(&(s->td),s->key,EMOKIT_KEYSIZE);
	return 0;
}

EMOKIT_DECLSPEC
void emokit_get_crypto_key(struct emokit_device* s, int dev_type) {
	unsigned char type = (unsigned char) dev_type;
	int i;
	unsigned int l = 16;
	type &= 0xF;
	type = (type == 0);

	s->key[0] = (uint8_t)s->serial[l-1];
	s->key[1] = '\0';
	s->key[2] = (uint8_t)s->serial[l-2];
	if(type) {
		s->key[3] = 'H';
		s->key[4] = (uint8_t)s->serial[l-1];
		s->key[5] = '\0';
		s->key[6] = (uint8_t)s->serial[l-2];
		s->key[7] = 'T';
		s->key[8] = (uint8_t)s->serial[l-3];
		s->key[9] = '\x10';
		s->key[10] = (uint8_t)s->serial[l-4];
		s->key[11] = 'B';
	}
	else {
		s->key[3] = 'T';
		s->key[4] = (uint8_t)s->serial[l-3];
		s->key[5] = '\x10';
		s->key[6] = (uint8_t)s->serial[l-4];
		s->key[7] = 'B';
		s->key[8] = (uint8_t)s->serial[l-1];
		s->key[9] = '\0';
		s->key[10] = (uint8_t)s->serial[l-2];
		s->key[11] = 'H';
	}
	s->key[12] = (uint8_t)s->serial[l-3];
	s->key[13] = '\0';
	s->key[14] = (uint8_t)s->serial[l-4];
	s->key[15] = 'P';
}


void emokit_deinit(struct emokit_device* s) {
	//mcrypt_generic_deinit (s->td);
	//mcrypt_module_close(s->td);
}

double get_level(unsigned char frame[32], const unsigned char bits[14]) {
	signed char i;
	char b,o;
	int level = 0;

	for (i = 13; i >= 0; --i) {
		level <<= 1;
		b = (bits[i] >> 3) + 1;
		o = bits[i] % 8;

		level |= (frame[b] >> o) & 1;
	}
	//TODO: chack this later
	return level*0.51;
}

EMOKIT_DECLSPEC int emokit_get_next_raw(struct emokit_device* s) {
	//Two blocks of 16 bytes must be read.

	if (memcpy (s->block_buffer, s->raw_frame, s->blocksize)) {
		_mcrypt_decrypt(&s->td,s->block_buffer);
		memcpy(s->raw_unenc_frame, s->block_buffer, s->blocksize);
	}
	else {
		return -1;
	}

	if (memcpy(s->block_buffer, s->raw_frame + s->blocksize, s->blocksize)) {
		_mcrypt_decrypt(&s->td,s->block_buffer);
		memcpy(s->raw_unenc_frame + s->blocksize, s->block_buffer, s->blocksize);
	}
	else {
		return -1;
	}
	return 0;
}


//returns the percentage battery value given the unencrypted report value
EMOKIT_DECLSPEC unsigned char battery_value(unsigned char in) {

	if (in>=248) return 100;
	else {
		switch(in) {
		case 247:return 99; break;
		case 246:return 97; break;
		case 245:return 93; break;
		case 244:return 89; break;
		case 243:return 85; break;
		case 242:return 82; break;
		case 241:return 77; break;
		case 240:return 72; break;
		case 239:return 66; break;
		case 238:return 62; break;
		case 237:return 55; break;
		case 236:return 46; break;
		case 235:return 32; break;
		case 234:return 20; break;
		case 233:return 12; break;
		case 232:return 6; break;
		case 231:return 4 ; break;
		case 230:return 3; break;
		case 229:return 2; break;
		case 228:
		case 227:
		case 226:
			return 1;
			break;
		default:
			return 0;
		}
	}
}

//decode and update the s->last_quality, return s->last_quality
EMOKIT_DECLSPEC struct emokit_contact_quality handle_quality(struct emokit_device* s) {
	int current_contact_quality=get_level(s->raw_unenc_frame,QUALITY_MASK);
	switch(s->raw_unenc_frame[0]) {
	case 0:
		s->last_quality.F3=current_contact_quality;
		break;
	case 1:
		s->last_quality.FC5=current_contact_quality;
		break;
	case 2:
		s->last_quality.AF3=current_contact_quality;
		break;
	case 3:
		s->last_quality.F7=current_contact_quality;
		break;
	case 4:
		s->last_quality.T7=current_contact_quality;
		break;
	case 5:
		s->last_quality.P7=current_contact_quality;
		break;
	case 6:
		s->last_quality.O1=current_contact_quality;
		break;
	case 7:
		s->last_quality.O2=current_contact_quality;
		break;
	case 8:
		s->last_quality.P8=current_contact_quality;
		break;
	case 9:
		s->last_quality.T8=current_contact_quality;
		break;
	case 10:
		s->last_quality.F8=current_contact_quality;
		break;
	case 11:
		s->last_quality.AF4=current_contact_quality;
		break;
	case 12:
		s->last_quality.FC6=current_contact_quality;
		break;
	case 13:
		s->last_quality.F4=current_contact_quality;
		break;
	case 14:
		s->last_quality.F8=current_contact_quality;
		break;
	case 15:
		s->last_quality.AF4=current_contact_quality;
		break;
	case 64:
		s->last_quality.F3=current_contact_quality;
		break;
	case 65:
		s->last_quality.FC5=current_contact_quality;
		break;
	case 66:
		s->last_quality.AF3=current_contact_quality;
		break;
	case 67:
		s->last_quality.F7=current_contact_quality;
		break;
	case 68:
		s->last_quality.T7=current_contact_quality;
		break;
	case 69:
		s->last_quality.P7=current_contact_quality;
		break;
	case 70:
		s->last_quality.O1=current_contact_quality;
		break;
	case 71:
		s->last_quality.O2=current_contact_quality;
		break;
	case 72:
		s->last_quality.P8=current_contact_quality;
		break;
	case 73:
		s->last_quality.T8=current_contact_quality;
		break;
	case 74:
		s->last_quality.F8=current_contact_quality;
		break;
	case 75:
		s->last_quality.AF4=current_contact_quality;
		break;
	case 76:
		s->last_quality.FC6=current_contact_quality;
		break;
	case 77:
		s->last_quality.F4=current_contact_quality;
		break;
	case 78:
		s->last_quality.F8=current_contact_quality;
		break;
	case 79:
		s->last_quality.AF4=current_contact_quality;
		break;
	case 80:
		s->last_quality.FC6=current_contact_quality;
		break;
	default:
		break;
	}
	return (s->last_quality);
}


EMOKIT_DECLSPEC
struct emokit_frame emokit_get_next_frame(struct emokit_device* s) {
	struct emokit_frame k;
	memset(s->raw_unenc_frame, 0, 32);

	if (emokit_get_next_raw(s)<0) {
		k.counter=0;
		return k;
	}

	memset(&k.cq,0,sizeof(struct emokit_contact_quality));
	if (s->raw_unenc_frame[0] & 128) {
		k.counter = 128;
		k.battery = battery_value( s->raw_unenc_frame[0] );
		s->last_battery=k.battery;
	} else {
		k.counter = s->raw_unenc_frame[0];
		k.battery = s->last_battery;
	}
	k.F3 = get_level(s->raw_unenc_frame, F3_MASK);
	k.FC6 = get_level(s->raw_unenc_frame, FC6_MASK);
	k.P7 = get_level(s->raw_unenc_frame, P7_MASK);
	k.T8 = get_level(s->raw_unenc_frame, T8_MASK);
	k.F7 = get_level(s->raw_unenc_frame, F7_MASK);
	k.F8 = get_level(s->raw_unenc_frame, F8_MASK);
	k.T7 = get_level(s->raw_unenc_frame, T7_MASK);
	k.P8 = get_level(s->raw_unenc_frame, P8_MASK);
	k.AF4 = get_level(s->raw_unenc_frame, AF4_MASK);
	k.F4 = get_level(s->raw_unenc_frame, F4_MASK);
	k.AF3 = get_level(s->raw_unenc_frame, AF3_MASK);
	k.O2 = get_level(s->raw_unenc_frame, O2_MASK);
	k.O1 = get_level(s->raw_unenc_frame, O1_MASK);
	k.FC5 = get_level(s->raw_unenc_frame, FC5_MASK);

	k.gyroX = s->raw_unenc_frame[29] - 102;
	k.gyroY = s->raw_unenc_frame[30] - 104;

	k.cq=handle_quality(s);
	//memcpy(k.unenc,s->raw_unenc_frame,32);

	return k;
}

EMOKIT_DECLSPEC void emokit_delete(struct emokit_device* dev)
{
	emokit_deinit(dev);
	free(dev);
}

//jstring Java_com_innolearn_emotivdriverlib_EmotivDriver_valami( JNIEnv* env,
//		jobject thiz )
//{
//	jobject retObj;
//		jmethodID constructor;
//		jobject cls;
//		retObj = (*env)->NewObject(env, emotivClass, emotivConstructor, 0,0, 0, 0, 0, 0,
//				0, 0, 0, 0, 0, 0, 0, 0,
//				0, 0, 0, 0, 0, 0,
//				0, 0, 0, 0, 0, 0, 0,
//				0,0, 0, 0, 0);
//
//	return (*env)->NewStringUTF(env, "Teszt");
//}


