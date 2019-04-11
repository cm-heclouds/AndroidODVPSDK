package com.ont.media.odvp.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.ont.media.odvp.player.AudioPlayer;
import com.ont.media.odvp.utils.OntLog;
import com.ont.media.odvp.def.IEncodeDef;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by betali on 2018/6/8.
 */

public class AudioDecode {

    private final String TAG = "AudioDecode";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private AudioPlayer mHostPlayer;
    private MediaCodec mDecoder;

    // config
    private int mSampleRate = IEncodeDef.AUDIO_SAMPLE_RATE;
    private int mChannelCount = IEncodeDef.AUDIO_CHANNEL_COUNT;
    private int mFormat = IEncodeDef.AUDIO_SAMPLE_SIZE;
    private int mBitrate = IEncodeDef.AUDIO_BITRATE;
    private int mKeyProfile = IEncodeDef.AUDIO_KEY_PROFILE;

    public AudioDecode(AudioPlayer hostPlayer) {
        this.mHostPlayer = hostPlayer;
    }

    public void setSampleRate(int sampleRate) {

        this.mSampleRate = sampleRate;
    }

    public void setChannelCount(int channelCount) {

        this.mChannelCount = channelCount;
    }

    public void setKeyProfile(int keyProfile) {

        this.mKeyProfile = keyProfile;
    }

    public boolean init(byte[] config) {

        try {

            mDecoder = MediaCodec.createDecoderByType(AUDIO_MIME_TYPE);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, mKeyProfile);

            /*byte[] bytes = new byte[]{(byte) 0x12, (byte)0x10};*/
            ByteBuffer bb = ByteBuffer.wrap(config);
            mediaFormat.setByteBuffer("csd-0", bb);

            mDecoder.configure(mediaFormat, null, null, 0);
        } catch (IOException e) {

            e.printStackTrace();
            return false;
        }

        mDecoder.start();
        return true;
    }

    public void decode(byte[] buf, int offset, int length, int ts) {

        if (length == 9) {

            return;
        }

        ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();

        try {

            int inputBufIndex = mDecoder.dequeueInputBuffer(-1);
            if (inputBufIndex >= 0) {

                ByteBuffer inputBuffer = codecInputBuffers[inputBufIndex];
                inputBuffer.clear();
                inputBuffer.put(buf, offset, length);
                mDecoder.queueInputBuffer(inputBufIndex, 0, length, ts * 1000, 0);
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteBuffer outputBuffer;
            while (true) {

                int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, 0);
                if (outputBufferIndex >= 0) {

                    outputBuffer = codecOutputBuffers[outputBufferIndex];
                    outputBuffer.position(info.offset);
                    outputBuffer.limit(info.offset + info.size);
                    byte[] outData = new byte[info.size];
                    outputBuffer.get(outData);
                    outputBuffer.clear();
                    mHostPlayer.playAudioTrack(outData, 0, info.size);
                    mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                    codecOutputBuffers = mDecoder.getOutputBuffers();
                } else {

                    break;
                }
            }
        } catch (Exception e) {

            OntLog.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 释放资源
     */
    public void release() {

        try {

            if (mDecoder != null) {

                mDecoder.stop();
                mDecoder.release();
                mDecoder = null;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
