#include <jni.h>
#include <libyuv.h>
#include <stdio.h>
#include <memory.h>
//#include <transfer_utils.h>
//#include "rgb2jpeg.h"
//#include "jpeglib.h"

struct YuvFrame {
    int width;
    int height;
    uint8_t *data;
    uint8_t *y;
    uint8_t *u;
    uint8_t *v;
};

static struct YuvFrame i420_rotated_frame;
static struct YuvFrame i420_scaled_frame;
static struct YuvFrame nv12_frame;

jboolean ont_convert_to_i420(uint8_t *src_frame, jint src_width, jint src_height,
                             jboolean need_flip, jint rotate_degree, int format);
jboolean ont_convert_to_i420_with_crop_scale(uint8_t *src_frame, jint src_width, jint src_height,
                                             jint crop_x, jint crop_y, jint crop_width,
                                             jint crop_height,
                                             jboolean need_flip, jint rotate_degree, int format);

int off_x, off_y;//x 偏移y 偏移
jint num_width, num_height;//数字宽高
int date_len;

char *mNumArrays = NULL;
size_t date_size;

JNIEXPORT void JNICALL
Java_com_ont_media_odvp_OntFormat_WaterMarkInit(JNIEnv *env, jobject thiz, jint offset_x,
                                                jint offset_y, jint mark_len) {
    off_x = offset_x;
    off_y = offset_y;

    num_width = 17;
    num_height = 23;
    date_len = mark_len;

    if(mNumArrays == NULL){
        mNumArrays = calloc((size_t)date_len, num_width * num_height * sizeof(char));

        //这里数字水印数据是通过生成图片转换来的
        char NUM_0[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};//17*23的二维数组

        char NUM_1[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_2[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_3[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_4[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_5[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_6[] = {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_7[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_8[] = {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0};

        char NUM_9[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0,
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                        0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_SPACE[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_COLON[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        char NUM_LINE[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        date_size = (size_t) num_width * num_height;
        memcpy(mNumArrays, NUM_0, date_size);
        memcpy(mNumArrays + date_size * 1, NUM_1, date_size);
        memcpy(mNumArrays + date_size * 2, NUM_2, date_size);
        memcpy(mNumArrays + date_size * 3, NUM_3, date_size);
        memcpy(mNumArrays + date_size * 4, NUM_4, date_size);
        memcpy(mNumArrays + date_size * 5, NUM_5, date_size);
        memcpy(mNumArrays + date_size * 6, NUM_6, date_size);
        memcpy(mNumArrays + date_size * 7, NUM_7, date_size);
        memcpy(mNumArrays + date_size * 8, NUM_8, date_size);
        memcpy(mNumArrays + date_size * 9, NUM_9, date_size);
        memcpy(mNumArrays + date_size * 10, NUM_LINE, date_size);
        memcpy(mNumArrays + date_size * 11, NUM_SPACE, date_size);
        memcpy(mNumArrays + date_size * 12, NUM_COLON, date_size);
    }
}

int getIndex(jchar c) {
    if (c >= '0' && c <= '9')
        return c - '0';
    else if (c == '-')
        return 10;
    else if (c == ' ')
        return 11;
    else if (c == ':')
        return 12;
    return 11;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_WaterMark(JNIEnv *env, jobject thiz, jbyteArray frame,
                                            jstring date_, jint src_width, jint src_height) {

    jbyte *yuv = (*env)->GetByteArrayElements(env, frame, NULL);
    const jchar *date = (*env)->GetStringChars(env, date_, NULL);

    //添加时间水印
    jbyte *dest = yuv;
    jbyte *start = dest + off_y * src_width;
    for (int i = 0; i < date_len; i++) {
        int index = getIndex(*(date + i));
        char *num = mNumArrays + date_size * index;
        jbyte *column = start + i * num_width;
        if (!num)
            continue;
        for (int j = 0; j < num_height; j++) {
            jbyte *destIndex = column + j * src_width + off_x;
            char *src = num + j * num_width;
            for (int k = 0; k < num_width; k++) {
                if (*(src + k) != 0) {//黑色背景色
                    *(destIndex + k) = -21;//水印文字颜色，-21为白色，0为黑色
                }
            }
        }
    }

    int y_size = src_width * src_height;
    jbyteArray markedFrame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, markedFrame, 0, y_size * 3 / 2, yuv);

    (*env)->ReleaseByteArrayElements(env, frame, yuv, 0);
    (*env)->ReleaseStringChars(env, date_, date);

    return markedFrame;
}

JNIEXPORT void JNICALL
Java_com_ont_media_odvp_OntFormat_WaterMarkRelease(JNIEnv *env, jobject thiz) {
    if(mNumArrays){
        free(mNumArrays);
        mNumArrays = NULL;
    }
}

JNIEXPORT void JNICALL
Java_com_ont_media_odvp_OntFormat_setEncoderResolution(JNIEnv *env, jobject thiz, jint out_width, jint out_height) {
    int y_size = out_width * out_height;

    if (i420_scaled_frame.width != out_width || i420_scaled_frame.height != out_height) {
        free(i420_scaled_frame.data);
        i420_scaled_frame.width = out_width;
        i420_scaled_frame.height = out_height;
        i420_scaled_frame.data = (uint8_t *) malloc(y_size * 3 / 2);
        i420_scaled_frame.y = i420_scaled_frame.data;
        i420_scaled_frame.u = i420_scaled_frame.y + y_size;
        i420_scaled_frame.v = i420_scaled_frame.u + y_size / 4;
    }

    if (nv12_frame.width != out_width || nv12_frame.height != out_height) {
        free(nv12_frame.data);
        nv12_frame.width = out_width;
        nv12_frame.height = out_height;
        nv12_frame.data = (uint8_t *) malloc(y_size * 3 / 2);
        nv12_frame.y = nv12_frame.data;
        nv12_frame.u = nv12_frame.y + y_size;
        nv12_frame.v = nv12_frame.u + y_size / 4;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_RGBAToI420(JNIEnv *env, jobject thiz, jbyteArray frame,
                                             jint src_width, jint src_height, jboolean need_flip,
                                             jint rotate_degree) {

    jbyte *rgba_frame = (*env)->GetByteArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420((uint8_t *) rgba_frame, src_width, src_height, need_flip, rotate_degree,
                             FOURCC_RGBA)) {
        return NULL;
    }

    int y_size = i420_scaled_frame.width * i420_scaled_frame.height;
    jbyteArray i420Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, i420Frame, 0, y_size * 3 / 2, (jbyte *) i420_scaled_frame.data);

    (*env)->ReleaseByteArrayElements(env, frame, rgba_frame, JNI_ABORT);
    return i420Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_RGBAToNV12(JNIEnv *env, jobject thiz, jbyteArray frame,
                                             jint src_width, jint src_height, jboolean need_flip,
                                             jint rotate_degree) {

    jbyte *rgba_frame = (*env)->GetByteArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420((uint8_t *) rgba_frame, src_width, src_height, need_flip, rotate_degree,
                         FOURCC_RGBA)) {
        return NULL;
    }

    int ret = ConvertFromI420(i420_scaled_frame.y, i420_scaled_frame.width,
                              i420_scaled_frame.u, i420_scaled_frame.width / 2,
                              i420_scaled_frame.v, i420_scaled_frame.width / 2,
                              nv12_frame.data, nv12_frame.width,
                              nv12_frame.width, nv12_frame.height,
                              FOURCC_NV12);
    if (ret < 0) {

        return NULL;
    }

    int y_size = nv12_frame.width * nv12_frame.height;
    jbyteArray nv12Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, nv12Frame, 0, y_size * 3 / 2, (jbyte *) nv12_frame.data);

    (*env)->ReleaseByteArrayElements(env, frame, rgba_frame, JNI_ABORT);
    return nv12Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_NV21ToNV12(JNIEnv *env, jobject thiz, jbyteArray frame,
                                             jstring date_, jint src_width, jint src_height,
                                             jboolean need_flip, jint rotate_degree) {

    jbyte *nv21_frame = (*env)->GetByteArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420((uint8_t *) nv21_frame, src_width, src_height, need_flip, rotate_degree,
                             FOURCC_NV21)) {
        return NULL;
    }

    int ret = ConvertFromI420(i420_scaled_frame.y, i420_scaled_frame.width,
                              i420_scaled_frame.u, i420_scaled_frame.width / 2,
                              i420_scaled_frame.v, i420_scaled_frame.width / 2,
                              nv12_frame.data, nv12_frame.width,
                              nv12_frame.width, nv12_frame.height,
                              FOURCC_NV12);
    if (ret < 0) {
        return NULL;
    }

    if(date_ != NULL){

        const jchar *date = (*env)->GetStringChars(env, date_, NULL);

        //添加时间水印
        jbyte *dest = (jbyte *)nv12_frame.data;
        jbyte *start = dest + off_y * nv12_frame.width;
        for (int i = 0; i < date_len; i++) {
            int index = getIndex(*(date + i));
            char *num = mNumArrays + date_size * index;
            jbyte *column = start + i * num_width;
            if (!num)
                continue;
            for (int j = 0; j < num_height; j++) {
                jbyte *destIndex = column + j * nv12_frame.width + off_x;
                char *src = num + j * num_width;
                for (int k = 0; k < num_width; k++) {
                    if (*(src + k) != 0) {//黑色背景色
                        *(destIndex + k) = -21;//水印文字颜色，-21为白色，0为黑色
                    }
                }
            }
        }

        (*env)->ReleaseStringChars(env, date_, date);
    }

    int y_size = src_width * src_height;
    jbyteArray markedFrame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, markedFrame, 0, y_size * 3 / 2, (jbyte *) nv12_frame.data);

    (*env)->ReleaseByteArrayElements(env, frame, nv21_frame, 0);

    return markedFrame;

//    int y_size = nv12_frame.width * nv12_frame.height;
//    jbyteArray nv12Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
//    (*env)->SetByteArrayRegion(env, nv12Frame, 0, y_size * 3 / 2, (jbyte *) nv12_frame.data);
//
//    (*env)->ReleaseByteArrayElements(env, frame, nv21_frame, JNI_ABORT);
//    return nv12Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_NV21ToI420(JNIEnv *env, jobject thiz, jbyteArray frame,
                                             jstring date_, jint src_width, jint src_height,
                                             jboolean need_flip, jint rotate_degree) {

    jbyte *nv21_frame = (*env)->GetByteArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420((uint8_t *) nv21_frame, src_width, src_height, need_flip, rotate_degree,
                             FOURCC_NV21)) {
        return NULL;
    }

    if(date_ != NULL){

        const jchar *date = (*env)->GetStringChars(env, date_, NULL);

        //添加时间水印
        jbyte *dest = (jbyte *) i420_scaled_frame.data;
        jbyte *start = dest + off_y * i420_scaled_frame.width;
        for (int i = 0; i < date_len; i++) {
            int index = getIndex(*(date + i));
            char *num = mNumArrays + date_size * index;
            jbyte *column = start + i * num_width;
            if (!num)
                continue;
            for (int j = 0; j < num_height; j++) {
                jbyte *destIndex = column + j * i420_scaled_frame.width + off_x;
                char *src = num + j * num_width;
                for (int k = 0; k < num_width; k++) {
                    if (*(src + k) != 0) {//黑色背景色
                        *(destIndex + k) = -21;//水印文字颜色，-21为白色，0为黑色
                    }
                }
            }
        }

        (*env)->ReleaseStringChars(env, date_, date);
    }

    int y_size = src_width * src_height;
    jbyteArray markedFrame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, markedFrame, 0, y_size * 3 / 2, (jbyte *) i420_scaled_frame.data);

    (*env)->ReleaseByteArrayElements(env, frame, nv21_frame, 0);

    return markedFrame;

//    int y_size = i420_scaled_frame.width * i420_scaled_frame.height;
//    jbyteArray i420Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
//    (*env)->SetByteArrayRegion(env, i420Frame, 0, y_size * 3 / 2, (jbyte *) i420_scaled_frame.data);
//
//    (*env)->ReleaseByteArrayElements(env, frame, nv21_frame, JNI_ABORT);
//    return i420Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_ARGBToI420Scaled(JNIEnv *env, jobject thiz, jintArray frame, jint src_width,
                        jint src_height, jboolean need_flip, jint rotate_degree,
                        jint crop_x, jint crop_y, jint crop_width, jint crop_height) {
    jint *argb_frame = (*env)->GetIntArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420_with_crop_scale((uint8_t *) argb_frame, src_width, src_height,
                                         crop_x, crop_y, crop_width, crop_height,
                                         need_flip, rotate_degree, FOURCC_ARGB)) {
        return NULL;
    }

    int y_size = i420_scaled_frame.width * i420_scaled_frame.height;
    jbyteArray i420Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, i420Frame, 0, y_size * 3 / 2, (jbyte *) i420_scaled_frame.data);

    (*env)->ReleaseIntArrayElements(env, frame, argb_frame, JNI_ABORT);
    return i420Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_ARGBToNV12Scaled(JNIEnv *env, jobject thiz, jintArray frame, jint src_width,
                        jint src_height, jboolean need_flip, jint rotate_degree,
                        jint crop_x, jint crop_y, jint crop_width, jint crop_height) {
    jint *argb_frame = (*env)->GetIntArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420_with_crop_scale((uint8_t *) argb_frame, src_width, src_height,
                                         crop_x, crop_y, crop_width, crop_height,
                                         need_flip, rotate_degree, FOURCC_ARGB)) {
        return NULL;
    }

    int ret = ConvertFromI420(i420_scaled_frame.y, i420_scaled_frame.width,
                              i420_scaled_frame.u, i420_scaled_frame.width / 2,
                              i420_scaled_frame.v, i420_scaled_frame.width / 2,
                              nv12_frame.data, nv12_frame.width,
                              nv12_frame.width, nv12_frame.height,
                              FOURCC_NV12);
    if (ret < 0) {
        return NULL;
    }

    int y_size = nv12_frame.width * nv12_frame.height;
    jbyteArray nv12Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, nv12Frame, 0, y_size * 3 / 2, (jbyte *) nv12_frame.data);

    (*env)->ReleaseIntArrayElements(env, frame, argb_frame, JNI_ABORT);
    return nv12Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_ARGBToI420(JNIEnv *env, jobject thiz, jintArray frame, jint src_width,
                                    jint src_height, jboolean need_flip, jint rotate_degree) {
    jint *argb_frame = (*env)->GetIntArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420((uint8_t *) argb_frame, src_width, src_height, need_flip, rotate_degree,
                         FOURCC_ARGB)) {
        return NULL;
    }

    int y_size = i420_scaled_frame.width * i420_scaled_frame.height;
    jbyteArray i420Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, i420Frame, 0, y_size * 3 / 2, (jbyte *) i420_scaled_frame.data);

    (*env)->ReleaseIntArrayElements(env, frame, argb_frame, JNI_ABORT);
    return i420Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_ARGBToNV12(JNIEnv *env, jobject thiz, jintArray frame, jint src_width,
                                    jint src_height, jboolean need_flip, jint rotate_degree) {
    jint *argb_frame = (*env)->GetIntArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420((uint8_t *) argb_frame, src_width, src_height, need_flip, rotate_degree,
                         FOURCC_ARGB)) {
        return NULL;
    }

    int ret = ConvertFromI420(i420_scaled_frame.y, i420_scaled_frame.width,
                              i420_scaled_frame.u, i420_scaled_frame.width / 2,
                              i420_scaled_frame.v, i420_scaled_frame.width / 2,
                              nv12_frame.data, nv12_frame.width,
                              nv12_frame.width, nv12_frame.height,
                              FOURCC_NV12);
    if (ret < 0) {
        return NULL;
    }

    int y_size = nv12_frame.width * nv12_frame.height;
    jbyteArray nv12Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, nv12Frame, 0, y_size * 3 / 2, (jbyte *) nv12_frame.data);

    (*env)->ReleaseIntArrayElements(env, frame, argb_frame, JNI_ABORT);
    return nv12Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_NV21ToNV12Scaled(JNIEnv *env, jobject thiz, jbyteArray frame, jint src_width,
                                jint src_height, jboolean need_flip, jint rotate_degree,
                                jint crop_x, jint crop_y, jint crop_width, jint crop_height) {
    jbyte *rgba_frame = (*env)->GetByteArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420_with_crop_scale((uint8_t *) rgba_frame, src_width, src_height,
                                crop_x, crop_y, crop_width, crop_height,
                                need_flip, rotate_degree, FOURCC_NV21)) {
        return NULL;
    }

    int ret = ConvertFromI420(i420_scaled_frame.y, i420_scaled_frame.width,
                              i420_scaled_frame.u, i420_scaled_frame.width / 2,
                              i420_scaled_frame.v, i420_scaled_frame.width / 2,
                              nv12_frame.data, nv12_frame.width,
                              nv12_frame.width, nv12_frame.height,
                              FOURCC_NV12);
    if (ret < 0) {
        return NULL;
    }

    int y_size = nv12_frame.width * nv12_frame.height;
    jbyteArray nv12Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, nv12Frame, 0, y_size * 3 / 2, (jbyte *) nv12_frame.data);

    (*env)->ReleaseByteArrayElements(env, frame, rgba_frame, JNI_ABORT);
    return nv12Frame;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ont_media_odvp_OntFormat_NV21ToI420Scaled(JNIEnv *env, jobject thiz, jbyteArray frame, jint src_width,
                        jint src_height, jboolean need_flip, jint rotate_degree,
                        jint crop_x, jint crop_y, jint crop_width, jint crop_height) {
    jbyte *argb_frame = (*env)->GetByteArrayElements(env, frame, NULL);

    if (!ont_convert_to_i420_with_crop_scale((uint8_t *) argb_frame, src_width, src_height,
                                         crop_x, crop_y, crop_width, crop_height,
                                         need_flip, rotate_degree, FOURCC_NV21)) {
        return NULL;
    }

    int y_size = i420_scaled_frame.width * i420_scaled_frame.height;
    jbyteArray i420Frame = (*env)->NewByteArray(env, y_size * 3 / 2);
    (*env)->SetByteArrayRegion(env, i420Frame, 0, y_size * 3 / 2, (jbyte *) i420_scaled_frame.data);

    (*env)->ReleaseByteArrayElements(env, frame, argb_frame, JNI_ABORT);
    return i420Frame;
}

jboolean ont_convert_to_i420(uint8_t *src_frame, jint src_width, jint src_height,
                             jboolean need_flip, jint rotate_degree, int format) {
    int y_size = src_width * src_height;

    if (rotate_degree % 180 == 0) {
        if (i420_rotated_frame.width != src_width || i420_rotated_frame.height != src_height) {
            free(i420_rotated_frame.data);
            i420_rotated_frame.width = src_width;
            i420_rotated_frame.height = src_height;
            i420_rotated_frame.data = (uint8_t *) malloc(y_size * 4 / 2);
            i420_rotated_frame.y = i420_rotated_frame.data;
            i420_rotated_frame.u = i420_rotated_frame.y + y_size;
            i420_rotated_frame.v = i420_rotated_frame.u + y_size / 4;
        }
    } else {
        if (i420_rotated_frame.width != src_height || i420_rotated_frame.height != src_width) {
            free(i420_rotated_frame.data);
            i420_rotated_frame.width = src_height;
            i420_rotated_frame.height = src_width;
            i420_rotated_frame.data = (uint8_t *) malloc(y_size * 4 / 2);
            i420_rotated_frame.y = i420_rotated_frame.data;
            i420_rotated_frame.u = i420_rotated_frame.y + y_size;
            i420_rotated_frame.v = i420_rotated_frame.u + y_size / 4;
        }
    }

    jint ret = ConvertToI420(src_frame, y_size,
                             i420_rotated_frame.y, i420_rotated_frame.width,
                             i420_rotated_frame.u, i420_rotated_frame.width / 2,
                             i420_rotated_frame.v, i420_rotated_frame.width / 2,
                             0, 0,
                             src_width, src_height,
                             src_width, src_height,
                             (enum RotationMode) rotate_degree, format);
    if (ret < 0) {

        return JNI_FALSE;
    }

    ret = I420Scale(i420_rotated_frame.y, i420_rotated_frame.width,
                    i420_rotated_frame.u, i420_rotated_frame.width / 2,
                    i420_rotated_frame.v, i420_rotated_frame.width / 2,
                    need_flip ? -i420_rotated_frame.width : i420_rotated_frame.width,
                    i420_rotated_frame.height,
                    i420_scaled_frame.y, i420_scaled_frame.width,
                    i420_scaled_frame.u, i420_scaled_frame.width / 2,
                    i420_scaled_frame.v, i420_scaled_frame.width / 2,
                    i420_scaled_frame.width, i420_scaled_frame.height,
                    kFilterNone);

    if (ret < 0) {

        return JNI_FALSE;
    }

    return JNI_TRUE;
}

jboolean ont_convert_to_i420_with_crop_scale(uint8_t *src_frame, jint src_width, jint src_height,
                                            jint crop_x, jint crop_y, jint crop_width,
                                            jint crop_height,
                                            jboolean need_flip, jint rotate_degree, int format) {
    int y_size = src_width * src_height;

    if (rotate_degree % 180 == 0) {
        if (i420_rotated_frame.width != src_width || i420_rotated_frame.height != src_height) {
            free(i420_rotated_frame.data);
            i420_rotated_frame.data = (uint8_t *) malloc(y_size * 3 / 2);
            i420_rotated_frame.y = i420_rotated_frame.data;
            i420_rotated_frame.u = i420_rotated_frame.y + y_size;
            i420_rotated_frame.v = i420_rotated_frame.u + y_size / 4;
        }

        i420_rotated_frame.width = crop_width;
        i420_rotated_frame.height = crop_height;

    } else {
        if (i420_rotated_frame.width != src_height || i420_rotated_frame.height != src_width) {
            free(i420_rotated_frame.data);
            i420_rotated_frame.data = (uint8_t *) malloc(y_size * 3 / 2);
            i420_rotated_frame.y = i420_rotated_frame.data;
            i420_rotated_frame.u = i420_rotated_frame.y + y_size;
            i420_rotated_frame.v = i420_rotated_frame.u + y_size / 4;
        }

        i420_rotated_frame.width = crop_height;
        i420_rotated_frame.height = crop_width;
    }

    jint ret = ConvertToI420(src_frame, y_size,
                             i420_rotated_frame.y, i420_rotated_frame.width,
                             i420_rotated_frame.u, i420_rotated_frame.width / 2,
                             i420_rotated_frame.v, i420_rotated_frame.width / 2,
                             crop_x, crop_y,
                             src_width, need_flip ? -src_height : src_height,
                             crop_width, crop_height,
                             (enum RotationMode) rotate_degree, format);
    if (ret < 0) {
        return JNI_FALSE;
    }

    ret = I420Scale(i420_rotated_frame.y, i420_rotated_frame.width,
                    i420_rotated_frame.u, i420_rotated_frame.width / 2,
                    i420_rotated_frame.v, i420_rotated_frame.width / 2,
                    i420_rotated_frame.width, i420_rotated_frame.height,
                    i420_scaled_frame.y, i420_scaled_frame.width,
                    i420_scaled_frame.u, i420_scaled_frame.width / 2,
                    i420_scaled_frame.v, i420_scaled_frame.width / 2,
                    i420_scaled_frame.width, i420_scaled_frame.height,
                    kFilterNone);

    if (ret < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}