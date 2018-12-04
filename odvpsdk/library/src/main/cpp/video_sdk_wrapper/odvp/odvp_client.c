#include <sys/time.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "device.h"
#include "odvp_client.h"
#include "android_log.h"

#define TOKENBUFSIZE 32

/**
 * 创建设备
 * @param product_id
 * @param device_name
 * @return
 */
ont_device_t * device_create(ont_device_callback_t *cbs, void * cbs_weak_this) {

    ont_device_t *dev = NULL;
    int err = ont_device_create(&dev, cbs);
    if (ONT_ERR_OK != err) {

        dev = NULL;
    } else {

        dev->device_cbs_weak_this = cbs_weak_this;
    }

    return dev;
}

int device_connect(ont_device_t * dev, uint64_t device_id) {

    if (NULL == dev) {

        return ONT_ERR_BADPARAM;
    }

    static char token[TOKENBUFSIZE];
    dev->product_id = 1;
    dev->device_id = device_id;

    int err = ont_device_get_acc(dev, token);
    if (ONT_ERR_OK != err) {
        LOGE("Failed to get acc service, error=%d\n", err);
        return err;
    }

    err = ont_device_connect(dev);
    if (ONT_ERR_OK != err) {
        ont_device_destroy(dev);
        LOGE("Failed to connect to the server, error=%d\n", err);
        return err;
    }

    err = ont_device_verify(dev, token);
    if (ONT_ERR_OK != err) {
        LOGE("Failed to verify device, error=%d\n", err);
        return err;
    }
    return err;
}

void device_destroy(ont_device_t * dev) {

    ont_device_destroy(dev);
    dev = NULL;
}

int device_upload_picture(ont_device_t *dev, const char *filename, const char *name, int channel_id)
{
    FILE *fp = fopen(filename, "r");

    int ret = ONT_ERR_OK;

    if (fp)
    {
        fseek(fp, 0L, SEEK_END);

        long  flen = ftell(fp);
        char *buff = ont_platform_malloc(flen);

        fseek(fp,0L,SEEK_SET);
        fread(buff,flen,1,fp);

        uint16_t name_len = 0;
        //去掉后缀名
        char *p = strrchr(name, '.');

        if (p) {
            name_len = p - name;
        } else {
            name_len = strlen(name);
        }

        ret = ont_device_upload_picture(dev, channel_id, 2, name, name_len, buff, flen);

        ont_platform_free(buff);
        fclose(fp);
    }

    return ret;
}