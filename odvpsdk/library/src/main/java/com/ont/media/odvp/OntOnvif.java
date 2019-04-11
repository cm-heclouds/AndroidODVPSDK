package com.ont.media.odvp;

/**
 * Created by betali on 2018/2/27.
 */

public class OntOnvif {
    static {
        System.loadLibrary("yuv");
        System.loadLibrary("ont_odvp");
    }

    /**
     * 添加onvif摄像头通道
     * @param configBuf onvif摄像头配置文件路径
     * @return 0 =  成功， < 0 失败
     */
    public static int addChannel(long dev, String configBuf) {

        if (BuildConfig.onvifType == 1) {
            return nativeAddChannel(dev, configBuf);
        } else {
            return 0;
        }
    }

    /**
     * 直播
     * @param channel 通道id
     * @param pushUrl 推流地址
     * @return 0 =  成功， < 0 失败
     */
    public static int liveStreamPlay(long dev, int channel, byte proType, int min, String pushUrl) {

        if (BuildConfig.onvifType == 1) {
            return nativeLiveStreamPlay(dev, channel, proType, min, pushUrl);
        } else {
            return 0;
        }
    }

    /**
     * 直播清晰度调整
     * @param channel 通道id
     * @param level 清晰度
     * @return 0 =  成功， < 0 失败
     */
    public static int liveStreamCtrl(long dev, int channel, int level) {

        if (BuildConfig.onvifType == 1) {
            return nativeLiveStreamCtrl(dev, channel, level);
        } else {
            return 0;
        }
    }

    /**
     * 摄像头控制
     * @param channel 通道id
     * @param mode 模式
     * @param cmd 命令
     * @param speed 速度
     * @return 0 =  成功， < 0 失败
     */
    public static int devPtzCtrl(long dev, int channel, int mode, int cmd, int speed) {

        if (BuildConfig.onvifType == 1) {
            return nativeDevPtzCtrl(dev, channel, mode, cmd, speed);
        } else {
            return 0;
        }
    }

    private static native int nativeAddChannel(long dev, String configBuf);
    private static native int nativeLiveStreamPlay(long dev, int channel, byte proType, int min, String pushUrl);
    private static native int nativeLiveStreamCtrl(long dev, int channel, int level);
    private static native int nativeDevPtzCtrl(long dev, int channel, int mode, int cmd, int speed);
}
