package com.ont.media.odvp.codec;

import com.ont.media.odvp.model.PublishConfig;
import com.ont.media.odvp.record.Mp4Record;
import com.ont.media.odvp.stream.OntStreamPusher;

/**
 * Created by betali on 2018/4/17.
 */

public class EncodeMgr {

    private long mStartTimeUs;
    private AudioEncode mAudioEncode;
    private VideoEncode mVideoEncode;
    private boolean isEnableAudio;

    public EncodeMgr(OntStreamPusher ontStreamPusher, Mp4Record mp4Record) {

        mAudioEncode = new AudioEncode(ontStreamPusher, mp4Record);
        mVideoEncode = new VideoEncode(ontStreamPusher, mp4Record);
    }

    public void setPublishConfig(PublishConfig publishConfig) {

        isEnableAudio = publishConfig.isEnableAudio();
        mAudioEncode.setPublishConfig(publishConfig);
        mVideoEncode.setPublishConfig(publishConfig);
    }

    public int chooseVideoColorFormat(String name) {

        return mVideoEncode.chooseColorFormat(name);
    }

    public boolean start() {

        mStartTimeUs = System.nanoTime() / 1000L;

        if (isEnableAudio) {
            mAudioEncode.init();
        }
        if (!mVideoEncode.init()) {

            return false;
        }

        if (isEnableAudio) {
            mAudioEncode.start();
        }
        mVideoEncode.start();
        return true;
    }

    public void stop() {

        if (isEnableAudio) {
            mAudioEncode.stop();
        }
        mVideoEncode.stop();
    }

    public void onGetAudioFrame(byte[] pcmFrame, int length) {

        mAudioEncode.onGetFrame(pcmFrame, length, mStartTimeUs);
    }

    public void onGetVideoRgbaFrame(byte[] rgbaFrame, int width, int height) {

        mVideoEncode.onGetRGBAFrame(rgbaFrame, width, height, mStartTimeUs);
    }

    public void onGetVideoI420Frame(byte[] yuvFrame, int length) {

        mVideoEncode.onGetI420Frame(yuvFrame, length, mStartTimeUs);
    }

    public void onGetVideoNV12Frame(byte[] yuvFrame, int length) {

        mVideoEncode.onGetNV12Frame(yuvFrame, length, mStartTimeUs);
    }

    public void onGetVideoNV21Frame(byte[] nv21Frame, boolean flip, int rotation, int width, int height) {

        mVideoEncode.onGetNV21Frame(nv21Frame, flip, rotation, width, height, mStartTimeUs);
    }
}
