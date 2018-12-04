package com.ont.odvp.sample.def;

/**
 * Created by betali on 2018/1/17.
 */

public interface IPublishEventListener {

//    void onStartFinish(boolean success);
//    void onStopFinish();
//    void onCameraStartFinish(boolean success, boolean hasHardEncode);
//    void onMuxerInitFinish(boolean success);
    void onDisconnect();
    void onStopRecord();
    void onCameraResolutionUpdate();
}
