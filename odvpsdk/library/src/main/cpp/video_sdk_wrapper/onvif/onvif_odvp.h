#ifndef ONVIF_ODVP_H
#define ONVIF_ODVP_H

#include <stdint.h>

# ifdef __cplusplus
extern "C" {
# endif

int ont_add_onvif_devices(ont_device_t *dev);

int ont_video_dev_channel_sync(void *dev);

# ifdef __cplusplus
}
# endif
#endif