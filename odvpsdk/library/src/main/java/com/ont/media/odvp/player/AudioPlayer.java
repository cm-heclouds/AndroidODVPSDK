package com.ont.media.odvp.player;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Handler;

import com.ont.media.odvp.utils.OntLog;
import com.ont.media.odvp.codec.AudioDecode;
import com.ont.media.odvp.def.IEncodeDef;
import com.ont.media.odvp.def.IStreamDef;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by betali on 2018/6/8.
 */

public class AudioPlayer {

    private final String TAG = "AudioPlayer";
    private ConcurrentLinkedQueue<AudioMsg> mAudioMsgCache;
    private Thread mPlayerThread;
    private Object mPlayerThreadNotifyLock;

    private Context mContext;
    private volatile byte mPlayerStatus;
    private AudioDecode mDecoder;
    private AudioPlayerTrack mPlayerTrack;
    private volatile boolean isPrepare;
    private Handler mMainThreadHandler;
    private AudioManager mAudioManager;
    private byte mNoSoundMsgRound;
    private volatile boolean mSoundNotifyStop;
    private volatile boolean mSoundNotifyStart;
    private AudioFocusHelper mAudioFocusHelper;
    private boolean mPublishRunning;
    private int mCurChunk;

    public AudioPlayer(Context context, Handler mainThreadHandler) {

        mPlayerTrack = new AudioPlayerTrack();
        mDecoder = new AudioDecode(this);
        mAudioMsgCache = new ConcurrentLinkedQueue<>();
        mPlayerThreadNotifyLock = new Object();
        mMainThreadHandler = mainThreadHandler;
        mContext = context;
        mPlayerStatus = STATUS_STOP;
        mAudioFocusHelper = new AudioFocusHelper();
        mAudioManager = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    public void setPublishRunning(boolean publishRunning) {

        this.mPublishRunning = publishRunning;
    }

    public void onResume() {

        if (isPause()) {

            start();
        }
    }

    public void start() {

        if (isRunning()) {

            return;
        }

        mPlayerStatus = STATUS_RUNNING;
        mAudioFocusHelper.requestFocus();
        mPlayerThread = new Thread() {

            @Override
            public void run() {

                while (isRunning() && mPlayerThread != null && !Thread.interrupted()) {

                    while (!mAudioMsgCache.isEmpty()) {

                        mNoSoundMsgRound = 0;
                        AudioMsg msg = mAudioMsgCache.poll();
                        if (!isPrepare) {

                            prepare(msg.data);
                            isPrepare = true;
                        }
                        mDecoder.decode(msg.data, msg.offset, msg.length, msg.ts);
                    }

                    if (!mSoundNotifyStop) {

                        mNoSoundMsgRound++;
                        if (mNoSoundMsgRound >= 30) {

                            mNoSoundMsgRound = 0;
                            mSoundNotifyStop = true;
                            mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(IStreamDef.ON_SOUND_MSG_STOP));
                        }
                    }

                    synchronized (mPlayerThreadNotifyLock) {

                        try {

                            mPlayerThreadNotifyLock.wait(500);
                        } catch (InterruptedException ie) {

                            OntLog.e(TAG, "network thread wait error!");
                            mPlayerThread.interrupt();
                        }
                    }
                }
            }
        };
        mPlayerThread.start();
        mSoundNotifyStart = false;
    }

    public boolean prepare(byte[] ADTSHeader) {

        ADTSResult ret = decodeADTSHeader(ADTSHeader);

        mPlayerTrack.setSampBit(IEncodeDef.AUDIO_FORMAT);
        mPlayerTrack.setChannel(AudioFormat.CHANNEL_OUT_STEREO);
        mPlayerTrack.setFrequency(ret.sampleRate);
        mPlayerTrack.init();

        mDecoder.setSampleRate(ret.sampleRate);
        mDecoder.setChannelCount(ret.channelCount);
        mDecoder.setKeyProfile(ret.keyProfile);
        if (!mDecoder.init(new byte[]{ret.config0, ret.config1})) {

            OntLog.e(TAG, "create mediaDecode failed");
            return false;
        }

        return true;
    }

    public void stop() {

        mPlayerStatus = STATUS_STOP;
        mAudioMsgCache.clear();

        if (mPlayerThread != null) {

            mPlayerThread.interrupt();
            try {

                mPlayerThread.join();
            } catch (InterruptedException e) {

                OntLog.e(TAG, "interrupt error!");
                mPlayerThread.interrupt();
            }
            mPlayerThread = null;
        }

        mPlayerTrack.release();
        mDecoder.release();
        isPrepare = false;
        mSoundNotifyStop = false;
    }

