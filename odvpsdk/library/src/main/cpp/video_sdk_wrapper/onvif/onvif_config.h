#ifndef _ONVIF_CONFIG_H_
#define _ONVIF_CONFIG_H_

#include "ont_list.h"
#include "device_onvif.h"

# ifdef __cplusplus
extern "C" {
# endif
struct _onvif_channel {
	uint32_t channelid;
	char url[1024];
	char user[128];
	char pass[128];
	char title[255];
	char desc[255];
};

int device_cfg_initilize(const char *config_buf);

int cfg_get_channel_num();

struct _onvif_channel *cfg_get_channels();

struct _onvif_channel *cfg_get_channel(int channelid);

device_onvif_cluster_t *cfg_get_cluster();

# ifdef __cplusplus
}
# endif
#endif