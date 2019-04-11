package com.ont.odvp.sample.device;

import android.os.Handler;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.ont.media.odvp.IOntCmdCb;
import com.ont.media.odvp.IOntDeviceCb;
import com.ont.media.odvp.OntDeviceAuthRet;
import com.ont.media.odvp.OntDeviceInfoRet;
import com.ont.media.odvp.OntOdvp;
import com.ont.media.odvp.OntOnvif;
import com.ont.media.odvp.OntRtmp;
import com.ont.media.odvp.model.PlatRespDevPS;
import com.ont.media.odvp.model.PlatformCmd;
import com.ont.media.odvp.model.VideoFileInfo;
import com.ont.media.odvp.model.VodBaseInfo;
import com.ont.media.odvp.model.VodCmdReply;
import com.ont.media.odvp.model.VodInfo;
import com.ont.odvp.sample.BuildConfig;
import com.ont.odvp.sample.def.ICallback;
import com.ont.odvp.sample.def.ICmdListener;
import com.ont.odvp.sample.def.ICmdExecutor;
import com.ont.odvp.sample.def.IPathDef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CmdExecutor extends ICmdExecutor {

    private Handler mCmdThreadHandler;
    private ICmdListener mCmdListener;

    private String mDeviceId;
    private byte[] mAuthCode;
    private String mRegCodeCopy, mAuthInfoCopy;
    private boolean mEnableEncrypt;
    private ICmdListener.QueryFilesRet mQueryFilesRet;

    private long mDeviceReference;
    private volatile boolean mConnect;
    private boolean isCancel;
    private long mTmLast;

    public CmdExecutor(Handler cmdThreadHandler) {
        this.mCmdThreadHandler = cmdThreadHandler;
    }

    public void setCmdListener(ICmdListener cmdListener) {
        this.mCmdListener = cmdListener;
    }

    public void setDeviceId(String deviceId) {
        this.mDeviceId = deviceId;
    }

    public void setAuthCode(byte[] authCode) {
        this.mAuthCode = authCode;
    }

    public void setEnableEncrypt(boolean enableEncrypt) {
        this.mEnableEncrypt = enableEncrypt;
    }

    public boolean isConnect() {
        return mConnect;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    @Override
    public void stopLoop() {

        isCancel = true;
    }

    @Override
    public void startLoop() {

        isCancel = false;
    }

    private IOntDeviceCb deviceCallback = new IOntDeviceCb() {

        @Override
        public void onKeepAliveResp() {

        }

        @Override
        public int onLiveStreamStart(int channel, byte proType, int ttl, String pushUrl) {

            int ret = 0;
            if (mCmdListener != null) {
                ret = mCmdListener.videoLiveStreamStart(mDeviceReference, channel, proType, ttl, pushUrl);
            }
            return ret;
        }

        @Override
        public int onVodStreamStart(int channelId, byte proType, VideoFileInfo fileInfo, String playFlag, String pushUrl, int ttl) {

            if (mCmdListener == null) {

                return -1;
            }

            String location = getFileLocation(fileInfo.begin_time, fileInfo.end_time);
            if (TextUtils.isEmpty(location)) {

                return -1;
            }

            return mCmdListener.videoVodStreamStart(mDeviceReference, location, channelId, proType, playFlag, pushUrl, ttl);
        }

        @Override
        public int onChannelRecordUpdate(int channel, int status, int seconds, String url) {
            return 0;
        }

        @Override
        public int onUserDefinedCmd(PlatformCmd cmd) {

            if (cmd.need_resp) {

                OntOdvp.nativeDeviceReplyUserDefinedCmd(mDeviceReference, 0, cmd.id, "test response");
            } else {

                OntOdvp.nativeDeviceReplyUserDefinedCmd(mDeviceReference, 4, null, null);
            }
            return 0;
        }

        @Override
        public int onApiDefinedMsg(byte[] msg, int msgLen) {

            return 0;
        }

        /*@Override
        public int onPlatRespDevPushStreamMsg(PlatRespDevPS resp) {

            int ret = 0;
            if (mCmdListener != null) {
                ret = mCmdListener.videoLiveStreamStart(mDeviceReference, resp.chan_id, (byte)0, resp.url_ttl_min, resp.push_url);
            }
            return ret;
        }*/
    };

    private IOntCmdCb cmdCallback = new IOntCmdCb() {

        @Override
        public int onCmdLiveStreamCtrl(int channelId, int level) {

            if (mCmdListener == null) {

                return -1;
            }
            return mCmdListener.videoLiveStreamCtrl(mDeviceReference, channelId, level);
        }

        @Override
        public int onCmdPtzCtrl(int channelId, int mode, int ptz, int speed) {

            if (mCmdListener == null) {

                return -1;
            }

            return mCmdListener.videoDevPtzCtrl(mDeviceReference, channelId, mode, ptz, speed);
        }

        @Override
        public int onCmdQueryFiles(int channelId, int page, int startIndex, int max, String startTime, String endTime, String cmdId) {

            if (mCmdListener == null) {

                return -1;
            }

            return internalVideoDevQueryFiles(channelId, page, startIndex, max, startTime, endTime, cmdId);
        }
    };

    private int internalVideoDevQueryFiles(int channelId, int page, int startIndex, int max, String startTime, String endTime, String cmdId) {

        // get files
        mQueryFilesRet = mCmdListener.videoDevQueryFiles(channelId, startIndex, max, startTime, endTime);

        // reply
        VodCmdReply vodCmdReply = new VodCmdReply();
        vodCmdReply.all_count = mQueryFilesRet.totalNum;
        vodCmdReply.cur_count = mQueryFilesRet.curNum;
        vodCmdReply.page = page;
        vodCmdReply.per_page = max;
        vodCmdReply.page_total = mQueryFilesRet.pageTotal;
        vodCmdReply.rvods = transferVodBaseInfo(mQueryFilesRet.files);
        final String cmdReply = new Gson().toJson(vodCmdReply);
        OntOdvp.nativeDeviceReplyOntCmd(mDeviceReference, 0, cmdId, cmdReply);

        return 0;
    }

    private String getFileLocation(String beginTime, String endTime) {

        List<VodInfo> listVodInfo = mQueryFilesRet == null ? null : mQueryFilesRet.files;
        if (listVodInfo == null) {

            return null;
        }

        Iterator<VodInfo> iterator = listVodInfo.iterator();
        while (iterator.hasNext()) {

            VodInfo info = iterator.next();
            if (info == null || info.baseInfo == null) {

                continue;
            }

            if (info.baseInfo.beginTime.equals(beginTime) && info.baseInfo.endTime.equals(endTime)) {

                return info.location;
            }
        }

        return null;
    }

    private ArrayList<VodBaseInfo> transferVodBaseInfo(List<VodInfo> listVodInfo) {

        if (listVodInfo == null) {

            return null;
        }

        ArrayList<VodBaseInfo> listBaseInfo = new ArrayList<>();
        Iterator<VodInfo> iterator = listVodInfo.iterator();
        while (iterator.hasNext()) {

            VodInfo info = iterator.next();
            if (info != null) {

                listBaseInfo.add(info.baseInfo);
            }
        }

        return listBaseInfo;
    }

    public void postTask(final int type, final ICallback callback, final Object... parms) {

        postTaskDelayed(type, 0, callback, parms);
    }

    public void postTaskDelayed(final int type, long delayMillis, final ICallback callback, final Object... parms) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int ret = 0;
                switch (type) {
                    case TaskType.TYPE_CONNECT:
                        ret = connect((DeviceInfo) parms[0], callback);
                        OntOdvpProxy.getInstance().postResult(ret, callback);
                        break;
                    case TaskType.TYPE_DISCONNECT:
                        disconnect();
                        OntOdvpProxy.getInstance().postResult(0, callback);
                        break;
                    case TaskType.TYPE_GET_CHANNELS:
                        final long[] channels = getChannels();
                        OntOdvpProxy.getInstance().postResult(0, callback, channels);
                        break;
                    case TaskType.TYPE_DELETE_CHANNEL:
                        ret = deleteChannel((long)parms[0]);
                        OntOdvpProxy.getInstance().postResult(ret, callback);
                        break;
                    case TaskType.TYPE_UPDATE_DATA:
                        ret = updateData((String)parms[0], (String)parms[1], (int)parms[2]);
                        OntOdvpProxy.getInstance().postResult(ret, callback);
                        break;
                    case TaskType.TYPE_KEEP_ALIVE:
                        ret = keepAlive();
                        if (ret < 0) {
                            disconnect();
                            OntOdvpProxy.getInstance().postCmdListenerEvent(ICmdListener.EventType.TYPE_DISCONNECT);
                        }
                        break;
                    case TaskType.TYPE_REQUEST_PUSH:
                        PlatRespDevPS resp = OntOdvp.nativeDeviceRequestPushStream(mDeviceReference, (long)parms[0], (int)parms[1]);
                        OntOdvpProxy.getInstance().postResult(resp.net_result, callback, resp);
                        break;
                    default:
                        break;
                }
            }
        };

        if (delayMillis > 0) {
            mCmdThreadHandler.postDelayed(runnable, delayMillis);
        } else {
            mCmdThreadHandler.post(runnable);
        }
    }

    private int keepAlive () {

        if(isCancel) {

            return 0;
        }

        int ret = 0;
        long tmNow = System.currentTimeMillis();
        if (tmNow - mTmLast > 30000) {

            ret = OntOdvp.nativeDeviceKeepAlive(mDeviceReference);
            mTmLast = tmNow;
        }

        ret = OntOdvp.nativeDeviceCheckReceive(mDeviceReference, 0);
        if (ret < 0) {

            return ret;
        }

        int delta = OntRtmp.nativePlayListSingleStep(mDeviceReference);
        if (delta != 0) {

            postTaskDelayed(TaskType.TYPE_KEEP_ALIVE, delta > 100 ? 100 : delta, null);
        } else {

            postTask(TaskType.TYPE_KEEP_ALIVE, null);
        }

        return 0;
    }

    private int connect(DeviceInfo deviceInfo, ICallback callback) {

        // create device reference
        mDeviceReference = OntOdvp.nativeDeviceCreate(new WeakReference<IOntDeviceCb>(deviceCallback), new WeakReference<IOntCmdCb>(cmdCallback));
        if (mDeviceReference == 0) {

            return -1;
        }

        // connect to access
        int ret = OntOdvp.nativeDeviceConnect(mDeviceReference, "0", deviceInfo.ip, deviceInfo.port);
        if (ret != 0) {

            OntOdvp.nativeDeviceDestroy(mDeviceReference);
            return ret;
        }

        // exchange key
        int requestTimeout = 1000;
        if (mEnableEncrypt) {
            ret = OntOdvp.nativeDeviceRequestRsaPublicKey(mDeviceReference);
            if (ret != 0) {

                OntOdvp.nativeDeviceDisconnect(mDeviceReference);
                OntOdvp.nativeDeviceDestroy(mDeviceReference);
                return ret;
            }
            requestTimeout = 10000;
        }

        // check receive
        ret = OntOdvp.nativeDeviceCheckReceive(mDeviceReference, requestTimeout);
        if (ret != 0) {

            OntOdvp.nativeDeviceDisconnect(mDeviceReference);
            OntOdvp.nativeDeviceDestroy(mDeviceReference);
            return ret;
        }

        if (TextUtils.isEmpty(mDeviceId) || mAuthCode == null || mAuthCode.length <= 0 || !deviceInfo.regCode.equals(mRegCodeCopy) || !deviceInfo.authInfo.equals(mAuthInfoCopy)) {

            // device register
            mRegCodeCopy = deviceInfo.regCode;
            mAuthInfoCopy = deviceInfo.authInfo;
            ret = OntOdvp.nativeDeviceRegister(mDeviceReference, deviceInfo.regCode, deviceInfo.authInfo);
            if (ret != 0) {

                OntOdvp.nativeDeviceDisconnect(mDeviceReference);
                OntOdvp.nativeDeviceDestroy(mDeviceReference);
                return ret;
            }

            // update device information
            mDeviceId = OntOdvp.nativeGetDeviceId(mDeviceReference);
            mAuthCode = OntOdvp.nativeGetAuthCode(mDeviceReference);
            OntOdvpProxy.getInstance().postResult(0, callback, new OntDeviceInfoRet(mDeviceId, mAuthCode));
        } else {

            // set device information
            OntOdvp.nativeSetDeviceId(mDeviceReference, mDeviceId);
            OntOdvp.nativeSetAuthCode(mDeviceReference, mAuthCode);
        }

        // authenticate device
        OntDeviceAuthRet authRet = OntOdvp.nativeDeviceAuth(mDeviceReference);
        if (authRet.ret != 0) {

            OntOdvp.nativeDeviceDisconnect(mDeviceReference);
            OntOdvp.nativeDeviceDestroy(mDeviceReference);
            return authRet.ret;
        }
        if (authRet.authCode != null && authRet.authCode.length > 0) {

            mAuthCode = authRet.authCode;
            OntOdvpProxy.getInstance().postResult(0, callback, new OntDeviceInfoRet(mDeviceId, mAuthCode));
        }

        // 添加内置摄像头通道
        OntOdvp.nativeDeviceAddChannel(mDeviceReference, 1, "num1", "内置摄像头");

        if (BuildConfig.onvifType == 1) {

            // 添加onvif摄像头通道
            String cameraConfig = getCameraConfig();
            if (!TextUtils.isEmpty(cameraConfig)) {
                OntOnvif.addChannel(mDeviceReference, getCameraConfig());
            }
        }

        mTmLast = System.currentTimeMillis();
        startLoop();
        mConnect = true;
        postTask(TaskType.TYPE_KEEP_ALIVE, null);
        return ret;
    }

    private void disconnect() {

        stopLoop();
        if (mConnect) {
            OntOdvp.nativeDeviceDisconnect(mDeviceReference);
            OntOdvp.nativeDeviceDestroy(mDeviceReference);
        }
        mConnect = false;
    }

    private long[] getChannels() {

        return OntOdvp.nativeDeviceGetChannels(mDeviceReference);
    }

    private int deleteChannel(long channelId) {

        return OntOdvp.nativeDeviceDelChannel(mDeviceReference, channelId);
    }

    private int updateData(String id, String data, int len) {

        return OntOdvp.nativeDeviceDataUpload(mDeviceReference, id, data, len);
    }

    private String getCameraConfig() {

        StringBuffer out = new StringBuffer();

        try {
            File file = new File(IPathDef.DEVICE_CONFIG_PATH);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String outLine = null;

            while ((outLine = reader.readLine()) != null) {

                out.append(outLine);
            }

        } catch (IOException e) {
            return null;
        }

        return out.toString();
    }
}
