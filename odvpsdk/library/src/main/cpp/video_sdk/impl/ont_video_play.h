#ifndef _ONT_VIDEO_PLAY_H
#define _ONT_VIDEO_PLAY_H

#ifdef __cplusplus
extern "C" {
#endif

#include "device.h"
#include "platform_cmd.h"

extern int
ont_video_playlist_singlestep(void *dev);

extern void
ont_keepalive_resp_callback(ont_device_t *dev);

extern int
ont_video_live_stream_start(void *dev, int channel);

extern int32_t
user_defind_cmd(ont_device_t *dev, ont_device_cmd_t *cmd);

extern int32_t
api_defind_msg(ont_device_t *dev, char *msg, size_t msg_len);

extern int32_t
_ont_cmd_live_stream_ctrl(void *dev, int32_t channel, int32_t level);

extern int32_t
plat_resp_dev_push_stream_msg(ont_device_t *dev, ont_plat_resp_dev_ps_t *msg);

extern int32_t
ont_video_live_stream_rtmp_publish
		(
				ont_device_t *dev,
				int32_t channel,
				uint8_t protype,
				uint16_t ttl_min,
				const char *push_url
		);

extern int32_t
_ont_cmd_dev_ptz_ctrl
		(
				void *dev,
				int32_t channel,
				int32_t mode,
				t_ont_video_ptz_cmd ptz,
				int32_t speed
		);

extern int32_t
ont_vod_start_notify
		(
				ont_device_t *dev,
				int32_t channel,
				uint8_t protype,
				ont_video_file_t *fileinfo,
				const char *playflag,
				const char *pushurl,
				uint16_t ttl
		);

#ifdef __cplusplus
}
#endif

#endif
