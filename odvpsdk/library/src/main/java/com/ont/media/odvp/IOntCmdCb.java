package com.ont.media.odvp;

/**
 * Created by betali on 2018/4/9.
 */

public interface IOntCmdCb {

    int onCmdLiveStreamCtrl(int channelId, int level);
    int onCmdPtzCtrl(int channelId, int mode, int ptz, int speed);
    int onCmdQueryFiles(int channelId, int page, int startIndex, int max, String startTime, String endTime, String cmdId);
}
