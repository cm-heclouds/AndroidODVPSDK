package com.ont.media.odvp;

import com.ont.media.odvp.model.PlatRespDevPS;

import java.lang.ref.WeakReference;

public class OntOdvp {

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("ont_odvp");
    }

    /**
     * 获取设备ID
     * @param dev 设备索引
     * @return 设备ID，== NULL 失败
     */
    public static native String nativeGetDeviceId(long dev);

    /**
     * 获取设备鉴权码
     * @param dev 设备索引
     * @return 设备鉴权码，== NULL 失败
     */
    public static native byte[] nativeGetAuthCode(long dev);

    /**
     * 设置设备ID
     * @param dev 设备索引
     * @param deviceId 设备ID，只能为合法数字
     * @return 0 成功，!= 0 失败
     */
    public static native int nativeSetDeviceId(long dev, String deviceId);

    /**
     * 设置鉴权码
     * @param dev 设备索引
     * @param authCode 设备鉴权码
     * @return 0 成功，!= 0 失败
     */
    public static native int nativeSetAuthCode(long dev, byte[] authCode);

    /**
     * 创建设备对象
     * @param deviceCallback 设备命令回调
     * @param cmdCallback 命令回调
     * @return 设备索引， = 0 失败
     */
    public static native long nativeDeviceCreate(WeakReference<IOntDeviceCb> deviceCallback, WeakReference<IOntCmdCb> cmdCallback);

    /**
     * 连接接入机
     * @param dev 设备索引
     * @param deviceId 设备ID，只能为合法数字
     * @return 0 = 成功， < 0 失败
     */
    public static native int nativeDeviceConnect(long dev, String deviceId, String ip, String port);

    /**
     * 密钥交换
     * @param dev 设备索引
     * @return 0 = 成功， < 0 失败
     */
    public static native int nativeDeviceRequestRsaPublicKey(long dev);

    /**
     * 新设备注册
     * @param dev 设备索引
     * @param regCode 注册码
     * @param authInfo 鉴权信息
     * @return 0 = 成功， < 0 失败 （注册成功后，可以获取设备ID，设备鉴权码用于保存，下次连接无需再注册，直接进行设备鉴权）
     */
    public static native int nativeDeviceRegister(long dev, String regCode, String authInfo);

    /**
     * 设备鉴权（鉴权成功即设备上线）
     * @param dev 设备索引
     * @return ret 0 = 成功， != 0 失败 authCode != NULL 返回新的鉴权码
     */
    public static native OntDeviceAuthRet nativeDeviceAuth(long dev);

    /**
     * 添加通道
     * @param dev 设备索引
     * @param channelId 通道id
     * @param title 通道名称
     * @param desc 通道描述
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceAddChannel(long dev, int channelId, String title, String desc);

    /**
     * 获取通道信息
     * @param dev 设备索引
     * @return 通道ID号， NULL 失败
     */
    public static native long[] nativeDeviceGetChannels(long dev);

    /**
     * 删除通道
     * @param dev 设备所以
     * @param channelId 通道ID
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceDelChannel(long dev, long channelId);

    /**
     * 维持链接
     * @param dev 设备所以
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceKeepAlive(long dev);

    /**
     * 设备断开连接（断开连接即下线）
     * @param dev 设备索引
     */
    public static native void nativeDeviceDisconnect(long dev);

    /**
     * 获取平台下发的命令
     * @param dev 设备索引
     * @param tm 超时时间（单位：毫秒）
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceCheckReceive(long dev, int tm);

    /**
     * 销毁内存虚拟设备信息
     * @param dev 设备索引
     */
    public static native void nativeDeviceDestroy(long dev);

    /**
     * 回复平台下发的命令
     * @param dev 设备索引
     * @param result 命令执行结果
     * @param cmdId 命令ID
     * @param resp 回复内容
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceReplyOntCmd(long dev, int result, String cmdId, String resp);

    /**
     * 回复平台下发的自定义命令
     * @param dev 设备索引
     * @param result 命令执行结果
     * @param cmdId 命令ID
     * @param resp 回复内容
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceReplyUserDefinedCmd(long dev, int result, String cmdId, String resp);

    /**
     * 数据上传
     * @param dev 设备索引
     * @param dataId 数据ID，只能为合法数字
     * @param data 数据内容
     * @param len 数据长度
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceDataUpload(long dev, String dataId, String data, int len);

    /**
     * 图片上传
     * @param dev 设备索引
     * @param picture 图片绝对路径
     * @param name 图片名称
     * @param channel 通道ID
     * @return 0 =  成功， < 0 失败
     */
    public static native int nativeDeviceUploadPicture(long dev, String picture, String name, long channel);

    /**
     * 设备请求推流并调用回调函数处理平台回复
     * @param dev             OneNET接入设备实例
     * @param channelId		 通道id
     * @param idleSec		 推流空闲时间,单位s
     * @return  PlatRespDevPS.net_result == 0 成功， != 0 失败
     */
    public static native PlatRespDevPS nativeDeviceRequestPushStream(long dev, long channelId, int idleSec);
}
