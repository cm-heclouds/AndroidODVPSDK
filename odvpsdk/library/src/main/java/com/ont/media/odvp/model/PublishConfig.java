package com.ont.media.odvp.model;

import android.media.AudioFormat;

import com.ont.media.odvp.def.IEncodeDef;

/**
 * Created by betali on 2018/12/6.
 */
public class PublishConfig {

    public static class AudioSampleRate {

        public static int TYPE_UNKNOWN = -1;
        public static int TYPE_5_5KHz = 0;
        public static int TYPE_11KHz = 1;
        public static int TYPE_22KHz = 2;
        public static int TYPE_44KHz = 3;
    };

    public static class AudioSampleSize {

        public static int TYPE_UNKNOWN = -1;
        public static int TYPE_8_BIT = 0;
        public static int TYPE_16_BIT = 1;
    }

    public static class AudioChannelConfig {

        public static int TYPE_UNKNOWN = -1;
        public static int TYPE_MONO_CHANNEL = 0;
        public static int TYPE_STEREO_CHANNEL = 1;
    }

    boolean mEnableAudio = true;
    boolean mWaterMark = true;
    int mVideoFrameInterval = IEncodeDef.VIDEO_IFRAME_INTERVAL;
    int mVideoFrameRate = IEncodeDef.VIDEO_FRAME_RATE;
    int mVideoColorFormat;
    int mVideoBitrate;
    int mAudioSampleRate = IEncodeDef.AUDIO_SAMPLE_RATE;
    int mAudioSampleSize = IEncodeDef.AUDIO_SAMPLE_SIZE;
    int mAudioBitrate = IEncodeDef.AUDIO_BITRATE;
    int mAudioChannelConfig;
    int mAudioSource = IEncodeDef.AUDIO_SOURCE;
    int mWidth;
    int mHeight;

    public PublishConfig clone() {

        PublishConfig newObj = new PublishConfig();
        newObj.setEnableAudio(this.mEnableAudio);
        newObj.setWaterMark(this.mWaterMark);
        newObj.setVideoFrameInterval(this.mVideoFrameInterval);
        newObj.setVideoFrameRate(this.mVideoFrameRate);
        newObj.setVideoColorFormat(this.mVideoColorFormat);
        newObj.setVideoBitrate(this.mVideoBitrate);
        newObj.setAudioSampleRate(this.mAudioSampleRate);
        newObj.setAudioSampleSize(this.mAudioSampleSize);
        newObj.setAudioBitrate(this.mAudioBitrate);
        newObj.setAudioChannelConfig(this.mAudioChannelConfig);
        newObj.setAudioSource(this.mAudioSource);
        newObj.setWidth(this.mWidth);
        newObj.setHeight(this.mHeight);
        return newObj;
    }

    public void copy(PublishConfig publishConfig) {

        this.setEnableAudio(publishConfig.mEnableAudio);
        this.setWaterMark(publishConfig.mWaterMark);
        this.setVideoFrameInterval(publishConfig.mVideoFrameInterval);
        this.setVideoFrameRate(publishConfig.mVideoFrameRate);
        this.setVideoColorFormat(publishConfig.mVideoColorFormat);
        this.setVideoBitrate(publishConfig.mVideoBitrate);
        this.setAudioSampleRate(publishConfig.mAudioSampleRate);
        this.setAudioSampleSize(publishConfig.mAudioSampleSize);
        this.setAudioBitrate(publishConfig.mAudioBitrate);
        this.setAudioChannelConfig(publishConfig.mAudioChannelConfig);
        this.setAudioSource(publishConfig.mAudioSource);
        this.setWidth(publishConfig.mWidth);
        this.setHeight(publishConfig.mHeight);
    }

    public PublishConfig setEnableAudio(boolean enableAudio) {

        this.mEnableAudio = enableAudio;
        return this;
    }

    public PublishConfig setWaterMark(boolean waterMark) {

        this.mWaterMark = waterMark;
        return this;
    }

