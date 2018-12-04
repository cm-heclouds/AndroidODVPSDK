package com.ont.media.odvp;

import java.lang.ref.WeakReference;

/**
 * Created by betali on 2018/6/13.
 */

public class OntCmdCbWrapper {

    public static int onCmdLiveStreamCtrl(Object weakThis, int channelId, int level) {

        IOntCmdCb cmdCb = ((WeakReference<IOntCmdCb>) weakThis).get();
        if (cmdCb == null) {

            return -1;
        }

        return cmdCb.onCmdLiveStreamCtrl(channelId, level);
    }

    public static int onCmdPtzCtrl(Object weakThis, int channelId, int mode, int ptz, int speed) {

        IOntCmdCb cmdCb = ((WeakReference<IOntCmdCb>) weakThis).get();
        if (cmdCb == null) {

            return -1;
        }

        return cmdCb.onCmdPtzCtrl(channelId, mode, ptz, speed);
    }

    public static int onCmdQueryFiles(Object weakThis, int channelId, int page, int startIndex, int max, String startTime, String endTime, String cmdId) {

        IOntCmdCb cmdCb = ((WeakReference<IOntCmdCb>) weakThis).get();
        if (cmdCb == null) {

            return -1;
        }

        return cmdCb.onCmdQueryFiles(channelId, page, startIndex, max, startTime, endTime, cmdId);
    }
}
