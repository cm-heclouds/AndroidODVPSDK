#include <time.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>

#include "log.h"
#include "rvod.h"
#include "device.h"
#include "config.h"
#include "rtmp_if.h"
#include "security.h"
#include "ont_list.h"
#include "ont_bytes.h"
#include "status_sync.h"
#include "platform_cmd.h"
#include "sample_config.h"
#include "video_rvod_mp4.h"
#include "ont_packet_queue.h"
#include "rtmp_log.h"

#ifdef ONT_OS_POSIX
#include <signal.h>
#endif

#ifdef WIN32
#include "windows.h"
#include "Dbghelp.h"
#include "tchar.h"
#include "assert.h"
#pragma comment(lib, "Dbghelp.lib")
#endif



#if(PROTOCOL_SECURITY_MBEDTLS ||  PROTOCOL_SECURITY_OPENSSL)
#define ONT_VIDEO_PROTOCOL_ENCRYPT 1
#endif


#ifdef WIN32
void CreateDumpFile(LPCWSTR lpstrDumpFilePathName, EXCEPTION_POINTERS *pException)
{
	TCHAR szFull[260];
	TCHAR szDrive[3];
	TCHAR szDir[256];
	GetModuleFileName(NULL, szFull, sizeof(szFull) / sizeof(char));
	_tsplitpath(szFull, szDrive, szDir, NULL, NULL);
	strcpy(szFull, szDrive);
	strcat(szFull, szDir);
	strcat(szFull, "//");
	strcat(szFull, lpstrDumpFilePathName);

	// 创建Dump文件
	//
	HANDLE hDumpFile = CreateFile(szFull, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

	// Dump信息
	//
	MINIDUMP_EXCEPTION_INFORMATION dumpInfo;
	dumpInfo.ExceptionPointers = pException;
	dumpInfo.ThreadId = GetCurrentThreadId();
	dumpInfo.ClientPointers = TRUE;

	// 写入Dump文件内容
	//
	MiniDumpWriteDump(GetCurrentProcess(), GetCurrentProcessId(), hDumpFile, MiniDumpNormal, &dumpInfo, NULL, NULL);

	CloseHandle(hDumpFile);
}

LONG ApplicationCrashHandler(EXCEPTION_POINTERS *pException)
{
	//FatalAppExit(-1, "*** Unhandled Exception! ***");

	CreateDumpFile("video_sample.dmp", pException);

	return EXCEPTION_EXECUTE_HANDLER;
}

#endif

void GetWorkDir(char *pBuf)
{
	//char pBuf[MAX_PATH];   
#ifdef WIN32
	GetCurrentDirectory(MAX_PATH, pBuf);
	strcat(pBuf, "\\");
#endif
}

int32_t _ont_cmd_live_stream_ctrl(void *dev, int32_t channel, int32_t level);
int32_t _ont_cmd_dev_ptz_ctrl(void *dev, int32_t channel, int32_t mode, t_ont_video_ptz_cmd ptz, int32_t speed);
int32_t _ont_cmd_dev_query_files(void *dev, int32_t channel, int32_t startindex, int32_t max, const char *starTime, const char *endTime, ont_video_file_t **files, int32_t *filenum, int32_t *totalnum);



void ont_keepalive_resp_callback(ont_device_t *dev);
int ont_video_live_stream_start(void *dev, int channel);
int ont_video_live_stream_rtmp_publish(ont_device_t *dev, int32_t channel, uint8_t protype, uint16_t ttl_min, const char *push_url);
int ont_record_channel_cmd_update(void *dev, int32_t channel, int32_t status, int32_t seconds, char url[255]);

int ont_device_cmd_upload_picture_test(ont_device_t *dev, const char *filename, int channel_id);

int32_t ont_vod_start_notify(ont_device_t *dev, int32_t channel, uint8_t protype, ont_video_file_t *fileinfo, const char *playflag, const char*pushurl, uint16_t ttl);
int32_t user_defind_cmd(ont_device_t *dev, ont_device_cmd_t *cmd);
int32_t api_defind_msg(ont_device_t *dev, char *msg, size_t msg_len);
int32_t plat_resp_dev_push_stream_msg(ont_device_t *dev, ont_plat_resp_dev_ps_t *msg);

ont_device_callback_t _g_device_cbs =
{
	ont_keepalive_resp_callback,
	ont_video_live_stream_rtmp_publish,
	ont_vod_start_notify,
	ont_record_channel_cmd_update,
	user_defind_cmd,
	api_defind_msg,
	plat_resp_dev_push_stream_msg
};

ont_cmd_callbacks_t _g_ont_cmd =
{
	_ont_cmd_live_stream_ctrl,
	_ont_cmd_dev_ptz_ctrl,
	_ont_cmd_dev_query_files
};


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
			RTMP_Log(RTMP_LOGERROR, "Failed to login the onvif server");
		}
		else {
			//uint8_t devid[32];
			//ont_platform_snprintf((char *)devid, 32, "%d", dev->device_id);
			ont_video_live_stream_start(dev, channels[i].channelid);
		}
	}
	return j;
}


