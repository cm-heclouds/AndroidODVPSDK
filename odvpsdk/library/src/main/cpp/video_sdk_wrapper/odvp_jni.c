#include <jni.h>
#include <malloc.h>
#include <string.h>
#include <stdlib.h>
#include "ont_bytes.h"
#include "device.h"
#include "protocol.h"
#include "platform_cmd.h"
#include "odvp_client.h"
#include "android_log.h"
#include "j4a_base.h"

static JavaVM *jvm;
struct OntDeviceCb_ref {

    jclass id;

    jmethodID method_OnKeepAliveResp;
    jmethodID method_OnLiveStreamStart;
    jmethodID method_OnVodStreamStart;
    jmethodID method_OnChannelRecordUpdate;
    jmethodID method_OnUserDefinedCmd;
    jmethodID method_OnApiDefinedMsg;
    //jmethodID method_OnPlatRespDevPushStreamMsg;
};
struct OntCmdCb_ref {

    jclass id;

    jmethodID method_OnCmdLiveStreamCtrl;
    jmethodID method_OnCmdPtzCtrl;
    jmethodID method_OnCmdQueryFiles;
};
struct OntDeviceAuthRet_ref {

    jclass id;

    jmethodID method_Constructor;
    jfieldID field_Ret;
    jfieldID field_AuthCode;
};

struct OntDeviceCb_ref class_OntDeviceCb;
struct OntCmdCb_ref class_OntCmdCb;
struct OntDeviceAuthRet_ref class_OntDeviceAuthRet;

void on_keepalive_resp(ont_device_t *dev);
int32_t on_live_stream_start(ont_device_t *dev, int32_t channel, uint8_t protype, uint16_t ttl_min,
                         const char *push_url);
int32_t on_vod_stream_start(ont_device_t *dev, int32_t channel, uint8_t protype,
                            ont_video_file_t *fileinfo, const char *playflag, const char *pushurl,
                            uint16_t ttl);
int32_t on_cmd_channel_record_update(void *dev, int32_t channel, int32_t status, int32_t seconds,
                                     char *url);
int32_t on_user_defined_cmd(ont_device_t *dev, ont_device_cmd_t *cmd);
int32_t on_api_defined_msg(ont_device_t *dev, char *msg, size_t msg_len);
int32_t on_plat_resp_dev_push_stream_msg(ont_device_t *dev, ont_plat_resp_dev_ps_t *resp);
ont_device_callback_t _g_device_cbs =
        {
                on_keepalive_resp,
                on_live_stream_start,
                on_vod_stream_start,
                on_cmd_channel_record_update,
                on_user_defined_cmd,
                on_api_defined_msg,
                on_plat_resp_dev_push_stream_msg
        };

int32_t on_cmd_live_stream_ctrl(void *dev, int32_t channel, int32_t level);
int32_t on_cmd_ptz_ctrl(void *dev, int32_t channel, int32_t mode, t_ont_video_ptz_cmd ptz,
                        int32_t speed);
int32_t on_cmd_query_files(void *dev, int32_t channel, int32_t page, int32_t startindex, int32_t max, const char *starTime, const char *endTime, char *uuid);
ont_cmd_callbacks_t _g_ont_cmd =
        {
                on_cmd_live_stream_ctrl,
                on_cmd_ptz_ctrl,
                on_cmd_query_files
        };

