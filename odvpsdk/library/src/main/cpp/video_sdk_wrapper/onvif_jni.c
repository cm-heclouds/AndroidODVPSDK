#include <jni.h>
#include "device.h"
#include "odvp/odvp_client.h"
#include "onvif_odvp.h"
#include "onvif/onvif_config.h"
#include "utils/j4a_base.h"
#include "platform_cmd.h"

extern int32_t _ont_cmd_live_stream_ctrl(void *dev, int32_t channel, int32_t level);
extern int32_t _ont_cmd_dev_ptz_ctrl(void *dev, int32_t channel, int32_t mode, t_ont_video_ptz_cmd ptz, int32_t speed);
extern int ont_video_live_stream_rtmp_publish(ont_device_t *dev, int32_t channel, uint8_t protype, uint16_t ttl_min, const char *push_url);

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOnvif_nativeAddChannel(JNIEnv *env, jobject instance, jlong dev, jstring config_buf_) {

    const char *config_buf = J4A_GetStringUTFChars(env, config_buf_, 0);
    device_cfg_initilize(config_buf);
    ont_add_onvif_devices((ont_device_t *)dev);
    ont_video_dev_channel_sync((ont_device_t *)dev);
    J4A_ReleaseStringUTFChars(env, config_buf_, config_buf);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOnvif_nativeLiveStreamPlay(JNIEnv *env, jobject instance, jlong dev, jint channel, jbyte pro_type, jint ttl_min, jstring push_url_) {

    const char *push_url = J4A_GetStringUTFChars(env, push_url_, 0);
    int result = ont_video_live_stream_rtmp_publish((ont_device_t *)dev, channel, pro_type, ttl_min, push_url);
    J4A_ReleaseStringUTFChars(env, push_url_, push_url);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOnvif_nativeLiveStreamCtrl(JNIEnv *env, jobject instance, jlong dev, jint channel, jint level) {

    return _ont_cmd_live_stream_ctrl((ont_device_t *)dev, channel, level);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOnvif_nativeDevPtzCtrl(JNIEnv *env, jobject instance, jlong dev, jint channel, jint mode, jint cmd, jint speed) {

    return _ont_cmd_dev_ptz_ctrl((ont_device_t *)dev, channel, mode, cmd, speed);
}

