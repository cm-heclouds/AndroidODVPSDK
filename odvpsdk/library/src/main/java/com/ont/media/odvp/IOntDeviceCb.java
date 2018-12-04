package com.ont.media.odvp;

import com.ont.media.odvp.model.PlatRespDevPS;
import com.ont.media.odvp.model.PlatformCmd;
import com.ont.media.odvp.model.VideoFileInfo;

/**
 * Created by betali on 2018/4/9.
 */

public interface IOntDeviceCb {

    void onKeepAliveResp();
    int onLiveStreamStart(int channel, byte proType, int ttl, String pushUrl);
    int onVodStreamStart(int channelId, byte proType, VideoFileInfo fileInfo, String playFlag, String pushUrl, int ttl);
    int onChannelRecordUpdate(int channel, int status, int seconds, String url);
    int onUserDefinedCmd(PlatformCmd cmd);
    int onApiDefinedMsg(byte[] msg, int msgLen);
    //int onPlatRespDevPushStreamMsg(PlatRespDevPS resp);
}
