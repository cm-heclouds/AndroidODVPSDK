#include <malloc.h>
#include <jni.h>
#include <stddef.h>
#include <string.h>
#include "device.h"
#include "odvp/odvp_client.h"
#include "j4a_base.h"
#include "amf.h"
#include "rtmp.h"
#include "rtmp_client.h"

#ifdef __cplusplus
extern "C" {
#endif

void sound_notify_cb(void* ctx, int chunk, int ts, const char *data, int len);
static JavaVM *jvm;
struct OntStream_ref{

    jclass id;
    jmethodID method_onSoundNotify;
};
struct OntStream_ref class_OntStream;

JNIEXPORT jbyte JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeMakeAudioHeaderTag(JNIEnv *env, jobject instance, jint format, jint sample_rate, jint sample_size, jint sample_type) {

    return make_audio_headerTag(format, sample_rate, sample_size, sample_type);
}

JNIEXPORT jlong JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeOpenStream(JNIEnv *env, jobject instance, jstring url_, jstring device_id_) {

    const char *url = J4A_GetStringUTFChars(env, url_, 0);
    const char *device_id = J4A_GetStringUTFChars(env, device_id_, 0);
    long rtmpPointer = open_stream(url, device_id);
    J4A_ReleaseStringUTFChars(env, url_, url);
    J4A_ReleaseStringUTFChars(env, device_id_, device_id);
    return rtmpPointer;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeCheckRcvHandler(JNIEnv *env, jobject instance, jlong rtmpPointer){

    return check_rcv_handler((RTMP*)rtmpPointer);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeSendMetadata(JNIEnv *env, jobject instance, jlong rtmpPointer, jobject metadata_) {

    jclass cls_objClass = (*env)->GetObjectClass(env, metadata_);
    RTMPMetadata* metadata = ont_platform_malloc(sizeof(RTMPMetadata));
    memset(metadata, 0x00, sizeof(RTMPMetadata));
    metadata->height = J4A_GetIntField(env, cls_objClass, "height", metadata_);
    metadata->width = J4A_GetIntField(env, cls_objClass, "width", metadata_);
    metadata->audioCodecid = J4A_GetIntField(env, cls_objClass, "audioCodecid", metadata_);
    metadata->audioDataRate = J4A_GetIntField(env, cls_objClass, "audioDataRate", metadata_);
    metadata->audioSampleRate = J4A_GetIntField(env, cls_objClass, "audioSampleRate", metadata_);
    metadata->duration = J4A_GetDoubleField(env, cls_objClass, "duration", metadata_);
    metadata->frameRate = J4A_GetIntField(env, cls_objClass, "frameRate", metadata_);
    metadata->hasAudio = J4A_GetBooleanField(env, cls_objClass, "hasAudio", metadata_);
    metadata->videoCodecid = J4A_GetIntField(env, cls_objClass, "videoCodecid", metadata_);
    metadata->videoDataRate = J4A_GetIntField(env, cls_objClass, "videoDataRate", metadata_);

    int result = send_metadata((RTMP*)rtmpPointer, metadata);
    ont_platform_free(metadata);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeSendSpspps(JNIEnv *env, jobject instance, jlong rtmpPointer, jbyteArray sps_, jint sps_offset, jint sps_len, jbyteArray pps_, jint pps_offset, jint pps_len, jlong ts) {

    jbyte *sps = (*env)->GetByteArrayElements(env, sps_, NULL);
    jbyte *pps = (*env)->GetByteArrayElements(env, pps_, NULL);
    int result = send_spspps((RTMP*)rtmpPointer, sps + sps_offset, sps_len, pps + pps_offset, pps_len, ts);
    (*env)->ReleaseByteArrayElements(env, sps_, sps, 0);
    (*env)->ReleaseByteArrayElements(env, pps_, pps, 0);
    return result;
}


JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeSendVideoData(JNIEnv *env, jobject instance, jlong rtmpPointer, jbyteArray data_, jint offset, jint size, jlong ts, jint key_frame) {

    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);
    int result = send_video_data((RTMP*)rtmpPointer, data + offset, size, ts, key_frame);
    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeSendAudioData(JNIEnv *env, jobject instance, jlong rtmpPointer, jbyte header_tag, jbyteArray data_, jint size, jlong ts, jboolean config_data) {

    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);
    int result = send_audio_data((RTMP*)rtmpPointer, header_tag, data, size, ts, config_data);
    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return result;
}

void Java_com_ont_media_odvp_OntRtmp_nativeCloseStream(JNIEnv *env, jobject instance, jlong rtmpPointer) {

    RTMP *rtmp = (RTMP*) rtmpPointer;
    if (rtmp != NULL && rtmp->soundCtx != NULL) {

        J4A_DeleteGlobalRef(env, rtmp->soundCtx);
        rtmp->soundCtx = NULL;
    }
    close_stream(rtmp);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeWrite(JNIEnv * env, jobject thiz, jlong rtmpPointer, jbyteArray data, jint size) {

    RTMP *rtmp = (RTMP *) rtmpPointer;
    if (rtmp == NULL) {
        return -1;
    }

    int connected = RTMP_IsConnected(rtmp);
    if (!connected) {
        return -1;
    }

    return RTMP_Write(rtmp, data, size);
}

JNIEXPORT jboolean JNICALL
Java_com_ont_media_odvp_OntRtmp_nativePause(JNIEnv * env, jobject thiz, jlong rtmpPointer, jboolean pause) {

    RTMP *rtmp = (RTMP *) rtmpPointer;
    if (rtmp == NULL) {
        return FALSE;
    }

    int DoPause = 0;
    if (pause == JNI_TRUE) {
        DoPause = 1;
    }
    return RTMP_Pause(rtmp, DoPause);
}

JNIEXPORT jboolean JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeIsConnected(JNIEnv *env, jobject instance, jlong rtmpPointer)
{
    RTMP *rtmp = (RTMP *) rtmpPointer;
    if (rtmp == NULL) {
        return FALSE;
    }
     int connected = RTMP_IsConnected(rtmp);
     if (connected) {
        return TRUE;
     }
     else {
        return FALSE;
     }
}

extern int ont_video_playlist_singlestep(void *dev);
extern int32_t ont_vod_start_notify(ont_device_t *dev, const char* location, int32_t channel, uint8_t protype, const char *playflag, const char *pushurl, uint16_t ttl);

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativePlayListSingleStep(JNIEnv *env, jobject instance, jlong dev) {

    return ont_video_playlist_singlestep((ont_device_t *)dev);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeVodStreamPlay(JNIEnv *env, jobject instance, jlong dev, jstring location_, jint channel_id, jbyte pro_type, jstring play_flag_, jstring push_url_, jint ttl) {

    const char *location = J4A_GetStringUTFChars(env, location_, 0);
    const char *play_flag = J4A_GetStringUTFChars(env, play_flag_, 0);
    const char *push_url = J4A_GetStringUTFChars(env, push_url_, 0);
    int result = ont_vod_start_notify((ont_device_t *)dev, location, channel_id, pro_type, play_flag, push_url, ttl);
    J4A_ReleaseStringUTFChars(env, location_, location);
    J4A_ReleaseStringUTFChars(env, play_flag_, play_flag);
    J4A_ReleaseStringUTFChars(env, push_url_, push_url);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntRtmp_nativeSetSoundNotify(JNIEnv *env, jobject instance, jlong rtmp_point, jobject weak_this) {

    jint ret = (*env)->GetJavaVM(env, &jvm);
    if (ret != JNI_OK) {
        return ret;
    }

    if (class_OntStream.id == NULL) {

        ret                                  = -1;
        const char *J4A_UNUSED(name)      = NULL;
        const char *J4A_UNUSED(sign)      = NULL;
        jclass      J4A_UNUSED(class_id)   = NULL;

        sign = "com/ont/media/odvp/OntSoundCbWrapper";
        class_OntStream.id = J4A_FindClass__asGlobalRef__catchAll(env, sign);
        if (class_OntStream.id == NULL)
            goto fail;

        class_id = class_OntStream.id;
        name     = "onSoundNotify";
        sign     = "(Ljava/lang/Object;II[BI)V";
        class_OntStream.method_onSoundNotify = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntStream.method_onSoundNotify == NULL)
            goto fail;
    }

    RTMP* rtmp = (RTMP *) rtmp_point;
    if (rtmp->soundCtx != NULL) {

        J4A_DeleteGlobalRef(env, rtmp->soundCtx);
        rtmp->soundCtx = NULL;
    }
    return rtmp_set_sound_notify((void*)rtmp_point, sound_notify_cb, J4A_NewGlobalRef__catchAll(env, weak_this));

fail:
    return ret;
}

void sound_notify_cb(void* ctx, int chunk, int ts, const char *data, int len) {

    if (class_OntStream.method_onSoundNotify == NULL) {
        return;
    }

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        return;
    }

    jbyteArray bytes = 0;
    bytes = J4A_NewByteArray__catchAll(env, len);
    if (bytes != NULL) {

        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *)data);
    }

    jobject weak_this = (jobject) ctx;
    (*env)->CallStaticVoidMethod(env, class_OntStream.id, class_OntStream.method_onSoundNotify, weak_this, chunk, ts, bytes, len);
    J4A_DeleteLocalRef(env, bytes);
}

#ifdef __cplusplus
}
#endif
