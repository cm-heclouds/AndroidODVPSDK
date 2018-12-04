#ifdef __ANDROID__
#include <android/log.h>
#else
#include <stdio.h>
#endif

#ifndef LOG_TAG
#define LOG_TAG "OntOdvp"
#endif /* ifndef LOG_TAG */

#ifdef __ANDROID__
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGV(...) printf(LOG_TAG, __VA_ARGS__)
#define LOGD(...) printf(LOG_TAG, __VA_ARGS__)
#define LOGI(...) printf(LOG_TAG, __VA_ARGS__)
#define LOGW(...) printf(LOG_TAG, __VA_ARGS__)
#define LOGE(...) printf(LOG_TAG, __VA_ARGS__)
#endif

