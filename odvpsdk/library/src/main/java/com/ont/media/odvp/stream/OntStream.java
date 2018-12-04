package com.ont.media.odvp.stream;

import com.ont.media.odvp.IOntSoundCb;
import com.ont.media.odvp.OntRtmp;
import com.ont.media.odvp.model.Resolution;
import com.ont.media.odvp.model.RtmpMetaData;
import com.ont.media.odvp.player.AudioPlayer;
import com.ont.media.odvp.utils.OntLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by betali on 2018/1/15.
 */

public class OntStream {

    private static final String TAG = OntStream.class.getSimpleName();

    private AudioPlayer mAudioPlayer;
    private long mLastSendFrameTimestamp;
    private long mLastSendVideoFrameTimestamp;
    private long mLastSendAudioFrameTimestamp;

    private boolean mAudio = true;
    private int mWidth;
    private int mHeight;
    private int mSampleRate = 3;
    private int mFormat = 1;
    private int mChannelConfig = 1;

    private long mRtmpPointer;
    private boolean mConfigOk;
    private volatile boolean mIsConnect;
    private byte mAudioHeaderTag;
    private ArrayList<OntStreamPusher.MediaFrame> mAudioFrameList;
    private ArrayList<OntStreamPusher.MediaFrame> mVideoFrameList;

    public OntStream(AudioPlayer audioPlayer) {

        mAudioFrameList = new ArrayList<>();
        mVideoFrameList = new ArrayList<>();
        mAudioPlayer = audioPlayer;
        mLastSendFrameTimestamp = -1;
    }

    public void setAudio(boolean audio) {

        this.mAudio = audio;
    }

    public void setWidth(int width) {

        this.mWidth = width;
    }

    public void setHeight(int height) {

        this.mHeight = height;
    }

    public void setSampleRate(int sampleRate) {

        this.mSampleRate = sampleRate;
    }

    public void setFormat(int format) {

        this.mFormat = format;
    }

    public void setChannelConfig(int channelConfig) {

        this.mChannelConfig = channelConfig;
    }

    public long getRtmpPointer() {
        return mRtmpPointer;
    }

    public boolean connect(String pushUrl, String deviceId) {

        mConfigOk = false;
        mRtmpPointer = OntRtmp.nativeOpenStream(pushUrl, deviceId);
        if (mRtmpPointer == 0) {

            return false;
        }
        RtmpMetaData metaData = new RtmpMetaData();
        metaData.setDuration(0.0);
        metaData.setWidth(mWidth);
        metaData.setHeight(mHeight);
        metaData.setVideoCodecid(7);
        metaData.setAudioCodecid(10);
        metaData.setHasAudio(true);
        if (OntRtmp.nativeSendMetadata(mRtmpPointer, metaData) != 0) {

            OntRtmp.nativeCloseStream(mRtmpPointer);
            mRtmpPointer = 0;
            return false;
        }

        OntRtmp.nativeSetSoundNotify(mRtmpPointer, new WeakReference<IOntSoundCb>(mSoundCallback));

        /*
         *  format = 10 AAC
         *  sampleRate = 3 44KH
         *  sampleSize = 1 16bit
         *  sampleType = 0 mono
         */
        mAudioHeaderTag = OntRtmp.nativeMakeAudioHeaderTag(10, mSampleRate, mFormat, mChannelConfig);
        mIsConnect = true;

        return true;
    }

    public void release(boolean sendLastFrames) {

        if (sendLastFrames) {
            sendLeftFrames();
        }
        disconnect();
    }

    public boolean sendMetadata(int width, int height) {

        RtmpMetaData metaData = new RtmpMetaData();
        metaData.setDuration(0.0);
        metaData.setWidth(width);
        metaData.setHeight(height);
        metaData.setVideoCodecid(7);
        metaData.setAudioCodecid(10);
        metaData.setHasAudio(true);
        if (OntRtmp.nativeSendMetadata(mRtmpPointer, metaData) != 0) {

            return false;
        }
        return true;
    }

    private void disconnect() {

        mIsConnect = false;
        mLastSendFrameTimestamp = -1;
        mLastSendVideoFrameTimestamp = 0;
        mLastSendAudioFrameTimestamp = 0;

        mAudioFrameList.clear();
        mVideoFrameList.clear();
        OntRtmp.nativeCloseStream(mRtmpPointer);
        mRtmpPointer = 0;
    }

    public long getLastSendVideoFrameTimestamp() {
        return mLastSendVideoFrameTimestamp;
    }

    public long getLastSendAudioFrameTimestamp() {
        return mLastSendAudioFrameTimestamp;
    }

