package com.ont.media.odvp.def;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaRecorder;

/**
 * Created by betali on 2018/4/18.
 */

public interface IEncodeDef {

    int VIDEO_FRAME_RATE = 25;
    int VIDEO_IFRAME_INTERVAL = 2;           // 2 seconds between I-frames
    int VIDEO_MAX_CACHE_NUMBER = 48;

    int AUDIO_SAMPLE_RATE = 44100;
    int AUDIO_SAMPLE_SIZE = AudioFormat.ENCODING_PCM_16BIT;
    int AUDIO_BITRATE = 64 * 1024;
    int AUDIO_CHANNEL_COUNT = 2;
    int AUDIO_KEY_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
}
