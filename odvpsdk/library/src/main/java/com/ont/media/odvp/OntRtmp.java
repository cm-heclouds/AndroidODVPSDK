package com.ont.media.odvp;


import com.ont.media.odvp.model.RtmpMetaData;

/**
 * librtmp jni wrapper
 */
public class OntRtmp {

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("ont_odvp");
    }

    /**
     * 创建一个FLV audio header tag
     * @param format
     *    0 = Linear PCM, platform endian
     *    1 = ADPCM
     *    2 = MP3
     *    3 = Linear PCM, little endian
     *    4 = Nellymoser 16 kHz mono
     *    5 = Nellymoser 8 kHz mono
     *    6 = Nellymoser
     *    7 = G.711 A-law logarithmic PCM
     *    8 = G.711 mu-law logarithmic PCM
     *    9 = reserved
     *    10 = AAC
     *    11 = Speex
     *    14 = MP3 8 kHz
     *    15 = Device-specific sound
     *    Formats 7, 8, 14, and 15 are reserved.
     *    AAC is supported in Flash Player 9,0,115,0 and higher.
     *    Speex is supported in Flash Player 10 and higher.
     * @param sampleRate 0 = 5.5 kHz, 1 = 11 kHz, 2 = 22 kHz, 3 = 44 kHz
     * @param sampleSize 0 = 8-bit samples   1 = 16-bit samples
     * @param sampleType 0 = Mono sound     1 = Stereo sound
     * @return flv audio header tag
     */
    public static native byte nativeMakeAudioHeaderTag(int format, int sampleRate, int sampleSize, int sampleType);

    /**
     * 创建一个连接服务器的rtmp通道
     * @param url 推送目标地址
     * @param deviceId 设备ID
     * @return 0 = 失败， 其他 = rtmp指针
     */
    public static native long nativeOpenStream(String url, String deviceId);

    /**
     * 接收rtmp数据包并处理（处理完成通过接口回调）
     * @param rtmpPointer rtmp索引
     * @return 0 成功， < 0 失败
     */
    public static native int nativeCheckRcvHandler(long rtmpPointer);

    /**
     * 设置rtmp音频数据包接收回调
     * @param rtmpPointer rtmp索引
     * @param weakThis 音频回调
     * @return 0 成功， < 0 失败
     */
    public static native int nativeSetSoundNotify(long rtmpPointer, Object weakThis);

    /**
     * 发送metadata
     * @param rtmpPointer rtmp指针
     * @param metaData 媒体数据描述
     * @return 0 = 成功， < 0 失败
     */
    public static native int nativeSendMetadata(long rtmpPointer, RtmpMetaData metaData);

    /**
     * 发送H264的sps，pps 数据
     * @param rtmpPointer rtmp指针
     * @param sps_ 包含sps的byte数组
     * @param sps_offset sps的起始位置
     * @param sps_len sps的长度
     * @param pps_ 包含pps的byte数组
     * @param pps_offset pps的起始位置
     * @param pps_len pps的长度
     * @param ts 时间戳
     * @return 0 = 成功， < 0 失败
     */
    public static native int nativeSendSpspps(long rtmpPointer, byte[] sps_, int sps_offset, int sps_len, byte[] pps_, int pps_offset, int pps_len, long ts);

    /**
     * 发送视频数据
     * @param rtmpPointer rtmp指针
     * @param data 视频数据数组
     * @param offset 数据起始位置
     * @param size 数据长度
     * @param ts 时间戳
     * @param keyFrame 是否关键帧
     * @return 0 = 成功， < 0 失败
     */
    public static native int nativeSendVideoData(long rtmpPointer, byte[] data, int offset, int size, long ts, int keyFrame);

    /**
     * 发送音频数据
     * @param rtmpPointer rtmp指针
     * @param headerTag headerTag
     * @param data 音频数据数组
     * @param size 数据长度
     * @param ts 时间戳
     * @param configData 是否configData
     * @return 0 = 成功， < 0 失败
     */
    public static native int nativeSendAudioData(long rtmpPointer, byte headerTag, byte[] data, int size, long ts, boolean configData);

    /**
     * 发送编码器输出的原始音频数据
     * @param rtmpPointer rtmp指针
     * @param headerTag headerTag
     * @param data 音频数据数组
     * @param size 数据长度
     * @param ts 时间戳
     * @param configData 是否configData
     * @return 0 = 成功， < 0 失败
     */
    public static int sendAudioData(long rtmpPointer, byte headerTag, byte[] data, int size, long ts, boolean configData) {

        return nativeSendAudioData(rtmpPointer, headerTag, data, size, ts, configData);
    }

    /**
     * 发送编码器输出的原始视频数据
     * @param rtmpPointer rtmp指针
     * @param data 原始视频数据
     * @param offset 数据起始位置
     * @param length 数据长度
     * @param timestamp 时间戳
     * @return 0 = 成功， < 0 失败
     */
    public static int sendVideoData(long rtmpPointer, byte[] data, int offset, int length, long timestamp) {

        int sendLength = 0;

        NalResult nal = get_nal(data, 0, length);
        if (nal == null) {

            return -1;
        }

        if (data[nal.dataStartIndex] == 0x67) {

            NalResult nal_n = get_nal(data, nal.dataStartIndex, length);
            if (nal_n == null) {

                return -1;
            }
            sendLength += nativeSendSpspps(rtmpPointer, data, nal.dataStartIndex, nal_n.codeStartIndex - nal.dataStartIndex, data, nal_n.dataStartIndex, length - nal_n.dataStartIndex, timestamp);
        } else if ((data[nal.dataStartIndex] & 0x1f) == 0x05) {

            sendLength += nativeSendVideoData(rtmpPointer, data, nal.dataStartIndex, length - nal.dataStartIndex, timestamp, 1);
        } else if ((data[nal.dataStartIndex] & 0x1f) == 0x01) {

            sendLength += nativeSendVideoData(rtmpPointer, data, nal.dataStartIndex, length - nal.dataStartIndex, timestamp, 0);
        }

        return sendLength >= 0 ? 0 : -1;
    }

    /**
     * 关闭并释放rtmp通道
     * @param rtmpPointer rtmp指针
     */
    public static native void nativeCloseStream(long rtmpPointer);

    /**
     * rtmp通道数据写入
     * @param data 数据数组
     * @param rtmpPointer rtmp指针
     * @return <= 0 失败， 其他 = 写入的数据长度
     */
    public static native int nativeWrite(long rtmpPointer, byte[] data, int size);

    /**
     *
     * @param pause
     * @param rtmpPointer
     * @return
     */
    public static native boolean nativePause(long rtmpPointer, boolean pause);

    /**
     * 判断rtmp通道是否还在连接状态
     * @param rtmpPointer rtmp指针
     * @return true = 连接， false = 未连接
     */
    public static native boolean nativeIsConnected(long rtmpPointer);

    /**
     * 录播
     * @param fileLocation 文件地址
     * @param channelId 通道id
     * @param playFlag
     * @param pushUrl 推流地址
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeVodStreamPlay(long dev, String fileLocation, int channelId, byte proType, String playFlag, String pushUrl, int ttl);

    /**
     * 推送本地缓存流数据
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativePlayListSingleStep(long dev);

    private static NalResult find_start_code(byte[] buf, int offsetIndex, int total)
    {
        NalResult result = new NalResult();
        while (true) {
            if (offsetIndex + 3 >= total) {

                result.codeStartIndex = -1;
                result.dataStartIndex = -1;
                return result;
            }

            if (buf[offsetIndex] == 0x00 && buf[offsetIndex + 1] == 0x00 && buf[offsetIndex + 2] == 0x00 && buf[offsetIndex + 3] == 0x01) {
                result.codeStartIndex = offsetIndex;
                result.dataStartIndex = offsetIndex + 4;
                return result;
            } else if (buf[offsetIndex] == 0x00 && buf[offsetIndex + 1] == 0x00 && buf[offsetIndex + 2] == 0x01) {
                result.codeStartIndex = offsetIndex;
                result.dataStartIndex = offsetIndex + 3;
                return result;
            }
            offsetIndex++;
        }
    }

    private static NalResult get_nal(byte[] buf, int offsetIndex, int total)
    {
        NalResult nalResult = find_start_code(buf, offsetIndex, total);
        if (nalResult.dataStartIndex < 0 || nalResult.dataStartIndex >= total) {
            return null;
        }

        return nalResult;
    }

    private static class NalResult{

        int codeStartIndex;
        int dataStartIndex;
    }
}