    public boolean onMediaMuxerMsg(OntStreamMsg msg) {

        if (msg == null || !mIsConnect) {

            OntLog.e(TAG, "frame msg err! msg = " + (msg == null) + ", connect = " + mIsConnect + "");
            return true;
        }

        boolean sendSuccess = true;
        switch (msg.what) {

            case OntStreamMsg.ON_SEND_METADATA:

                mLastSendFrameTimestamp = -1;
                mLastSendVideoFrameTimestamp = 0;
                mLastSendAudioFrameTimestamp = 0;
                mVideoFrameList.clear();
                mAudioFrameList.clear();
                Resolution resolution = (Resolution) msg.obj;
                if (!sendMetadata(resolution.width, resolution.height)) {

                    sendSuccess = false;
                }
                break;
            case OntStreamMsg.ON_SEND_VIDEO:

                mVideoFrameList.add((OntStreamPusher.MediaFrame) msg.obj);
                if (!sendFrames()) {

                    sendSuccess = false;
                }
                break;
            case OntStreamMsg.ON_SEND_AUDIO:

                mAudioFrameList.add((OntStreamPusher.MediaFrame) msg.obj);
                if (!sendFrames()) {

                    sendSuccess = false;
                }
                break;
            default:
                break;
        }

        return sendSuccess;
    }


    private boolean sendLeftFrames()
    {
        int videoFrameListSize = 0;
        int audioFrameListSize = 0;

        do {

            if (!sendFrames()) {

                return false;
            }

            videoFrameListSize = mVideoFrameList.size();
            audioFrameListSize = mAudioFrameList.size();
        } while ((videoFrameListSize > 0) && (audioFrameListSize > 0));

        if (videoFrameListSize > 0) {

           if (!sendVideoFrames(mVideoFrameList.get(videoFrameListSize - 1).timestamp)) {

               return false;
           }
        }
        else if (audioFrameListSize > 0) {

            if (!sendAudioFrames(mAudioFrameList.get(audioFrameListSize - 1).timestamp)) {

                return false;
            }
        }

        return true;
    }

    private boolean sendFrames() {
        // this is a simple sorting algorithm.
        // we do not know the audio or video frames timestamp in advance and they are not
        // deterministic. So we send video frames with the timestamp is less than the first one in the list
        // and the same algorithm applies for audio frames.
        if (!mAudio) {

            int videoFrameListSize = mVideoFrameList.size();
            if (!sendVideoFrames(mVideoFrameList.get(videoFrameListSize - 1).timestamp)) {

                return false;
            }
        } else {

            int listSize = mVideoFrameList.size();
            if (listSize > 0) {
                if (!sendAudioFrames(mVideoFrameList.get(0).timestamp)) {

                    return false;
                }
            }

            listSize = mAudioFrameList.size();
            if (listSize > 0) {
                if (!sendVideoFrames(mAudioFrameList.get(0).timestamp)) {

                    return false;
                }
            }
        }
        return true;
    }

    private boolean sendAudioFrames(long firstLeftVideoTimestamp) {

        Iterator<OntStreamPusher.MediaFrame> iterator = mAudioFrameList.iterator();
        while (iterator.hasNext())
        {
            OntStreamPusher.MediaFrame audioFrame = iterator.next();
            if (audioFrame.timestamp > firstLeftVideoTimestamp)
            {
                break;
            }

            // frame time stamp should be equal or less than the previous timestamp
            // in some cases timestamp of audio and video frames may be equal
            if (audioFrame.timestamp < mLastSendFrameTimestamp) {

                iterator.remove();
                continue;
            }

            if (audioFrame.timestamp == mLastSendFrameTimestamp) {

                audioFrame.timestamp++;
            }

            if (OntRtmp.sendAudioData(mRtmpPointer, mAudioHeaderTag, audioFrame.data, audioFrame.length, audioFrame.timestamp, !mConfigOk) < 0) {

                OntLog.e(TAG, "send audio error!");
                return false;
            }

            mConfigOk = true;
            mLastSendFrameTimestamp = audioFrame.timestamp;
            mLastSendAudioFrameTimestamp = audioFrame.timestamp;
            iterator.remove();
        }

        return true;
    }

    private boolean sendVideoFrames(long firstLeftAudioTimestamp) {

        Iterator<OntStreamPusher.MediaFrame> iterator = mVideoFrameList.iterator();
        while (iterator.hasNext()) {

            OntStreamPusher.MediaFrame videoFrame = iterator.next();
            if ((videoFrame.timestamp > firstLeftAudioTimestamp))
            {
                break;
            }
            // frame time stamp should be equal or less than timestamp
            // in some cases timestamp of audio and video frames may be equal
            if (videoFrame.timestamp < mLastSendFrameTimestamp) {

                iterator.remove();
                continue;
            }

            if (videoFrame.timestamp == mLastSendFrameTimestamp) {

                videoFrame.timestamp++;
            }

            if (OntRtmp.sendVideoData(mRtmpPointer, videoFrame.data, 0, videoFrame.length, videoFrame.timestamp) < 0) {

                OntLog.e(TAG, "send video error!");
                return false;
            }

            mLastSendFrameTimestamp = videoFrame.timestamp;
            mLastSendVideoFrameTimestamp = videoFrame.timestamp;
            iterator.remove();
        }

        return true;
    }

    private IOntSoundCb mSoundCallback = new IOntSoundCb() {
        @Override
        public void onSoundNotify(int chunk, int ts, byte[] data, int len) {

            mAudioPlayer.onSoundNotify(chunk, ts, data, len);
        }
    };
}
