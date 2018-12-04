package com.ont.media.odvp.stream;

import android.os.Handler;

import com.ont.media.odvp.OntRtmp;
import com.ont.media.odvp.utils.OntLog;
import com.ont.media.odvp.def.IStreamDef;
import com.ont.media.odvp.model.Resolution;
import com.ont.media.odvp.player.AudioPlayer;

import java.util.concurrent.ConcurrentLinkedQueue;

//import com.tencent.mars.xlog.Log;

/**
 * Created by betali on 2018/1/15.
 */

public class OntStreamPusher {

    private static final String TAG = OntStreamPusher.class.getSimpleName();

    private volatile boolean mRunning;
    private volatile boolean mDisconnectNotify;
    private Thread mPushThread;
    private Object mPushThreadNotifyLock;
    private ConcurrentLinkedQueue<OntStreamMsg> mMediaMuxerMsgCache;

    private OntStream mOntStream;
    private long mLastVideoTimestamp;
    private long mLastAudioTimestamp;
    private Handler mMainThreadHandler;

    public OntStreamPusher(Handler handler, AudioPlayer audioPlayer) {

        mPushThreadNotifyLock = new Object();
        mMediaMuxerMsgCache = new ConcurrentLinkedQueue<>();
        mOntStream = new OntStream(audioPlayer);

        mLastVideoTimestamp = -1;
        mLastAudioTimestamp = -1;

        mMainThreadHandler = handler;
    }

    public void setAudio(boolean audio) {

        mOntStream.setAudio(audio);
    }

    public void setWidth(int width) {

        mOntStream.setWidth(width);
    }

    public void setHeight(int height) {

        mOntStream.setHeight(height);
    }

    public void setSampleRate(int sampleRate) {

        mOntStream.setSampleRate(sampleRate);
    }

    public void setFormat(int format) {

        mOntStream.setFormat(format);
    }

    public void setChannelConfig(int channelConfig) {

        mOntStream.setChannelConfig(channelConfig);
    }

    public boolean start(final String pushUrl, final String deviceId) {

        mRunning = true;
        mPushThread = new Thread() {

            @Override
            public void run() {

                // 创建连接和本地流媒体文件
                if (!mOntStream.connect(pushUrl, deviceId)) {

                    // 创建连接失败
                    if (!mDisconnectNotify) {

                        OntLog.e(TAG, "network connect error!");
                        mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(IStreamDef.ON_PUBLISH_STREAM_DISCONNECT));
                        mDisconnectNotify = true;
                    }
                    return;
                }

                // 进入消息循环
                while (mRunning && mPushThread != null && !mPushThread.isInterrupted()) {

                    while (!mMediaMuxerMsgCache.isEmpty()) {

                        // 数据发送
                        if (!mOntStream.onMediaMuxerMsg(mMediaMuxerMsgCache.poll())) {

                            // 数据发送失败
                            if (!mDisconnectNotify) {

                                OntLog.e(TAG, "network send error!");
                                mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(IStreamDef.ON_PUBLISH_STREAM_DISCONNECT));
                                mDisconnectNotify = true;
                            }
                        }  else {
                            OntRtmp.nativeCheckRcvHandler(mOntStream.getRtmpPointer());
                        }
                    }

                    synchronized (mPushThreadNotifyLock) {

                        try {

                            mPushThreadNotifyLock.wait(500);
                        } catch (InterruptedException ie) {

                            OntLog.e(TAG, "network thread wait error!");
                            mPushThread.interrupt();
                        }
                    }
                }
                OntLog.e(TAG, "network quit! running = " + mRunning + ", mPushThread = "+ (mPushThread == null) +"");
            }
        };
        mPushThread.start();
        return true;
    }

    public void stop() {

        mRunning = false;
        mMediaMuxerMsgCache.clear();

        if (mPushThread != null) {

            mPushThread.interrupt();
            try {

                mPushThread.join();
            } catch (InterruptedException e) {

                e.printStackTrace();
                mPushThread.interrupt();
            }
            mPushThread = null;
        }

        mDisconnectNotify = false;
        mLastVideoTimestamp = -1;
        mLastAudioTimestamp = -1;
        mOntStream.release(false);
    }

    public void sendMetadata(int width, int height) {

        if (!mRunning) {

            return;
        }

        mLastVideoTimestamp = -1;
        mLastAudioTimestamp = -1;
        mMediaMuxerMsgCache.clear();
        addMediaMuxerMsg(new OntStreamMsg(OntStreamMsg.ON_SEND_METADATA, new Resolution(width, height)));
    }

    public void addVideoFrame(byte[] data, int length, long presentationTime) {

        if (!mRunning) {

            OntLog.i(TAG, "video add frame no running!");
            return;
        }
        if (presentationTime < mLastVideoTimestamp || length <= 0) {

            OntLog.i(TAG, "video add frame error! current = " + presentationTime  + ", last = " + mLastVideoTimestamp  + ", length = " + length);
            return;
        }

        mLastVideoTimestamp = presentationTime;
        addMediaMuxerMsg(new OntStreamMsg(OntStreamMsg.ON_SEND_VIDEO, new MediaFrame(data, length, presentationTime)));
    }

    public void addAudioFrame(byte[] data, int length, long presentationTime) {

        if (!mRunning) {

            OntLog.i(TAG, "audio add frame no running!");
            return;
        }
        if (presentationTime < mLastAudioTimestamp || length <= 0) {

            OntLog.i(TAG, "audio add frame error! current = " + presentationTime + ", last = " + mLastAudioTimestamp + ", length = " + length);
            return;
        }

        mLastAudioTimestamp = presentationTime;
        addMediaMuxerMsg(new OntStreamMsg(OntStreamMsg.ON_SEND_AUDIO, new MediaFrame(data, length, presentationTime)));
    }

    public long getLastSendVideoFrameTimestamp() {

        return mOntStream.getLastSendVideoFrameTimestamp();
    }

    public long getLastSendAudioFrameTimestamp() {

        return mOntStream.getLastSendAudioFrameTimestamp();
    }

    private void addMediaMuxerMsg(OntStreamMsg msg) {

        mMediaMuxerMsgCache.add(msg);
        synchronized (mPushThreadNotifyLock) {

            mPushThreadNotifyLock.notifyAll();
        }
    }

    public static class MediaFrame {
        byte[] data;
        long timestamp;
        int length;

        public MediaFrame(byte[] data, int length, long timestamp) {
            this.data = data;
            this.length = length;
            this.timestamp = timestamp;
        }
    }
}
