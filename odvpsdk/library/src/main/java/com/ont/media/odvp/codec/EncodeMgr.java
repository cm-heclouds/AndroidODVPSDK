package com.ont.media.odvp.codec;

import com.ont.media.odvp.record.Mp4Record;
import com.ont.media.odvp.stream.OntStreamPusher;

/**
 * Created by betali on 2018/4/17.
 */

public class EncodeMgr {

    private boolean mAudio = true;
    private long mStartTimeUs;
    private AudioEncode mAudioEncode;
    private VideoEncode mVideoEncode;

    public EncodeMgr(OntStreamPusher ontStreamPusher, Mp4Record mp4Record) {

        mAudioEncode = new AudioEncode(ontStreamPusher, mp4Record);
        mVideoEncode = new VideoEncode(ontStreamPusher, mp4Record);
    }

    // is open audio
    public void setAudio(boolean audio) {
        this.mAudio = audio;
    }

    // audio config
    public void setAudioSampleRate(int sampleRate) {

        mAudioEncode.setSampleRate(sampleRate);
    }

    public void setAudioChannelConfig(int channelConfig) {

        mAudioEncode.setChannelConfig(channelConfig);
    }

    public void setAudioFormat(int format) {

        mAudioEncode.setFormat(format);
    }

    public void setAudioBitrate(int bitrate) {

        mAudioEncode.setBitrate(bitrate);
    }

    // video config
    public void setVideoWidth(int width) {

        mVideoEncode.setWidth(width);
    }

    public void setVideoHeight(int height) {

        mVideoEncode.setHeight(height);
    }

    public void setVideoBitrate(int bitrate) {

        mVideoEncode.setBitrate(bitrate);
    }

    public void setVideoIFrameInterval(int iFrameInterval) {

        mVideoEncode.setIFrameInterval(iFrameInterval);
    }

    public void setVideoFrameRate(int frameRate) {

        mVideoEncode.setFrameRate(frameRate);
    }

    public void setVideoColorFormat(int colorFormat) {

        mVideoEncode.setColorFormat(colorFormat);
    }

    public void start() {

        mStartTimeUs = System.nanoTime() / 1000L;

        if (mAudio) {
            mAudioEncode.init();
        }
        mVideoEncode.init();

        if (mAudio) {
            mAudioEncode.start();
        }
        mVideoEncode.start();
    }

    public void stop() {

        if (mAudio) {
            mAudioEncode.stop();
        }
        mVideoEncode.stop();
    }

    public void onGetAudioFrame(byte[] pcmFrame, int length) {

        mAudioEncode.onGetFrame(pcmFrame, length, mStartTimeUs);
    }

    public void onGetVideoRgbaFrame(byte[] yuvFrame, int width, int height) {

        mVideoEncode.onGetRGBAFrame(yuvFrame, width, height, mStartTimeUs);
    }

    public void onGetVideoI420Frame(byte[] yuvFrame, int length) {

        mVideoEncode.onGetI420Frame(yuvFrame, length, mStartTimeUs);
    }

    public void onGetVideoNV12Frame(byte[] yuvFrame, int length) {

        mVideoEncode.onGetNV12Frame(yuvFrame, length, mStartTimeUs);
    }
}
