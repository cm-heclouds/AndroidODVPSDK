package com.ont.odvp.sample.publish;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.ont.media.odvp.OntFormat;
import com.ont.media.odvp.codec.EncodeMgr;
import com.ont.media.odvp.def.IEncodeDef;
import com.ont.media.odvp.def.IStreamDef;
import com.ont.media.odvp.model.PublishConfig;
import com.ont.media.odvp.model.Resolution;
import com.ont.media.odvp.player.AudioPlayer;
import com.ont.media.odvp.record.Mp4Record;
import com.ont.media.odvp.record.Mp4RecordHandler;
import com.ont.media.odvp.stream.OntStreamPusher;
import com.ont.odvp.sample.def.IGLViewEventListener;
import com.ont.odvp.sample.def.IPathDef;
import com.ont.odvp.sample.def.IPublishEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by betali on 2018/1/13 0013.
 */

public class PublishProxy {

    private static final String TAG = PublishProxy.class.getSimpleName();
    public static final int PERMISSIONS_REQUEST_CODE = 8954;
    private Activity mHostPage;
    private IPublishEventListener mPublishEventListener;

    private OntSurfaceView mGLSurfaceView;
    private EncodeMgr mEncodeMgr;
    private OntAudioRecord mOntAudioRecord;
    private OntStreamPusher mOntStreamPusher;
    private Mp4Record mMp4Record;
    private AudioPlayer mAudioPlayer;
    private PublishConfig mPublishConfig;

    private String mPublishUrl;
    private String mDeviceId;
    private boolean mPublishing;
    private boolean mNetworkWeakTriggered = false;
    private int count = 0;

    private Handler mPublishHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case IStreamDef.ON_PUBLISH_STREAM_DISCONNECT:

                    if (mPublishing) {
                        stopPublish(true);
                        if (mPublishEventListener != null) {

                            mPublishEventListener.onDisconnect();
                        }
                    }
                    break;
                case IStreamDef.ON_SOUND_MSG_START:

                    mAudioPlayer.start();
                    break;
                case IStreamDef.ON_SOUND_MSG_STOP:

