package com.ont.media.odvp.codec;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.ont.media.odvp.model.PublishConfig;
import com.ont.media.odvp.record.Mp4Record;
import com.ont.media.odvp.stream.OntStreamPusher;
import com.ont.media.odvp.utils.OntLog;
import com.ont.media.odvp.OntFormat;
import com.ont.media.odvp.def.IEncodeDef;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

//import com.tencent.mars.xlog.Log;

/**
 * Created by Benny.luo on 2018/1/13 0013.
 */

public class VideoEncode {

    private static final String TAG = VideoEncode.class.getSimpleName();
    public static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding

    // config
    private byte[] mSpspps;
    private PublishConfig mPublishConfig;

    // encode
    private MediaCodec mEncoder;
    private MediaCodecInfo mEncoderInfo;
    private MediaCodec.BufferInfo mBufferInfo;
    private OntStreamPusher mOntStreamPusher;
    private Map<Long, Object> mReserveBuffers;

    // record
    private int mMp4Track;
    private Mp4Record mMp4Record;

    public VideoEncode(OntStreamPusher ontStreamPusher, Mp4Record mp4Record) {

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

    public int chooseColorFormat(String name) {
        // choose the encoder "video/avc":
        //      1. select default one when type matched.
        //      2. google avc is unusable.
        //      3. choose qcom avc.
        mEncoderInfo = chooseVideoEncoder(name);
        //vmci = chooseVideoEncoder("google");
        //vmci = chooseVideoEncoder("qcom");

        int matchedColorFormat = 0;
        MediaCodecInfo.CodecCapabilities cc = mEncoderInfo.getCapabilitiesForType(MIME_TYPE);
        for (int i = 0; i < cc.colorFormats.length; i++) {
            int cf = cc.colorFormats[i];
            Log.i(TAG, String.format("vencoder %s supports color fomart 0x%x(%d)", mEncoderInfo.getName(), cf, cf));

            // choose YUV for h.264, prefer the bigger one.
            // corresponding to the color space transform in onPreviewFrame
            if (cf >= cc.COLOR_FormatYUV420Planar && cf <= cc.COLOR_FormatYUV420SemiPlanar) {
                if (cf > matchedColorFormat) {
                    matchedColorFormat = cf;
                }
            }
        }

        for (int i = 0; i < cc.profileLevels.length; i++) {
            MediaCodecInfo.CodecProfileLevel pl = cc.profileLevels[i];
            Log.i(TAG, String.format("vencoder %s support profile %d, level %d", mEncoderInfo.getName(), pl.profile, pl.level));
        }

        Log.i(TAG, String.format("vencoder %s choose color format 0x%x(%d)", mEncoderInfo.getName(), matchedColorFormat, matchedColorFormat));
        return matchedColorFormat;
    }

    public boolean init() {

        try {

            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (Exception ie) {

            return false;
        }

        OntFormat.setEncoderResolution(mPublishConfig.getWidth(), mPublishConfig.getHeight());
        try {

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mPublishConfig.getWidth(), mPublishConfig.getHeight());
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mPublishConfig.getVideoColorFormat());
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mPublishConfig.getVideoBitrate());
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mPublishConfig.getVideoFrameRate());
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mPublishConfig.getVideoFrameInterval());
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMp4Track = mMp4Record.addTrack(format);
        } catch (MediaCodec.CodecException e) {

            try {

                MediaFormat format1 = MediaFormat.createVideoFormat(MIME_TYPE, mPublishConfig.getWidth(), mPublishConfig.getHeight());
                format1.setInteger(MediaFormat.KEY_COLOR_FORMAT, mPublishConfig.getVideoColorFormat());
                format1.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
                format1.setInteger(MediaFormat.KEY_BIT_RATE, mPublishConfig.getVideoBitrate());
                format1.setInteger(MediaFormat.KEY_FRAME_RATE, mPublishConfig.getVideoFrameRate());
                format1.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mPublishConfig.getVideoFrameInterval());
                mEncoder.configure(format1, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mMp4Track = mMp4Record.addTrack(format1);
            } catch (Exception e1) {

                return false;
            }
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
        } catch (Exception e) {

            e.printStackTrace();
        }

        mBufferInfo = null;
        mSpspps = null;
        mReserveBuffers.clear();
    }

    public void onGetRGBAFrame(byte[] rgbaFrame, int width, int height, long startTimeUs) {

        byte[] yuvFrame;
        if (mPublishConfig.getVideoColorFormat() == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {

            yuvFrame = OntFormat.RGBAToI420(rgbaFrame, width, height, true, 180);
        } else if (mPublishConfig.getVideoColorFormat() == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {

            yuvFrame = OntFormat.RGBAToNV12(rgbaFrame, width, height, true, 180);
        } else {

            return;
        }

        onGetFrame(yuvFrame, yuvFrame.length, startTimeUs);
    }

    public void onGetI420Frame(byte[] yuvFrame, int length, long startTimeUs) {

        if (mPublishConfig.getVideoColorFormat() != MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {

            return;
        }

        onGetFrame(yuvFrame, length, startTimeUs);
    }

    public void onGetNV12Frame(byte[] yuvFrame, int length, long startTimeUs) {

        if (mPublishConfig.getVideoColorFormat() != MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {

            return;
        }

        onGetFrame(yuvFrame, length, startTimeUs);
    }

    public void onGetNV21Frame(byte[] nv21Frame, boolean flip, int rotation, int width, int height, long startTimeUs) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

        String date;
        if(mPublishConfig.isWaterMark()){
            date = format.format(new Date());
        }else{
            date = null;
        }

        byte[] yuvFrame;
        if (mPublishConfig.getVideoColorFormat() == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {

            yuvFrame = OntFormat.NV21ToI420(nv21Frame, date, width, height, flip, rotation);
        } else if (mPublishConfig.getVideoColorFormat() == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {

            yuvFrame = OntFormat.NV21ToNV12(nv21Frame, date, width, height, flip, rotation);
        } else {

            return;
        }

        onGetFrame(yuvFrame, yuvFrame.length, startTimeUs);
    }

    private void onGetFrame(byte[] yuvFrame, int length, long startTimeUs) {

        ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();

        int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {

            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(yuvFrame, 0, length);
            long timeStamp = System.nanoTime() / 1000L - startTimeUs;
            mEncoder.queueInputBuffer(inputBufferIndex, 0, length, timeStamp, 0);
        } else {

            OntLog.e(TAG, "video encode get input buffer error!");
        }

        while(true) {

            int outputBufferIndex;
            try {

                outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
            } catch (IllegalStateException e) {

                outputBufferIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
                OntLog.e(TAG, "video encode get output buffer error!");
            }

            if (outputBufferIndex >= 0) {

                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                mMp4Record.writeSampleData(mMp4Track, outputBuffer.duplicate(), mBufferInfo);

                outputBuffer.position(mBufferInfo.offset);
                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                long presentationTimeInMillis = mBufferInfo.presentationTimeUs / 1000L;
                if (presentationTimeInMillis == 0) {

                    // spspps
                    mSpspps = null;
                    mSpspps = new byte[mBufferInfo.size];
                    outputBuffer.get(mSpspps, 0, mBufferInfo.size);
                    outputBuffer.position(mBufferInfo.offset);
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);

                } else {

                    if (mSpspps != null) {

                        mOntStreamPusher.addVideoFrame(mSpspps, mSpspps.length, presentationTimeInMillis);
                        mSpspps = null;
                    }

                    byte[] data = getBuffer(mBufferInfo.size, mOntStreamPusher.getLastSendVideoFrameTimestamp(), presentationTimeInMillis);
                    outputBuffer.get(data, 0, mBufferInfo.size);
                    outputBuffer.position(mBufferInfo.offset);
                    mOntStreamPusher.addVideoFrame(data, mBufferInfo.size, presentationTimeInMillis);
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                outputBuffers = mEncoder.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                MediaFormat newFormat = mEncoder.getOutputFormat();
                ByteBuffer sps = newFormat.getByteBuffer("csd-0");
                ByteBuffer pps = newFormat.getByteBuffer("csd-1");
                mSpspps = null;
                mSpspps = new byte[sps.limit() + pps.limit()];
                sps.get(mSpspps, 0, sps.limit());
                pps.get(mSpspps, sps.limit(), pps.limit());
            } else {

                break;
            }
        }
    }

    private byte[] getBuffer(int size, long lastSentFrameTimestamp, long currentTimeStamp)
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
                // we let this array to soft hands of GC
            }
        }

        // no eligible data found, create a new byte
        byte[] data = new byte[size];
        mReserveBuffers.put(currentTimeStamp, data);
        return data;
    }

    private MediaCodecInfo chooseVideoEncoder(String name) {
        int nbCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < nbCodecs; i++) {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (!mci.isEncoder()) {
                continue;
            }

            String[] types = mci.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(MIME_TYPE)) {
                    Log.i(TAG, String.format("vencoder %s types: %s", mci.getName(), types[j]));
                    if (name == null) {
                        return mci;
                    }

                    if (mci.getName().contains(name)) {
                        return mci;
                    }
                }
            }
        }

        return null;
    }
}
