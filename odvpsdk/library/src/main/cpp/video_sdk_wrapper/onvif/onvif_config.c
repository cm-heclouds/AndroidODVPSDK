#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "onvif_config.h"
#include "cJSON.h"

struct sample_onvif_cfg
{
	struct _onvif_channel*  channels;

	device_onvif_cluster_t *cluster;

	int channelnum;
};

struct sample_onvif_cfg onvif_cfg;

int  device_cfg_initilize(const char * config_buf)
{
	cJSON *json = NULL;
	char *title, *desc, *url, *user, *pass;
	char *end = NULL;

	json = cJSON_ParseWithOpts(config_buf, &end, 0);
	onvif_cfg.cluster = ont_onvif_device_cluster_create();

	/*
	 * Get onvif channels
	 */
	int j = 0;
	if (cJSON_HasObjectItem(json, "onvif")) {
		j = cJSON_GetArraySize(cJSON_GetObjectItem(json, "onvif"));
	}
	onvif_cfg.channelnum = j;
	onvif_cfg.channels = ont_platform_malloc(j * sizeof(struct _onvif_channel));

	cJSON *item;
	for (int i = 0; i < j; i++)
	{
		if (cJSON_HasObjectItem(json, "onvif")) {
			item = cJSON_GetArrayItem(cJSON_GetObjectItem(json, "onvif"), i);
		}

		if (cJSON_HasObjectItem(item, "url")) {
			url = cJSON_GetObjectItem(item, "url")->valuestring;
		}

		if (cJSON_HasObjectItem(item, "user")) {
			user = cJSON_GetObjectItem(item, "user")->valuestring;
		}

		if (cJSON_HasObjectItem(item, "passwd")) {
			pass = cJSON_GetObjectItem(item, "passwd")->valuestring;
		}

		if (cJSON_HasObjectItem(item, "title")) {
			title = cJSON_GetObjectItem(item, "title")->valuestring;
		}

		if (cJSON_HasObjectItem(item, "desc")) {
			desc = cJSON_GetObjectItem(item, "desc")->valuestring;

		}

		if (cJSON_HasObjectItem(item, "channel_id")) {
			onvif_cfg.channels[i].channelid = cJSON_GetObjectItem(item, "channel_id")->valueint;
		}

		ont_platform_snprintf(onvif_cfg.channels[i].url, sizeof(onvif_cfg.channels[i].url), url);
		ont_platform_snprintf(onvif_cfg.channels[i].user, sizeof(onvif_cfg.channels[i].user), user);
		ont_platform_snprintf(onvif_cfg.channels[i].pass, sizeof(onvif_cfg.channels[i].pass), pass);
		ont_platform_snprintf(onvif_cfg.channels[i].title, sizeof(onvif_cfg.channels[i].title), title);
		ont_platform_snprintf(onvif_cfg.channels[i].desc, sizeof(onvif_cfg.channels[i].desc), desc);
	}

	cJSON_Delete(json);
	return 0;
}

int  cfg_get_channel_num()
{
	return onvif_cfg.channelnum;
}

struct _onvif_channel * cfg_get_channels()
{
	return onvif_cfg.channels;
}

struct _onvif_channel * cfg_get_channel(int channelid)
{
    int i =0;
	for (; i < onvif_cfg.channelnum;i++)
	{
		if (onvif_cfg.channels[i].channelid == channelid)
		{
			return &onvif_cfg.channels[i];
		}
	}
	return NULL;
}

device_onvif_cluster_t* cfg_get_cluster( void )
{
	return onvif_cfg.cluster;
}