int ont_device_cmd_upload_picture_test(ont_device_t *dev, const char *filename, int channel_id)
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
       char *p = strrchr(filename, '.'); //去掉后缀名

       if (p) {
           name_len = p - filename;
       } else {
           name_len = strlen(filename);
       }
                 
       ret = ont_device_upload_picture(dev, channel_id, 2, filename, name_len, buff, flen);  
       
       ont_platform_free(buff);
       fclose(fp);
    }

    return ret; 
}

#define IPSIZE 40
#define TOKENBUFSIZE 32

extern int initialize_environment(void);
extern int ont_video_playlist_singlestep(void *dev);

int main(int argc, char *argv[])
{
	//log_redirect();
	initialize_environment();
	ont_security_init();

#ifdef WIN32
	SetUnhandledExceptionFilter((LPTOP_LEVEL_EXCEPTION_FILTER)ApplicationCrashHandler);
#endif

	//ont_onvif_device_discovery();
	ont_device_t *dev=NULL;
	int err;
	int delta = 0;

    #if 0 /* unused variable */
	ont_device_cmd_t *cmd = NULL;
    #endif

	char cfg_path[256] = {0};
	GetWorkDir(cfg_path);
	strcat(cfg_path, "E:\\share\\video_project\\video_sdk\\bin\\Debug\\config.json");
	RTMP_Log(RTMP_LOGINFO, "cfg_path:%s\n", cfg_path);
#ifdef WIN32
#ifdef _DEBUG
    cfg_initilize("config.json");
#else
	cfg_initilize(cfg_path);
#endif

#else 
	cfg_initilize(cfg_path);
#endif
#ifdef _DEBUG
	RTMP_LogSetLevel(RTMP_LOGDEBUG);
#else
	RTMP_LogSetLevel(RTMP_LOGINFO);
#endif

	//cfg_initilize("config.json");
	uint64_t product_id = cfg_get_profile()->productid;
	uint64_t device_id = cfg_get_profile()->deviceid;

#ifdef ONT_OS_POSIX
	struct sigaction sa;
	sa.sa_handler = SIG_IGN;//设定接受到指定信号后的动作为忽略
	sa.sa_flags = 0;
	if (sigemptyset(&sa.sa_mask) == -1 || //初始化信号集为空
		sigaction(SIGPIPE, &sa, 0) == -1)
	{
		//屏蔽SIGPIPE信号
		exit(1);
	}
#endif


	char token[TOKENBUFSIZE];

	err = ont_device_create(&dev, &_g_device_cbs);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to create device instance, error=%d\n", err);
        getchar();
		return -1;
	}

	(void)ont_add_onvif_devices(dev);

	err = ont_device_set_platformcmd_handle(dev, &_g_ont_cmd);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to set platformcmd handle, error=%d\n", err);
        getchar();
		return -1;
	}
	