JNIEXPORT jstring JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeGetDeviceId(JNIEnv *env, jobject instance, jlong dev_) {

    if (dev_ == 0) {

        return NULL;
    }

    ont_device_t * dev = (ont_device_t *)dev_;
    if (dev->device_id == 0) {

        return NULL;
    }

    char device_id[256];
    memset(device_id, 0, 256);
    sprintf(device_id, "%llu", dev->device_id);
    return (*env)->NewStringUTF(env, device_id);
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeGetAuthCode(JNIEnv *env, jobject instance, jlong dev_) {

    if (dev_ == 0) {

        return NULL;
    }

    ont_device_t * dev = (ont_device_t *)dev_;
    int auth_code_length = strlen(dev->auth_code);
    if (auth_code_length <= 0) {

        return NULL;
    }

    jbyteArray auth_code_ = (*env)->NewByteArray(env, auth_code_length);
    if (auth_code_ != NULL) {
        (*env)->SetByteArrayRegion(env, auth_code_, 0, auth_code_length, (jbyte *)dev->auth_code);
    }

    return auth_code_;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeSetDeviceId(JNIEnv *env, jobject instance, jlong dev_, jstring device_id_) {

    if (dev_ == 0 || device_id_ == NULL) {

        return -1;
    }

    ont_device_t * dev = (ont_device_t*)dev_;
    const char *device_id = J4A_GetStringUTFChars(env, device_id_, 0);
    dev->device_id = strtoull(device_id, NULL, 10);
    J4A_ReleaseStringUTFChars(env, device_id_, device_id);

    return ONT_ERR_OK;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeSetAuthCode(JNIEnv *env, jobject instance, jlong dev_, jbyteArray auth_code_) {

    if (dev_ == 0 || auth_code_ == NULL) {

        return -1;
    }

    jbyte *auth_code = (*env)->GetByteArrayElements(env, auth_code_, 0);
    int auth_code_length = (*env)->GetArrayLength(env, auth_code_);
    ont_device_t * dev = (ont_device_t*)dev_;
    memset(dev->auth_code, 0, 256);
    memcpy(dev->auth_code, auth_code, auth_code_length);
    (*env)->ReleaseByteArrayElements(env, auth_code_, auth_code, 0);

    return ONT_ERR_OK;
}

JNIEXPORT jlong JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceCreate(JNIEnv *env, jobject instance, jobject device_cb_weak_this, jobject cmd_cb_weak_this) {

    if ((*env)->GetJavaVM(env, &jvm) != JNI_OK) {

        LOGE("%s error: GetJavaVM failed\n", __FUNCTION__);
        return 0;
    }

    const char *J4A_UNUSED(name)      = NULL;
    const char *J4A_UNUSED(sign)      = NULL;
    jclass      J4A_UNUSED(class_id)   = NULL;

    if (class_OntDeviceCb.id == NULL) {

        sign = "com/ont/media/odvp/OntDeviceCbWrapper";
        class_OntDeviceCb.id = J4A_FindClass__asGlobalRef__catchAll(env, sign);
        if (class_OntDeviceCb.id == NULL)
            goto fail;

        class_id = class_OntDeviceCb.id;
        name     = "onKeepAliveResp";
        sign     = "(Ljava/lang/Object;)V";
        class_OntDeviceCb.method_OnKeepAliveResp = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnKeepAliveResp == NULL)
            goto fail;

        class_id = class_OntDeviceCb.id;
        name     = "onLiveStreamStart";
        sign     = "(Ljava/lang/Object;IBILjava/lang/String;)I";
        class_OntDeviceCb.method_OnLiveStreamStart = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnLiveStreamStart == NULL)
            goto fail;

        class_id = class_OntDeviceCb.id;
        name     = "onVodStreamStart";
        sign     = "(Ljava/lang/Object;IBLcom/ont/media/odvp/model/VideoFileInfo;Ljava/lang/String;Ljava/lang/String;I)I";
        class_OntDeviceCb.method_OnVodStreamStart = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnVodStreamStart == NULL)
            goto fail;

        class_id = class_OntDeviceCb.id;
        name     = "onChannelRecordUpdate";
        sign     = "(Ljava/lang/Object;IIILjava/lang/String;)I";
        class_OntDeviceCb.method_OnChannelRecordUpdate = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnChannelRecordUpdate == NULL)
            goto fail;

        class_id = class_OntDeviceCb.id;
        name     = "onUserDefinedCmd";
        sign     = "(Ljava/lang/Object;Lcom/ont/media/odvp/model/PlatformCmd;)I";
        class_OntDeviceCb.method_OnUserDefinedCmd = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnUserDefinedCmd == NULL)
            goto fail;

        class_id = class_OntDeviceCb.id;
        name     = "onApiDefinedMsg";
        sign     = "(Ljava/lang/Object;[BI)I";
        class_OntDeviceCb.method_OnApiDefinedMsg = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnApiDefinedMsg == NULL)
            goto fail;

        /*class_id = class_OntDeviceCb.id;
        name     = "onPlatRespDevPushStreamMsg";
        sign     = "(Ljava/lang/Object;Lcom/ont/media/odvp/model/PlatRespDevPS;)I";
        class_OntDeviceCb.method_OnPlatRespDevPushStreamMsg = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceCb.method_OnPlatRespDevPushStreamMsg == NULL)
            goto fail;*/
    }

    if (class_OntCmdCb.id == NULL) {

        sign = "com/ont/media/odvp/OntCmdCbWrapper";
        class_OntCmdCb.id = J4A_FindClass__asGlobalRef__catchAll(env, sign);
        if (class_OntCmdCb.id == NULL)
            goto fail;

        class_id = class_OntCmdCb.id;
        name     = "onCmdLiveStreamCtrl";
        sign     = "(Ljava/lang/Object;II)I";
        class_OntCmdCb.method_OnCmdLiveStreamCtrl = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntCmdCb.method_OnCmdLiveStreamCtrl == NULL)
            goto fail;

        class_id = class_OntCmdCb.id;
        name     = "onCmdPtzCtrl";
        sign     = "(Ljava/lang/Object;IIII)I";
        class_OntCmdCb.method_OnCmdPtzCtrl = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntCmdCb.method_OnCmdPtzCtrl == NULL)
            goto fail;

        class_id = class_OntCmdCb.id;
        name     = "onCmdQueryFiles";
        sign     = "(Ljava/lang/Object;IIIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)I";
        class_OntCmdCb.method_OnCmdQueryFiles = J4A_GetStaticMethodID__catchAll(env, class_id, name, sign);
        if (class_OntCmdCb.method_OnCmdQueryFiles == NULL)
            goto fail;
    }

    ont_device_t * dev = device_create(&_g_device_cbs, (*env)->NewGlobalRef(env, device_cb_weak_this));
    if (dev == NULL) {

        goto fail;
    }
    ont_device_set_platformcmd_handle(dev, &_g_ont_cmd, (*env)->NewGlobalRef(env, cmd_cb_weak_this));