                    mAudioPlayer.stop();
                    break;
                default:
                    break;
            }
        }
    };

    public PublishProxy(Activity hostPage, OntSurfaceView glSurfaceView, IPublishEventListener publishEventListener) {

        mHostPage = hostPage;
        mGLSurfaceView = glSurfaceView;
        mPublishEventListener = publishEventListener;

        mAudioPlayer = new AudioPlayer(mHostPage, mPublishHandler);
        mGLSurfaceView.setHostPage(hostPage);
        mGLSurfaceView.setGLViewEventListener(mGLViewEventListener);
        mOntStreamPusher = new OntStreamPusher(mPublishHandler, mAudioPlayer);

        mMp4Record = new Mp4Record(new Mp4RecordHandler(null));
        mEncodeMgr = new EncodeMgr(mOntStreamPusher, mMp4Record);
        mOntAudioRecord = new OntAudioRecord(mEncodeMgr);
    }

    public void setPublishConfig(PublishConfig publishConfig) {

        if (publishConfig == null) {

            this.mPublishConfig = new PublishConfig();
        } else {

            this.mPublishConfig = publishConfig;
        }

        mGLSurfaceView.setPublishConfig(mPublishConfig.getVideoFrameRate());
    }

    public void openCamera(int cameraId, boolean firstInstall) {

        if (!isPermissionGranted())
        {
            requestPermission();
            return;
        }
        mGLSurfaceView.openCamera(cameraId);
    }

    public void closeCamera() {

        mGLSurfaceView.closePreview();
    }

    public void startShot(){

        mGLSurfaceView.setVideoShot(true);
        mGLSurfaceView.changeFilename();
    }

    public void onResume() {

        mAudioPlayer.onResume();
    }

    public boolean startPublish(String pushUrl, String deviceId, boolean startStream) {

        mPublishUrl = pushUrl;
        mDeviceId = deviceId;

        // init publish config
        if (mPublishConfig.isEnableAudio()) {

            if (mPublishConfig.getAudioChannelConfig() == 0) {
                mPublishConfig.setAudioChannelConfig(mOntAudioRecord.chooseChannelConfig(mPublishConfig.getAudioSource(), mPublishConfig.getAudioSampleRate(), mPublishConfig.getAudioSampleSize()));
            } else {
                mOntAudioRecord.initChannelConfig(mPublishConfig.getAudioSource(), mPublishConfig.getAudioSampleRate(), mPublishConfig.getAudioSampleSize(), mPublishConfig.getAudioChannelConfig());
            }
        }
        if (mPublishConfig.getVideoColorFormat() == 0) {
            mPublishConfig.setVideoColorFormat(mEncodeMgr.chooseVideoColorFormat(null));
        }
        mPublishConfig.setWidth(mGLSurfaceView.getResolutionWidth());
        mPublishConfig.setHeight(mGLSurfaceView.getResolutionHeight());
        mPublishConfig.setVideoBitrate(mGLSurfaceView.getBitrate());

        // init stream
		if (startStream) {
        	
			mOntStreamPusher.start(pushUrl, deviceId);
		}
        mOntStreamPusher.setPublishConfig(mPublishConfig);
        mOntStreamPusher.sendMetadata();

        // init encoder
        if (mPublishConfig.isWaterMark()) {
            String format = "yyyy-MM-dd HH:mm:ss";
            OntFormat.WaterMarkInit(50, 50, format.length());
        }
        mEncodeMgr.setPublishConfig(mPublishConfig);
        if (!mEncodeMgr.start()) {

            stopPublish(true);
            return false;
        }

        // init audio Record
        if (mPublishConfig.isEnableAudio()) {

            mOntAudioRecord.start();
        }
        mGLSurfaceView.enableRecord();
        mAudioPlayer.setPublishRunning(true);
        mPublishing = true;
        return true;
    }

    public boolean stopPublish(boolean stopStream) {

        mAudioPlayer.setPublishRunning(false);
        mAudioPlayer.stop();
        mMp4Record.stop();
        mGLSurfaceView.disableRecord();
        if (mPublishConfig.isEnableAudio()) {
            mOntAudioRecord.stop();
        }
        mEncodeMgr.stop();
        if (mPublishConfig.isWaterMark()) {
            OntFormat.WaterMarkRelease();
        }

        if (stopStream) {

            mOntStreamPusher.stop();
            mPublishing = false;
        }
        if (mPublishEventListener != null) {

            mPublishEventListener.onStopRecord();
        }
        return true;
    }

    public boolean startRecord() {

        return mMp4Record.record(new File(genOntVideoName()));
    }

    public void stopRecord() {

        mMp4Record.stop();
    }

    public boolean isPermissionGranted() {

        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(mHostPage, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean microPhonePermissionGranted = ContextCompat.checkSelfPermission(mHostPage, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        boolean writeSdCardPermissionGranted = ContextCompat.checkSelfPermission(mHostPage, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return cameraPermissionGranted && microPhonePermissionGranted && writeSdCardPermissionGranted;
    }

    public void requestPermission() {

        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(mHostPage, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean microPhonePermissionGranted = ContextCompat.checkSelfPermission(mHostPage, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        boolean writeSdCardPermissionGranted = ContextCompat.checkSelfPermission(mHostPage, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        final List<String> permissionList = new ArrayList();
        if (!cameraPermissionGranted) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (!microPhonePermissionGranted) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!writeSdCardPermissionGranted) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionList.size() > 0 )
        {
            String[] permissionArray = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(mHostPage, permissionArray, PERMISSIONS_REQUEST_CODE);
        }
    }

    public void updateDisplayOrientation() {

        boolean publishing = mPublishing;
        if (publishing) {

            stopPublish(true);
        }

        mGLSurfaceView.updateDisplayOrientation();
        if (publishing) {

            startPublish(mPublishUrl, mDeviceId, true);
        }
    }

    public ArrayList<Resolution> getPreviewSizeList() {

        return mGLSurfaceView.getPreviewSizeList();
    }

    public Resolution getPreviewResolution() {

        return new Resolution(mGLSurfaceView.getPreviewWidth(), mGLSurfaceView.getPreviewHeight());
    }

    public void setResolution(Resolution size) {

        mGLSurfaceView.setResolution(size);
    }

    public void changeCamera() {

        final boolean publishing = mPublishing;
        final boolean isResolutionChange = mGLSurfaceView.changeCamera();
        if (publishing && isResolutionChange) {

            startPublish(mPublishUrl, mDeviceId, true);
        }
    }

    private IGLViewEventListener mGLViewEventListener = new IGLViewEventListener() {

        @Override
        public void onGetVideoFrame(byte[] data, boolean flip, int rotation, int width, int height) {

            AtomicInteger videoMsgCacheNumber = mOntStreamPusher.getVideoMsgCacheNumber();
            if (videoMsgCacheNumber == null || videoMsgCacheNumber.get() > IEncodeDef.VIDEO_MAX_CACHE_NUMBER) {

                if (!mNetworkWeakTriggered) {

                    mNetworkWeakTriggered = true;
                    mPublishEventListener.onNetworkWeak();
                }
            } else {

                if (mNetworkWeakTriggered) {

                    mNetworkWeakTriggered = false;
                    mPublishEventListener.onNetworkResume();
                }
                mEncodeMgr.onGetVideoNV21Frame(data, flip, rotation, width, height);
            }
        }

        @Override
        public void onGetCameraSupportResolutions() {

            mPublishEventListener.onCameraResolutionUpdate();
        }

        @Override
        public void onChangeCameraConfirmResolution(boolean isResolutionChange) {

            if (mPublishing && isResolutionChange) {

                stopPublish(true);
            }
        }

        @Override
        public void onCameraAutoFocusCallback(boolean success) {

            mPublishEventListener.onCameraAutoFocusCallback(success);
        }

        @Override
        public void onCameraOpen() {

            mPublishEventListener.onCameraOpen();
        }
    };

    private String genOntVideoName() {

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        return IPathDef.VOD_FILE_PATH + File.separator + format.format(date) + ".mp4";
    }
}
