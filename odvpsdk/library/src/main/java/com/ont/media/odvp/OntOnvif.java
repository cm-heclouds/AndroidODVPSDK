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
    public static native int nativeAddChannel(long dev, String configBuf);

    /**
     * 直播
     * @param channel 通道id
     * @param pushUrl 推流地址
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeLiveStreamPlay(long dev, int channel, byte proType, int min, String pushUrl);

    /**
     * 直播清晰度调整
     * @param channel 通道id
     * @param level 清晰度
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeLiveStreamCtrl(long dev, int channel, int level);

    /**
     * 摄像头控制
     * @param channel 通道id
     * @param mode 模式
     * @param cmd 命令
     * @param speed 速度
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDevPtzCtrl(long dev, int channel, int mode, int cmd, int speed);
}
