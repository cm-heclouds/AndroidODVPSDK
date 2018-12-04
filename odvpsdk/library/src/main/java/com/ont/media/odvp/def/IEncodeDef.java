package com.ont.media.odvp.def;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;

/**
 * Created by betali on 2018/4/18.
 */

public interface IEncodeDef {

    int VIDEO_FRAME_RATE = 24;
    int VIDEO_IFRAME_INTERVAL = 2;           // 2 seconds between I-frames
    int VIDEO_BITRATE = 1024 * 1024;

    int AUDIO_SAMPLE_RATE = 44100;
    int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    int AUDIO_BITRATE = 64 * 1024;
    int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    int AUDIO_CHANNEL_COUNT = 2;
    int AUDIO_KEY_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
}