    public void pause(boolean loseFocus) {

        mPlayerStatus = STATUS_PAUSE;
        mAudioMsgCache.clear();

        if (loseFocus) {

            mAudioFocusHelper.abandonFocus();
        }

        if (mPlayerThread != null) {

            mPlayerThread.interrupt();
            try {

                mPlayerThread.join();
            } catch (InterruptedException e) {

                OntLog.e(TAG, "interrupt error!");
                mPlayerThread.interrupt();
            }
            mPlayerThread = null;
        }

        mPlayerTrack.release();
        mDecoder.release();
        isPrepare = false;
    }

    public void playAudioTrack(byte[] data, int offset, int length) {

        mPlayerTrack.playAudioTrack(data, offset, length);
    }

    public void onSoundNotify(int chunk, int ts, byte[] data, int len) {

        OntLog.e(TAG, "receive msg");
        if (isStop() && mPublishRunning) {

            if (!mSoundNotifyStart) {

                mSoundNotifyStart = true;
                mCurChunk = chunk;
                mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(IStreamDef.ON_SOUND_MSG_START));
                OntLog.e(TAG, "audio start");
            }

            return;
        }

        if (chunk != mCurChunk) {

            return;
        }

        mAudioMsgCache.add(new AudioPlayer.AudioMsg(data, 0, len, ts));
        synchronized (mPlayerThreadNotifyLock) {

            mPlayerThreadNotifyLock.notifyAll();
        }
    }

    public static class AudioMsg {

        byte[] data;
        int offset;
        int length;
        int ts;

        public AudioMsg(byte[] data, int offset, int length, int ts) {

            this.data = data;
            this.offset = offset;
            this.length = length;
            this.ts = ts;
        }
    }

    private ADTSResult decodeADTSHeader(byte[] ADTSHeader) {

        if (ADTSHeader == null || ADTSHeader.length < 7) {

            return null;
        }

        int mpeg4SampleRates[] = {

                96000, 88200, 64000, 48000, 44100, 32000,
                24000, 22050, 16000, 12000, 11025, 8000, 7350
        };

        // 前两个字节可以不解析先
        ADTSResult ret = new ADTSResult();
        ret.keyProfile =  (ADTSHeader[2] >> 6 & 0x03) + 1;
        ret.sampleRate = mpeg4SampleRates[(ADTSHeader[2] >> 2) & 0x0F];
        ret.channelCount = ((ADTSHeader[2] & 0x03) << 2) + ((ADTSHeader[3] >> 6) & 0x03);
        ret.config0 = (byte)((ret.keyProfile << 3) | ((ret.sampleRate & 0xe) >> 1));
        ret.config1 = (byte)(((ret.sampleRate & 0x1) << 7)|(ret.channelCount << 3));
        return ret;
    }

    private static class ADTSResult {

        int keyProfile;
        int sampleRate;
        int channelCount;
        byte config0;
        byte config1;
    }

    /**
     * 音频焦点改变监听
     */
    private class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
        boolean startRequested = false;
        boolean pausedForLoss = false;
        int currentFocus = 0;

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (currentFocus == focusChange) {
                return;
            }

            currentFocus = focusChange;
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    if (startRequested || pausedForLoss) {
                        start();
                        startRequested = false;
                        pausedForLoss = false;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (isRunning()) {
                        pausedForLoss = true;
                        pause(true);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (isRunning()) {
                        pausedForLoss = true;
                        pause(false);
                    }
                    break;
            }
        }

        /**
         * Requests to obtain the audio focus
         *
         * @return True if the focus was granted
         */
        boolean requestFocus() {
            if (currentFocus == AudioManager.AUDIOFOCUS_GAIN) {
                return true;
            }

            if (mAudioManager == null) {
                return false;
            }

            int status = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
                currentFocus = AudioManager.AUDIOFOCUS_GAIN;
                return true;
            }

            startRequested = true;
            return false;
        }

        /**
         * Requests the system to drop the audio focus
         *
         * @return True if the focus was lost
         */
        boolean abandonFocus() {

            if (mAudioManager == null) {
                return false;
            }

            startRequested = false;
            int status = mAudioManager.abandonAudioFocus(this);
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status;
        }
    }

    private final byte STATUS_STOP = 0x01;
    private final byte STATUS_RUNNING = 0x02;
    private final byte STATUS_PAUSE = 0x03;

    private boolean isRunning() {

        return mPlayerStatus == STATUS_RUNNING;
    }

    private boolean isStop() {

        return mPlayerStatus == STATUS_STOP;
    }

    private boolean isPause() {

        return mPlayerStatus == STATUS_PAUSE;
    }
}
