package com.ont.odvp.sample.device;

import android.os.Handler;
import android.os.HandlerThread;

//import com.ont.media.odvp.OntOnvif;
import com.ont.odvp.sample.def.ICallback;
import com.ont.odvp.sample.def.ICmdListener;
import com.ont.odvp.sample.def.ICmdExecutor;


public class OntOdvpProxy {

    public static final int ERROR_UNCONNECT = -9998;
    public static final int ERROR_ALREADY_CONNECT = -9997;

    private Handler mHostThreadHandler;
    private ICmdListener mCmdListener;

    private HandlerThread mCmdThread;
    private Handler mCmdThreadHandler;
    private CmdExecutor mCmdExecutor;

    private OntOdvpProxy() {}
    static class HOLDER {

        private static OntOdvpProxy instance = new OntOdvpProxy();
    }
    public static OntOdvpProxy getInstance() {

        return HOLDER.instance;
    }

    public void init(String deviceId, byte[] authCode) {

        mCmdThread = new HandlerThread("ODVP_EXECUTE");
        mCmdThread.start();
        mCmdThreadHandler = new Handler(mCmdThread.getLooper());

        mCmdExecutor = new CmdExecutor(mCmdThreadHandler);
        mCmdExecutor.setDeviceId(deviceId);
        mCmdExecutor.setAuthCode(authCode);
    }

    public void setHostThreadHandler(Handler hostThreadHandler) {

        mHostThreadHandler = hostThreadHandler;
    }

    public void setEnableEncrypt(boolean enableEncrypt) {

        mCmdExecutor.setEnableEncrypt(enableEncrypt);
    }

    public boolean isConnect() {

        return mCmdExecutor.isConnect();
    }

    public String getDeviceId() {

        return mCmdExecutor.getDeviceId();
    }

    public int connect(final DeviceInfo deviceInfo, final ICmdListener cmdListener, final ICallback callback) {

        if (isConnect()) {

            return ERROR_ALREADY_CONNECT;
        }

        mCmdListener = cmdListener;
        mCmdExecutor.setCmdListener(cmdListener);

        mCmdExecutor.postTask(ICmdExecutor.TaskType.TYPE_CONNECT, callback, deviceInfo);
        return 0;
    }

    public int disConnect(final ICallback callback) {

        if (!isConnect()) {

            return ERROR_UNCONNECT;
        }

        mCmdExecutor.postTask(ICmdExecutor.TaskType.TYPE_DISCONNECT, callback);
        return 0;
    }

    public int getChannels(final ICallback callback) {

        if (!isConnect()) {

            return ERROR_UNCONNECT;
        }

        mCmdExecutor.postTask(ICmdExecutor.TaskType.TYPE_GET_CHANNELS, callback);
        return 0;
    }

    public int delChannel(final long channelId, final ICallback callback) {

        if (!isConnect()) {

            return ERROR_UNCONNECT;
        }

        mCmdExecutor.postTask(ICmdExecutor.TaskType.TYPE_DELETE_CHANNEL, callback, channelId);
        return 0;
    }

    public int uploadData(final String id, final String data, final int len, final ICallback callback) {

        if (!isConnect()) {

            return ERROR_UNCONNECT;
        }

        mCmdExecutor.postTask(ICmdExecutor.TaskType.TYPE_UPDATE_DATA, callback, id, data, len);
        return 0;
    }

    public int requestPush(long channelId, int idleSec, ICallback callback) {

        if (!isConnect()) {
            return ERROR_UNCONNECT;
        }

        mCmdExecutor.postTask(ICmdExecutor.TaskType.TYPE_REQUEST_PUSH, callback, channelId, idleSec);
        return 0;
    }

    public void postResult(final int ret, final ICallback callback, final Object... parms) {

        if (mHostThreadHandler == null || callback == null) {

            return;
        }

        mHostThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                callback.onCallback(ret, parms);
            }
        });
    }

    public void postCmdListenerEvent(final int type) {

        if (mHostThreadHandler == null) {

            return;
        }

        mHostThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                switch (type) {
                    case ICmdListener.EventType.TYPE_DISCONNECT:
                        mCmdListener.onDisconnect();
                        break;
                    default:
                        break;
                }
            }
        });
    }
}
