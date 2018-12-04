package com.ont.odvp.sample.def;


/**
 * Created by betali on 2018/1/13 0013.
 */

public interface IGLViewEventListener {

    void onGetCameraSupportResolutions();
    void onChangeCameraConfirmResolution(boolean isResolutionChange);
    void onGetVideoFrame(byte[] data, int width, int height);
}
