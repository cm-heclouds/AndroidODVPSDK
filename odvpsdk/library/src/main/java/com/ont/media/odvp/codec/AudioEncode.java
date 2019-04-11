package com.ont.media.odvp.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;

import com.ont.media.odvp.model.PublishConfig;
import com.ont.media.odvp.record.Mp4Record;
import com.ont.media.odvp.stream.OntStreamPusher;
import com.ont.media.odvp.utils.OntLog;
import com.ont.media.odvp.def.IEncodeDef;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//import com.tencent.mars.xlog.Log;

/**
 * Created by betali on 2018/1/15.
 */

public class AudioEncode {

    private static final String TAG = AudioEncode.class.getSimpleName();
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    // config
    private PublishConfig mPublishConfig;
    private byte[] mSeqData;

    private MediaCodec mEncoder;
    private OntStreamPusher mOntStreamPusher;
    private Mp4Record mMp4Record;
    private MediaCodec.BufferInfo mBufferInfo;
    private Map<Long, Object> mReserveBuffers;
    private int mMp4Track;

    public AudioEncode(OntStreamPusher ontStreamPusher, Mp4Record mp4Record) {

        mOntStreamPusher = ontStreamPusher;
        mMp4Record = mp4Record;

        mReserveBuffers = new HashMap<Long, Object>();
    }

    public void setPublishConfig(PublishConfig publishConfig) {

        if (mPublishConfig == null) {

            mPublishConfig = publishConfig.clone();
        } else {

            mPublishConfig.copy(publishConfig);
        }
    }

    public boolean init() {

        MediaFormat audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, mPublishConfig.getAudioSampleRate(), mPublishConfig.getAudioChannelConfig() == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1);
        //audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mPublishConfig.getAudioBitrate());
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioRecord.getMinBufferSize(mPublishConfig.getAudioSampleRate(), mPublishConfig.getAudioChannelConfig(), mPublishConfig.getAudioSampleSize()));

        try {
            mEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mEncoder.configure(audioFormat, null /* surface */, null /* crypto */, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMp4Track = mMp4Record.addTrack(audioFormat);

        } catch (IOException | IllegalStateException e) {

            e.printStackTrace();
            mEncoder = null;
            return false;
        }
        return true;
    }

    public void start() {

        mBufferInfo = new MediaCodec.BufferInfo();
        mEncoder.start();
    }

    public void stop() {

        try {
            if (mEncoder != null) {
                mEncoder.stop();
                mEncoder.release();
                mEncoder = null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        mBufferInfo = null;
        mSeqData = null;
        mReserveBuffers.clear();
    }

    public void onGetFrame(byte[] pcmFrame, int length, long startTimeUs) {

        ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();

        int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {

            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            int bufferRemaining = inputBuffer.remaining();
            if (bufferRemaining < length) {
                inputBuffer.put(pcmFrame, 0, bufferRemaining);
            } else {
                inputBuffer.put(pcmFrame, 0, length);
            }

            long timestamp = System.nanoTime() / 1000L - startTimeUs;
            mEncoder.queueInputBuffer(inputBufferIndex, 0, inputBuffer.position(), timestamp, 0);
        } else {

            OntLog.e(TAG, "audio encode get input buffer error!");
        }

        while (true) {

            int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
            if (outputBufferIndex >= 0) {

                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {

                    OntLog.e(TAG, "audio encode get output buffer error!");
                    continue;
                }
                mMp4Record.writeSampleData(mMp4Track, outputBuffer.duplicate(), mBufferInfo);

                outputBuffer.position(mBufferInfo.offset);
                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                long presentationTimeInMillis = mBufferInfo.presentationTimeUs / 1000L;
                if (presentationTimeInMillis == 0) {

                    // seq data
                    mSeqData = null;
                    mSeqData = new byte[mBufferInfo.size];
                    outputBuffer.get(mSeqData, 0, mBufferInfo.size);
                    outputBuffer.position(mBufferInfo.offset);
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                } else {

                    if (mSeqData != null) {

                        mOntStreamPusher.addAudioFrame(mSeqData, mSeqData.length, presentationTimeInMillis);
                        mSeqData = null;
                    }

                    byte[] data = getBuffer(mBufferInfo.size, mOntStreamPusher.getLastSendAudioFrameTimestamp(), presentationTimeInMillis);
                    outputBuffer.get(data, 0, mBufferInfo.size);
                    outputBuffer.position(mBufferInfo.offset);
                    mOntStreamPusher.addAudioFrame(data, mBufferInfo.size, presentationTimeInMillis);
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                outputBuffers = mEncoder.getOutputBuffers();
            } else {

                break;
            }
        }
    }

    public byte[] getBuffer(int size, long lastSentFrameTimestamp, long currentTimeStamp)
    {
        /**
         * how does it work?
         * we put byte array with their timestamp value to a hash map
         * when there is a new output buffer array, we check the last frame timestamp of mediamuxer
         * if the byte buffer timestamp is less than the value of last frame timestamp of mediamuxer
         * it means that we can use that byte buffer again because it is already written to network
         */
        Iterator<Map.Entry<Long, Object>> iterator = mReserveBuffers.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<Long, Object> next = iterator.next();
            if (next.getKey() <= lastSentFrameTimestamp)
            {
                // it means this frame is sent
                byte[] value = (byte[]) next.getValue();
                iterator.remove();
                if (value.length >= size)
                {
                    mReserveBuffers.put(currentTimeStamp, value);
                    return value;
                }
                // if byte array length is not bigger than requested size,
                // we give this array to soft hands of GC
            }
        }

        // no eligible data found, create a new byte
        byte[] data = new byte[size];
        mReserveBuffers.put(currentTimeStamp, data);
        return data;
    }
}