__restart:
	ont_platform_sleep(5000);

	dev->product_id = product_id;
	dev->device_id = device_id;
	dev->encrypt_flag = 0;
	memset(token, 0x00, sizeof(token));
	err = ont_device_get_acc(dev, token);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to get acc service, error=%d\n", err);
		goto __restart;
	}
	RTMP_Log(RTMP_LOGINFO, "ip %s port %d token", dev->acc_ip, dev->port);
	RTMP_LogHex(RTMP_LOGINFO, (const uint8_t *)token, sizeof(token));
	err = ont_device_connect(dev);
	if (ONT_ERR_OK != err)
	{
		RTMP_Log(RTMP_LOGERROR, "Failed to connect to the server, error=%d\n", err);
		goto __restart;
	}
	err = ont_device_verify(dev, token);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to verify device, error=%d\n", err);
		goto __restart;
	}
	int pf_request_timeout = 1000;
#if ONT_VIDEO_PROTOCOL_ENCRYPT
	err = ont_device_request_rsa_publickey(dev);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to request rsa publickey, error=%d\n", err);
		goto __restart;
	}
	//need wait request platform key
  	pf_request_timeout=10000;
#endif


	err = ont_device_check_receive(dev, pf_request_timeout);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to receive, error=%d\n", err);
		goto __restart;
	}

	const char *id = cfg_get_profile()->id;
	const char *reg_code = cfg_get_profile()->key;//pass = key
	err = ont_device_register(dev, product_id, reg_code, id);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to register device, error=%d\n", err);
		goto __restart;
	}

	char auth_code_new[128];
	err = ont_device_auth(dev, dev->auth_code, auth_code_new);
	if (ONT_ERR_OK != err) {
		RTMP_Log(RTMP_LOGERROR, "Failed to  authentification, error=%d\n", err);
		goto __restart;
	}

	RTMP_Log(RTMP_LOGINFO, "get dev id %ld\n", dev->device_id);


	/*add channels*/
	int i = 0;
	int num = cfg_get_channel_num();
	struct _onvif_channel *_channels = cfg_get_channels();
	for (; i < num; i++)
	{
		err = ont_device_add_channel(dev, _channels[i].channelid, _channels[i].title, strlen(_channels[i].title), _channels[i].desc, strlen(_channels[i].desc));
	}

#ifdef _DEBUG
    /*图片上传测试*/
    if (num > 0)
    {
        //err = ont_device_cmd_upload_picture_test(dev, "OneNET.JPG", _channels[0].channelid);
	    if (ONT_ERR_OK != err) 
	    {
		    RTMP_Log(RTMP_LOGERROR, "Failed to upload picture, error=%d\n", err);
	    }        
    }
#endif

	uint32_t tm_last = ont_platform_time();
	volatile uint32_t tm_now;

	//ont_record_channel_cmd_update(dev, 1, 1, 10, "testurl");

    /*update local time*/

	struct ont_timeval tm;
    ont_device_get_systime(dev, &tm);
    ont_settimeofday(&tm, NULL);

#if 0
	/* 接口测试 */
	/* 设备请求推流，并根据平台回复调用设备相应回调函数作后续处理 */
	err = ont_device_req_push_stream(dev, 1, 10);
	if (ONT_ERR_OK != err)
	{
		RTMP_Log(RTMP_LOGERROR, "%s(%d), Failed to request to push stream, error=%d", __FUNCTION__, __LINE__, err);
	}
#endif

	while (1)
	{
		if (delta)
		{
			ont_platform_sleep(delta>100 ? 100 : delta);
		}
		tm_now = ont_platform_time();
		if (tm_now - tm_last > 30)
		{
			err = ont_device_keepalive(dev);
			if (err != 0)
			{
				/*error */
				RTMP_Log(RTMP_LOGERROR, "device is not alive, error=%d", err);
				break;
			}
			tm_last = tm_now;
		}
		err = ont_device_check_receive(dev, 0);
		if (err != 0)
		{
#ifdef WIN32
			RTMP_Log(RTMP_LOGINFO, "win sock err %d", GetLastError());
#endif
			/*error */
			RTMP_Log(RTMP_LOGERROR, "check recv error=%d", err);
			break;
		}
		delta = ont_video_playlist_singlestep(dev);
	}

	goto __restart;
	ont_device_destroy(dev);
	printf("press any key to exit\n");
	getchar();
	return 0;
}
