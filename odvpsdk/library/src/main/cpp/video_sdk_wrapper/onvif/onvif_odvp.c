#include <string.h>
#include "device.h"
#include "onvif_config.h"
#include "android_log.h"

extern int ont_video_live_stream_start(void *dev, int channel);
int ont_add_onvif_devices(ont_device_t *dev)
{
	int i;
	int j = cfg_get_channel_num();
	struct _onvif_channel * channels = cfg_get_channels();
	for (i = 0; i < j; i++)
	{
		if (ont_onvifdevice_adddevice(cfg_get_cluster(),
									  channels[i].channelid, channels[i].url, channels[i].user, channels[i].pass) != 0)
		{
			LOGE("Failed to login the onvif server");
		}
		else {
			ont_video_live_stream_start(dev, channels[i].channelid);
		}
	}
	return j;
}

int ont_video_dev_channel_sync(void *dev)
{
	int i = 0;
	int num = cfg_get_channel_num();
	struct _onvif_channel *_channels=cfg_get_channels();
	for (; i < num; i++)
	{
		ont_device_add_channel(dev, _channels[i].channelid, _channels[i].title, strlen(_channels[i].title), _channels[i].desc, strlen(_channels[i].desc));
	}
	return 0;
}