    public PublishConfig setVideoFrameInterval(int videoFrameInterval) {
        this.mVideoFrameInterval = videoFrameInterval;
        return this;
    }

    public PublishConfig setVideoFrameRate(int videoFrameRate) {
        this.mVideoFrameRate = videoFrameRate;
        return this;
    }

    public PublishConfig setVideoColorFormat(int videoColorFormat) {
        this.mVideoColorFormat = videoColorFormat;
        return this;
    }

    public PublishConfig setVideoBitrate(int videoBitrate) {
        this.mVideoBitrate = videoBitrate;
        return this;
    }

    public PublishConfig setAudioSampleRate(int audioSampleRate) {
        this.mAudioSampleRate = audioSampleRate;
        return this;
    }

    public PublishConfig setAudioSampleSize(int audioSampleSize) {
        this.mAudioSampleSize = audioSampleSize;
        return this;
    }

    public PublishConfig setAudioBitrate(int audioBitrate) {
        this.mAudioBitrate = audioBitrate;
        return this;
    }

    public PublishConfig setAudioChannelConfig(int audioChannelConfig) {
        this.mAudioChannelConfig = audioChannelConfig;
        return this;
    }

    public PublishConfig setAudioSource(int audioSource) {
        this.mAudioSource = audioSource;
        return this;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public boolean isEnableAudio() {
        return mEnableAudio;
    }

    public boolean isWaterMark() { return mWaterMark; }

    public int getVideoFrameInterval() {
        return mVideoFrameInterval;
    }

    public int getVideoFrameRate() {
        return mVideoFrameRate;
    }

    public int getVideoColorFormat() {
        return mVideoColorFormat;
    }

    public int getVideoBitrate() {
        return mVideoBitrate;
    }

    public int getAudioSampleRate() {
        return mAudioSampleRate;
    }

    public int getAudioSampleSize() {
        return mAudioSampleSize;
    }

    public int getAudioBitrate() {
        return mAudioBitrate;
    }

    public int getAudioChannelConfig() {
        return mAudioChannelConfig;
    }

    public int getAudioSource() {
        return mAudioSource;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int sampleRate2Num() {

        //96000, 88200, 64000, 48000, 44100, 32000,
        //24000, 22050, 16000, 12000, 11025, 8000, 0
        //0 = 5.5 kHz, 1 = 11 kHz, 2 = 22 kHz, 3 = 44 kHz
        if (mAudioSampleRate < 11025) {

            return AudioSampleRate.TYPE_5_5KHz;
        } else if (mAudioSampleRate >= 11025 && mAudioSampleRate < 22050) {

            return AudioSampleRate.TYPE_11KHz;
        } else if (mAudioSampleRate >= 22050 && mAudioSampleRate < 44100) {

            return AudioSampleRate.TYPE_22KHz;
        } else {

            return AudioSampleRate.TYPE_44KHz;
        }
    }

    public int sampleSize2Num() {

        // 0 = 8-bit samples   1 = 16-bit samples
        if (mAudioSampleSize == AudioFormat.ENCODING_PCM_16BIT) {

            return AudioSampleSize.TYPE_16_BIT;
        } else if (mAudioSampleSize == AudioFormat.ENCODING_PCM_8BIT) {

            return AudioSampleSize.TYPE_8_BIT;
        } else {

            return AudioSampleSize.TYPE_UNKNOWN;
        }
    }

    public int channelConfig2Num() {

        // 0 = Mono sound     1 = Stereo sound
        if (mAudioChannelConfig == AudioFormat.CHANNEL_IN_STEREO) {

            return AudioChannelConfig.TYPE_STEREO_CHANNEL;
        } else if (mAudioChannelConfig == AudioFormat.CHANNEL_IN_MONO) {

            return AudioChannelConfig.TYPE_MONO_CHANNEL;
        } else {

            return AudioChannelConfig.TYPE_UNKNOWN;
        }
    }
}
