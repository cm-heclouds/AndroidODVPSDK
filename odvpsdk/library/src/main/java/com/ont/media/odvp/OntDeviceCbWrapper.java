package com.ont.media.odvp;

import com.ont.media.odvp.model.PlatRespDevPS;
import com.ont.media.odvp.model.PlatformCmd;
import com.ont.media.odvp.model.VideoFileInfo;

import java.lang.ref.WeakReference;

/**
 * Created by betali on 2018/6/13.
 */

public class OntDeviceCbWrapper {

    public static void onKeepAliveResp(Object weakThis) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return;
        }

        deviceCb.onKeepAliveResp();
    }

    public static int onLiveStreamStart(Object weakThis, final int channel, final byte proType, final int ttl, final String pushUrl) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return -1;
        }

        return deviceCb.onLiveStreamStart(channel, proType, ttl, pushUrl);
    }

    public static int onVodStreamStart(Object weakThis, int channelId, byte proType, VideoFileInfo fileInfo, String playFlag, String pushUrl, int ttl) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return -1;
        }

        return deviceCb.onVodStreamStart(channelId, proType, fileInfo, playFlag, pushUrl, ttl);
    }

    public static int onChannelRecordUpdate(Object weakThis, int channel, int status, int seconds, String url) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return -1;
        }

        return deviceCb.onChannelRecordUpdate(channel, status, seconds, url);
    }

    public static int onUserDefinedCmd(Object weakThis, PlatformCmd cmd) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return -1;
        }

        return deviceCb.onUserDefinedCmd(cmd);
    }

    public static int onApiDefinedMsg(Object weakThis, byte[] msg, int msgLen) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return -1;
        }

        return deviceCb.onApiDefinedMsg(msg, msgLen);
    }

    /*public static int onPlatRespDevPushStreamMsg(Object weakThis, PlatRespDevPS resp) {

        IOntDeviceCb deviceCb = ((WeakReference<IOntDeviceCb>) weakThis).get();
        if (deviceCb == null) {

            return -1;
        }

        return deviceCb.onPlatRespDevPushStreamMsg(resp);
    }*/
}
