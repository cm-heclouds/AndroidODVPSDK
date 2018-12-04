package com.ont.media.odvp.player;

import android.media.AudioManager;
import android.media.AudioTrack;

import com.ont.media.odvp.utils.OntLog;

/**
 * Created by betali on 2018/6/8.
 */

public class AudioPlayerTrack {

    private int mFrequency;// 采样率
    private int mChannel;// 声道
    private int mSampBit;// 采样精度
    private AudioTrack mAudioTrack;

    public void setFrequency(int frequency) {

        this.mFrequency = frequency;
    }

    public void setChannel(int channel) {

        this.mChannel = channel;
    }

    public void setSampBit(int sampBit) {

        this.mSampBit = sampBit;
    }

    /**
     * 初始化
     */
    public void init() {

        if (mAudioTrack != null) {
            release();
        }

        // 获得构建对象的最小缓冲区大小
        int minBufSize = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrequency, mChannel, mSampBit, minBufSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    /**
     * 释放资源
     */
    public void release() {

        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    /**
     * 将解码后的pcm数据写入audioTrack播放
     *
     * @param data   数据
     * @param offset 偏移
     * @param length 需要播放的长度
     */
    public void playAudioTrack(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            OntLog.e("MyAudioTrack", "AudioTrack Exception : " + e.toString());
        }
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(mFrequency,
                mChannel, mSampBit);
    }
}
