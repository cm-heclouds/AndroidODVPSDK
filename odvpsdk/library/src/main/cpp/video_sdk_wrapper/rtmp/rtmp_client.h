//
// Created by faraklit on 08.02.2016.
//

#ifndef _XIECC_RTMP_H_
#define _XIECC_RTMP_H_
#include <stdint.h>
#include <stdbool.h>
#include <device.h>
#include "rtmp.h"
#include "rtmp_if.h"

#ifdef __cplusplus
extern "C"{
#endif

int send_metadata(RTMP* rtmp, RTMPMetadata* metadata);

unsigned char make_audio_headerTag(unsigned int format, unsigned int samplerate, unsigned int samplesize, unsigned int sampletype);

int send_spspps(RTMP* rtmp, unsigned char * sps, int sps_len, unsigned char *pps, int pps_len, unsigned int ts);

long open_stream(const char *url, const char * device_id);

int check_rcv_handler(RTMP* _r);

int send_video_data(RTMP* rtmp, unsigned char *data, unsigned int size, unsigned int ts, unsigned int key_frame);

int send_audio_data(RTMP* rtmp, unsigned char header_tag, unsigned char *data, unsigned int size, unsigned int ts, bool config_data);

void close_stream(RTMP* rtmp);

void open_flv_file(const char *filename);

void close_flv_file();

void write_flv_header(bool is_have_audio, bool is_have_video);

int internal_send_audio_data(RTMP* rtmp, unsigned char header_tag, unsigned char *data, unsigned int size, unsigned int ts, unsigned int headertype, bool config_data);

#ifdef __cplusplus
}
#endif
#endif
