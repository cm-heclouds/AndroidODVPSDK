package com.ont.media.odvp.model;

/**
 * Created by betali on 2018/2/11.
 */

public class RtmpMetaData {

    private double duration;               /**< 视频播放时间，直播视频置0*/
    private int    width;         /**< 视频宽度*/
    private int    height;        /**< 视频高度*/
    private int    videoDataRate; /**< 视频位率bps*/
    private int    frameRate;     /**< 视频帧率 */
    private int    videoCodecid;  /**< FLV视频格式参数，AVC 写7 */
    private boolean   hasAudio;      /**< 是否包含音频 */
    private int    audioDataRate; /**< 音频位率kbps*/
    private int    audioSampleRate;  /**< 音频播放频率, kbps*/
    private int    audioCodecid;     /**< FLV 音频格式参数, AAC 写10*/

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getVideoDataRate() {
        return videoDataRate;
    }

    public void setVideoDataRate(int videoDataRate) {
        this.videoDataRate = videoDataRate;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getVideoCodecid() {
        return videoCodecid;
    }

    public void setVideoCodecid(int videoCodecid) {
        this.videoCodecid = videoCodecid;
    }

    public boolean getHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public int getAudioDataRate() {
        return audioDataRate;
    }

    public void setAudioDataRate(int audioDataRate) {
        this.audioDataRate = audioDataRate;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioCodecid() {
        return audioCodecid;
    }

    public void setAudioCodecid(int audioCodecid) {
        this.audioCodecid = audioCodecid;
    }
}