fail:
    return ((jlong)dev);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceConnect(JNIEnv *env, jobject instance, jlong dev, jstring device_id_, jstring ip_, jstring port_) {

    const char *device_id = J4A_GetStringUTFChars(env, device_id_, 0);
    const char *ip = J4A_GetStringUTFChars(env, ip_, 0);
    const char *port = J4A_GetStringUTFChars(env, port_, 0);
    int ret = device_connect_by_addr((ont_device_t *)dev, strtoull(device_id, NULL, 10), ip, strtoull(port, NULL, 10));
    J4A_ReleaseStringUTFChars(env, device_id_, device_id);
    J4A_ReleaseStringUTFChars(env, ip_, ip);
    J4A_ReleaseStringUTFChars(env, port_, port);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceRequestRsaPublicKey(JNIEnv *env, jobject instance, jlong dev) {
    return ont_device_request_rsa_publickey((ont_device_t*)dev);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceReplyRsaPublicKey(JNIEnv *env, jobject instance, jlong dev) {
    return ont_device_replay_rsa_publickey((ont_device_t*)dev);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceRegister(JNIEnv *env, jobject instance, jlong dev_, jstring regCode_, jstring authInfo_) {

    ont_device_t * dev = (ont_device_t *)dev_;
    const char *regCode = J4A_GetStringUTFChars(env, regCode_, 0);
    const char *authInfo = J4A_GetStringUTFChars(env, authInfo_, 0);
    int result = ont_device_register(dev, dev->product_id, regCode, authInfo);
    J4A_ReleaseStringUTFChars(env, regCode_, regCode);
    J4A_ReleaseStringUTFChars(env, authInfo_, authInfo);

    return result;
}

JNIEXPORT jobject JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceAuth(JNIEnv *env, jobject instance, jlong dev_) {

    if (class_OntDeviceAuthRet.id == NULL) {

        const char *J4A_UNUSED(name)      = NULL;
        const char *J4A_UNUSED(sign)      = NULL;
        jclass      J4A_UNUSED(class_id)   = NULL;

        sign = "com/ont/media/odvp/OntDeviceAuthRet";
        class_OntDeviceAuthRet.id = J4A_FindClass__asGlobalRef__catchAll(env, sign);
        if (class_OntDeviceAuthRet.id == NULL)
            goto fail;

        class_id = class_OntDeviceAuthRet.id;
        name     = "<init>";
        sign     = "()V";
        class_OntDeviceAuthRet.method_Constructor = J4A_GetMethodID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceAuthRet.method_Constructor == NULL)
            goto fail;

        class_id = class_OntDeviceAuthRet.id;
        name     = "ret";
        sign     = "I";
        class_OntDeviceAuthRet.field_Ret = J4A_GetFieldID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceAuthRet.field_Ret == NULL)
            goto fail;

        class_id = class_OntDeviceAuthRet.id;
        name     = "authCode";
        sign     = "[B";
        class_OntDeviceAuthRet.field_AuthCode = J4A_GetFieldID__catchAll(env, class_id, name, sign);
        if (class_OntDeviceAuthRet.field_AuthCode == NULL)
            goto fail;
    }

    ont_device_t *dev = dev_;
    char auth_code_new[256];
    memset(auth_code_new, 0, 256);
    int result = ont_device_auth(dev, dev->auth_code, auth_code_new);

    jbyteArray auth_code_new_ = NULL;
    int auth_code_new_length = strlen(auth_code_new);
    if (ONT_ERR_OK == result && auth_code_new_length > 0) {

        auth_code_new_ = (*env)->NewByteArray(env, auth_code_new_length);
        if (auth_code_new_ != NULL) {
            (*env)->SetByteArrayRegion(env, auth_code_new_, 0, auth_code_new_length, (jbyte *)auth_code_new);
        }
    }

    jobject obj = (*env)->NewObject(env, class_OntDeviceAuthRet.id, class_OntDeviceAuthRet.method_Constructor);
    (*env)->SetIntField(env, obj, class_OntDeviceAuthRet.field_Ret, result);
    (*env)->SetObjectField(env, obj, class_OntDeviceAuthRet.field_AuthCode, auth_code_new_);

fail:
    return obj;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceAddChannel(JNIEnv *env, jobject instance, jlong dev, jint channelId, jstring title_, jstring desc_) {

    const char *title = J4A_GetStringUTFChars(env, title_, 0);
    const char *desc = J4A_GetStringUTFChars(env, desc_, 0);
    int result = ont_device_add_channel((ont_device_t*)dev, channelId, title, strlen(title), desc, strlen(desc));
    J4A_ReleaseStringUTFChars(env, title_, title);
    J4A_ReleaseStringUTFChars(env, desc_, desc);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceKeepAlive(JNIEnv *env, jobject instance, jlong dev) {

    return ont_device_keepalive((ont_device_t*)dev);
}

JNIEXPORT jlongArray JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceGetChannels(JNIEnv *env, jobject instance, jlong dev) {

    char resp[200];
    size_t resp_length=200;
    int32_t cnt = 0, index = 0;
    jlongArray channels_ = NULL;

    int ret = ont_device_msg_sendmsg((ont_device_t*)dev, MSG_TYPE_QUERY_CHANNELLIST, NULL, 0, 1, resp, &resp_length, DEVICE_MSG_TIMEOUT);
    if (ret != ONT_ERR_OK)
    {
        return NULL;

    }

    index = 0;
    cnt = resp[0];
    channels_ = (*env)->NewLongArray(env, cnt);
    jlong *channels = (*env)->GetLongArrayElements(env, channels_, NULL);
    for (; index < cnt; index++)
    {
        channels[index] = ont_decodeInt32(resp + 1 + index * 4);
    }

    (*env)->ReleaseLongArrayElements(env, channels_, channels, 0);
    return channels_;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceDelChannel(JNIEnv *env, jobject instance, jlong dev, jlong channelId) {
    return ont_device_del_channel((ont_device_t*)dev, channelId);
}

void
Java_com_ont_media_odvp_OntOdvp_nativeDeviceDisconnect(JNIEnv *env, jobject instance, jlong dev) {
    ont_device_disconnect((ont_device_t*)dev);
}

void
Java_com_ont_media_odvp_OntOdvp_nativeDeviceDestroy(JNIEnv *env, jobject instance, jlong dev_) {

    ont_device_t *dev = (ont_device_t *)dev_;
    if (dev->device_cbs_weak_this != NULL) {

        (*env)->DeleteGlobalRef(env, dev->device_cbs_weak_this);
        dev->device_cbs_weak_this = NULL;
    }

    if (dev->cmd_cbs_weak_this != NULL) {

        (*env)->DeleteGlobalRef(env, dev->cmd_cbs_weak_this);
        dev->cmd_cbs_weak_this = NULL;
    }

    device_destroy(dev);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceCheckReceive(JNIEnv *env, jobject instance, jlong dev, jint tm_ms) {

    return ont_device_check_receive((ont_device_t*)dev, tm_ms);
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceReplyOntCmd(JNIEnv *env, jobject instance, jlong dev, jint result_, jstring cmdId_, jstring resp_) {
    const char *cmdId = J4A_GetStringUTFChars(env, cmdId_, 0);
    const char *resp = J4A_GetStringUTFChars(env, resp_, 0);

    int result = ont_device_reply_ont_cmd((ont_device_t*)dev, result_, cmdId, resp, strlen(resp));

    J4A_ReleaseStringUTFChars(env, cmdId_, cmdId);
    J4A_ReleaseStringUTFChars(env, resp_, resp);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceReplyUserDefinedCmd(JNIEnv *env, jobject instance, jlong dev, jint result_, jstring cmdId_, jstring resp_) {
    const char *cmdId = J4A_GetStringUTFChars(env, cmdId_, 0);
    const char *resp = J4A_GetStringUTFChars(env, resp_, 0);

    int result = ont_device_reply_user_defined_cmd((ont_device_t*)dev, result_, cmdId, resp, strlen(resp));

    J4A_ReleaseStringUTFChars(env, cmdId_, cmdId);
    J4A_ReleaseStringUTFChars(env, resp_, resp);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceDataUpload(JNIEnv *env, jobject instance, jlong dev, jstring data_id_, jstring data_, jint len) {

    const char *data = J4A_GetStringUTFChars(env, data_, 0);
    const char *data_id = J4A_GetStringUTFChars(env, data_id_, 0);
    int result = ont_device_data_upload((ont_device_t*)dev, strtoull(data_id, NULL, 10), data, len);
    J4A_ReleaseStringUTFChars(env, data_, data);
    J4A_ReleaseStringUTFChars(env, data_id_, data_id);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceUploadPicture(JNIEnv *env, jobject instance, jlong dev, jstring filename_, jstring name_, jlong channel)
{
    const char *filename = J4A_GetStringUTFChars(env, filename_, 0);
    const char *name = J4A_GetStringUTFChars(env, name_, 0);
    int ret = device_upload_picture((ont_device_t*)dev, filename, name, channel);
    J4A_ReleaseStringUTFChars(env, filename_, filename);
    J4A_ReleaseStringUTFChars(env, name_, name);
}

JNIEXPORT jobject JNICALL
Java_com_ont_media_odvp_OntOdvp_nativeDeviceRequestPushStream(JNIEnv *env, jobject instance, jlong dev, jlong channel, jint idle_sec)
{
    ont_plat_resp_dev_ps_t resp = {0};
    int ret = ont_device_req_push_stream((ont_device_t*)dev, channel, idle_sec, &resp);

    jclass PlatRespClass = (*env)->FindClass(env, "com/ont/media/odvp/model/PlatRespDevPS");
    jmethodID constructMethodId = (*env)->GetMethodID(env, PlatRespClass, "<init>", "()V");
    jobject platResp = (*env)->NewObject(env, PlatRespClass, constructMethodId);

    jfieldID idNetResult = (*env)->GetFieldID(env, PlatRespClass, "net_result", "I");
    jfieldID idResult = (*env)->GetFieldID(env, PlatRespClass, "result", "I");
    jfieldID idChannel = (*env)->GetFieldID(env, PlatRespClass, "chan_id", "I");
    jfieldID idProtype = (*env)->GetFieldID(env, PlatRespClass, "pro_type", "I");
    jfieldID idPushUrl = (*env)->GetFieldID(env, PlatRespClass, "push_url", "Ljava/lang/String;");
    jfieldID idLen = (*env)->GetFieldID(env, PlatRespClass, "url_len", "I");
    jfieldID idMin = (*env)->GetFieldID(env, PlatRespClass, "url_ttl_min", "I");

    if (ret != 0) {

        (*env)->SetIntField(env, platResp, idNetResult, ret);
        return platResp;
    }

    (*env)->SetIntField(env, platResp, idNetResult, 0);
    (*env)->SetIntField(env, platResp, idResult, resp.result);
    (*env)->SetIntField(env, platResp, idChannel, resp.chan_id);
    (*env)->SetIntField(env, platResp, idProtype, resp.protype);
    (*env)->SetObjectField(env, platResp, idPushUrl, (*env)->NewStringUTF(env, resp.push_url));
    (*env)->SetIntField(env, platResp, idLen, resp.url_len);
    (*env)->SetIntField(env, platResp, idMin, resp.url_ttl_min);

    return platResp;
}

void on_keepalive_resp(ont_device_t *dev) {
    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }
    (*env)->CallStaticVoidMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnKeepAliveResp, (jobject)(dev->device_cbs_weak_this));
}

int32_t on_live_stream_start(ont_device_t *dev, int32_t channel, uint8_t protype, uint16_t ttl_min,
                         const char *push_url) {

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    return (*env)->CallStaticIntMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnLiveStreamStart, (jobject)(dev->device_cbs_weak_this), channel, protype, ttl_min, (*env)->NewStringUTF(env, push_url));
}

int32_t on_vod_stream_start(ont_device_t *dev, int32_t channel, uint8_t protype,
                            ont_video_file_t *fileinfo, const char *playflag, const char *pushurl,
                            uint16_t ttl) {

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    jclass videoFileInfoClass = (*env)->FindClass(env, "com/ont/media/odvp/model/VideoFileInfo");
    jmethodID constructMethodId = (*env)->GetMethodID(env, videoFileInfoClass, "<init>", "()V");
    jobject videoFileInfo = (*env)->NewObject(env, videoFileInfoClass, constructMethodId);

    jfieldID idChannel = (*env)->GetFieldID(env, videoFileInfoClass, "channel", "I");
    jfieldID idBeginTime = (*env)->GetFieldID(env, videoFileInfoClass, "begin_time", "Ljava/lang/String;");
    jfieldID idEndTime = (*env)->GetFieldID(env, videoFileInfoClass, "end_time", "Ljava/lang/String;");

    (*env)->SetIntField(env, videoFileInfo, idChannel, channel);
    (*env)->SetObjectField(env, videoFileInfo, idBeginTime, (*env)->NewStringUTF(env, fileinfo->begin_time));
    (*env)->SetObjectField(env, videoFileInfo, idEndTime, (*env)->NewStringUTF(env, fileinfo->end_time));

    return (*env)->CallStaticIntMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnVodStreamStart, (jobject)(dev->device_cbs_weak_this), channel, protype, videoFileInfo, (*env)->NewStringUTF(env, playflag), (*env)->NewStringUTF(env, pushurl), ttl);
}

int32_t on_cmd_channel_record_update(void *dev, int32_t channel, int32_t status, int32_t seconds, char *url) {

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    ont_device_t* device = (ont_device_t*) dev;
    return (*env)->CallStaticIntMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnChannelRecordUpdate, (jobject)(device->device_cbs_weak_this), channel ,status, seconds, (*env)->NewStringUTF(env, url));
}

int32_t on_user_defined_cmd(ont_device_t *dev, ont_device_cmd_t *cmd) {

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    jclass platformCmdClass = (*env)->FindClass(env, "com/ont/media/odvp/model/PlatformCmd");
    jmethodID constructMethodId = (*env)->GetMethodID(env, platformCmdClass, "<init>", "()V");
    jobject platformCmd = (*env)->NewObject(env, platformCmdClass, constructMethodId);

    jfieldID idNeedResp = (*env)->GetFieldID(env, platformCmdClass, "need_resp", "Z");
    jfieldID idCmdId = (*env)->GetFieldID(env, platformCmdClass, "id", "Ljava/lang/String;");
    jfieldID idReq = (*env)->GetFieldID(env, platformCmdClass, "req", "[B");
    jfieldID idSize = (*env)->GetFieldID(env, platformCmdClass, "size", "I");

    (*env)->SetBooleanField(env, platformCmd, idNeedResp, (jboolean)cmd->need_resp);
    (*env)->SetObjectField(env, platformCmd, idCmdId, (*env)->NewStringUTF(env, cmd->id));
    (*env)->SetIntField(env, platformCmd, idSize, cmd->size);

    jbyteArray cmd_req = NULL;
    if (cmd->size > 0) {
        cmd_req = (*env)->NewByteArray(env, cmd->size);
        (*env)->SetByteArrayRegion(env, cmd_req, 0, cmd->size, (jbyte *)cmd->req);
    }
    (*env)->SetObjectField(env, platformCmd, idReq, cmd_req);

    return (*env)->CallStaticIntMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnUserDefinedCmd, (jobject)(dev->device_cbs_weak_this), platformCmd);
}

int32_t on_api_defined_msg(ont_device_t *dev, char *msg, size_t msg_len){

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    jbyteArray cmd_msg = NULL;
    if (msg_len > 0) {

        cmd_msg = (*env)->NewByteArray(env, msg_len);
        (*env)->SetByteArrayRegion(env, cmd_msg, 0, msg_len, (jbyte *)msg);
    }
    return (*env)->CallStaticIntMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnApiDefinedMsg, (jobject)(dev->device_cbs_weak_this), cmd_msg, msg_len);
}

int32_t on_plat_resp_dev_push_stream_msg(ont_device_t *dev, ont_plat_resp_dev_ps_t *resp) {

    /*JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    jclass PlatRespClass = (*env)->FindClass(env, "com/ont/media/odvp/model/PlatRespDevPS");
    jmethodID constructMethodId = (*env)->GetMethodID(env, PlatRespClass, "<init>", "()V");
    jobject platResp = (*env)->NewObject(env, PlatRespClass, constructMethodId);

    jfieldID idResult = (*env)->GetFieldID(env, PlatRespClass, "result", "I");
    jfieldID idChannel = (*env)->GetFieldID(env, PlatRespClass, "chan_id", "I");
    jfieldID idProtype = (*env)->GetFieldID(env, PlatRespClass, "protype", "I");
    jfieldID idPushUrl = (*env)->GetFieldID(env, PlatRespClass, "push_url", "Ljava/lang/String;");
    jfieldID idLen = (*env)->GetFieldID(env, PlatRespClass, "url_len", "I");
    jfieldID idMin = (*env)->GetFieldID(env, PlatRespClass, "url_ttl_min", "I");

    (*env)->SetIntField(env, platResp, idResult, resp->result);
    (*env)->SetIntField(env, platResp, idChannel, resp->chan_id);
    (*env)->SetIntField(env, platResp, idProtype, resp->protype);
    (*env)->SetObjectField(env, platResp, idPushUrl, (*env)->NewStringUTF(env, resp->push_url));
    (*env)->SetIntField(env, platResp, idLen, resp->url_len);
    (*env)->SetIntField(env, platResp, idMin, resp->url_ttl_min);

    return (*env)->CallStaticIntMethod(env, class_OntDeviceCb.id, class_OntDeviceCb.method_OnPlatRespDevPushStreamMsg, (jobject)(dev->device_cbs_weak_this), platResp);*/
    return 0;
}

int32_t on_cmd_live_stream_ctrl(void *dev, int32_t channel, int32_t level) {

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    ont_device_t* device = (ont_device_t*)dev;
    return (*env)->CallStaticIntMethod(env, class_OntCmdCb.id, class_OntCmdCb.method_OnCmdLiveStreamCtrl, (jobject)(device->cmd_cbs_weak_this), channel, level);
}

int32_t on_cmd_ptz_ctrl(void *dev, int32_t channel, int32_t mode, t_ont_video_ptz_cmd ptz, int32_t speed)
{

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    ont_device_t* device = (ont_device_t*)dev;
    return (*env)->CallStaticIntMethod(env, class_OntCmdCb.id, class_OntCmdCb.method_OnCmdPtzCtrl, (jobject)(device->cmd_cbs_weak_this), channel, mode, ptz, speed);
}

int32_t on_cmd_query_files(void *dev, int32_t channel, int32_t page, int32_t startindex, int32_t max, const char *starTime, const char *endTime, char *uuid) {

    JNIEnv *env;
    jint ret = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (ret != JNI_OK) {
        LOGE("%s error: AttachCurrentThread failed\n", __FUNCTION__);
    }

    ont_device_t* device = (ont_device_t*)dev;
    return (*env)->CallStaticIntMethod(env, class_OntCmdCb.id, class_OntCmdCb.method_OnCmdQueryFiles, (jobject)(device->cmd_cbs_weak_this), channel, page, startindex, max, (*env)->NewStringUTF(env, starTime), (*env)->NewStringUTF(env, endTime), (*env)->NewStringUTF(env, uuid));
}