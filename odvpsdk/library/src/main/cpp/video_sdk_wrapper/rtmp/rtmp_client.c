#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "rtmp.h"
#include "rtmp_client.h"
#include <string.h>
#include "device.h"
#include "odvp_client.h"
#include "android_log.h"

static const char h264_delimiter[] = { 0x01, 0, 0, 0 };

static FILE *g_file_handle;

void open_flv_file(const char *filename) {
    if (NULL == filename) {
        LOGE("%s error: filename is null", __FUNCTION__);
        return;
    }

    g_file_handle = fopen(filename, "wb");

    if (g_file_handle == NULL) {
        LOGE("%s error: open %s failed", __FUNCTION__, filename);
    }

    return;
}

void close_flv_file() {
    if (g_file_handle) {
        fclose(g_file_handle);
    }
}

void write_flv_header(bool is_have_audio, bool is_have_video) {
    char flv_file_header[] = "FLV\x1\x5\0\0\0\x9\0\0\0\0"; // have audio and have video

    if (is_have_audio && is_have_video) {
        flv_file_header[4] = 0x05;
    } else if (is_have_audio && !is_have_video) {
        flv_file_header[4] = 0x04;
    } else if (!is_have_audio && is_have_video) {
        flv_file_header[4] = 0x01;
    } else {
        flv_file_header[4] = 0x00;
    }

    fwrite(flv_file_header, 13, 1, g_file_handle);

    return;
}

unsigned char make_audio_headerTag(unsigned int format, unsigned int samplerate, unsigned int samplesize, unsigned int sampletype) {

    return rtmp_make_audio_headerTag(format, samplerate, samplesize, sampletype);
}

int send_spspps(RTMP* rtmp, unsigned char * sps, int sps_len, unsigned char *pps, int pps_len, unsigned int ts){

    return rtmp_send_spspps(rtmp, sps, sps_len, pps, pps_len, ts);
}

int send_metadata(RTMP* rtmp, RTMPMetadata* metadata) {

    return rtmp_send_metadata(rtmp, metadata);
}

long open_stream(const char *url, const char * device_id) {

    RTMP* rtmp = rtmp_create_publishstream(url, 10, device_id);
    if (rtmp == NULL) {

        return 0;
    }

    return (long)rtmp;
}

int check_rcv_handler(RTMP* _r){

    return rtmp_check_rcv_handler(_r);
}

int send_video_data(RTMP* rtmp, unsigned char *data, unsigned int size, unsigned int ts, unsigned int key_frame) {

    if (rtmp == NULL) {

        return -1;
    }

    RTMPPacket packet;
    RTMPPacket_Reset(&packet);
    if (!RTMPPacket_Alloc(&packet, size + 9 + 8)) /*reserve some data*/
    {
        LOGE("alloc packet err, body size %d\n", size);
        return -1;
    }

    packet.m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet.m_nChannel = 0x04;
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_hasAbsTimestamp = 0;
    packet.m_nTimeStamp = ts;
    packet.m_nInfoField2 = rtmp->m_stream_id;
    packet.m_nBodySize += size + 9;

    int flagIndex = 0;
    if (key_frame) {

        packet.m_body[flagIndex] = 0x17;
    } else {

        packet.m_body[flagIndex] = 0x27;
    }
    flagIndex++;
    memcpy(packet.m_body + flagIndex, h264_delimiter, 4);
    flagIndex += 4;
    packet.m_body[flagIndex] = (uint8_t)(size >> 24);
    flagIndex++;
    packet.m_body[flagIndex] = (uint8_t)(size >> 16);
    flagIndex++;
    packet.m_body[flagIndex] = (uint8_t)(size >> 8);
    flagIndex++;
    packet.m_body[flagIndex] = (uint8_t)(size);
    flagIndex++;
    memcpy(packet.m_body + flagIndex, data, size);

    int nRet = RTMP_SendPacket(rtmp, &packet, 0, key_frame?0:1);
    RTMPPacket_Free(&packet);
    return nRet == TRUE ? 0:-1;
}

int send_audio_data(RTMP* rtmp, unsigned char header_tag, unsigned char *data, unsigned int size, unsigned int ts, bool config_data) {

    if (rtmp == NULL)
    {
        return -1;
    }

    if (config_data) {

        return internal_send_audio_data(rtmp, header_tag, data, 2, 0, RTMP_PACKET_SIZE_LARGE, config_data);
    } else {

        return internal_send_audio_data(rtmp, header_tag, data, size, ts, RTMP_PACKET_SIZE_MEDIUM, config_data);
    }
}

int internal_send_audio_data(RTMP* rtmp, unsigned char header_tag, unsigned char *data, unsigned int size, unsigned int ts, unsigned int headertype, bool config_data) {

    RTMPPacket packet;

    if (rtmp == NULL) {
        return -1;
    }

    RTMPPacket_Reset(&packet);
    RTMPPacket_Alloc(&packet, size + 2);

    packet.m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet.m_nChannel = 0x06;
    packet.m_hasAbsTimestamp = 0;
    packet.m_headerType = headertype;
    packet.m_nTimeStamp = ts;
    packet.m_nInfoField2 = rtmp->m_stream_id;

    packet.m_body[0] = header_tag;
    packet.m_nBodySize++;

    if (config_data) {
        packet.m_body[1] = 0x00;
    } else {
        packet.m_body[1] = 0x01;
    }
    packet.m_nBodySize++;

    memcpy(packet.m_body+2, data, size);
    packet.m_nBodySize += size;

    int nRet = RTMP_SendPacket(rtmp, &packet, 0, 0);
    RTMPPacket_Free(&packet);
    return nRet==TRUE?0:-1;
}

void close_stream(RTMP* rtmp) {

    if (rtmp == NULL) {

        return;
    }
    rtmp_stop_publishstream(rtmp);
}