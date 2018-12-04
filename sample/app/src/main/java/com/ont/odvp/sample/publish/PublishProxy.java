package com.ont.odvp.sample.publish;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.ont.odvp.sample.def.IGLViewEventListener;
import com.ont.odvp.sample.def.IPublishEventListener;
import com.ont.odvp.sample.def.IPathDef;
import com.ont.media.odvp.codec.EncodeMgr;
import com.ont.media.odvp.def.IStreamDef;
import com.ont.media.odvp.model.Resolution;
import com.ont.media.odvp.player.AudioPlayer;
import com.ont.media.odvp.record.Mp4Record;
import com.ont.media.odvp.record.Mp4RecordHandler;
import com.ont.media.odvp.stream.OntStreamPusher;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by betali on 2018/1/13 0013.
 */

public class PublishProxy {

    private static final String TAG = PublishProxy.class.getSimpleName();
    public static final int PERMISSIONS_REQUEST_CODE = 8954;
    private Activity mHostPage;
    private IPublishEventListener mPublishEventListener;

    private OntGLSurfaceView mGLSurfaceView;
    private EncodeMgr mEncodeMgr;
    private OntAudioRecord mOntAudioRecord;
    private OntStreamPusher mOntStreamPusher;
    private Mp4Record mMp4Record;
    private AudioPlayer mAudioPlayer;

    private String mPublishUrl;
    private String mDeviceId;
    private boolean mPublishing;
    private int count = 0;

    private Handler mPublishHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case IStreamDef.ON_PUBLISH_STREAM_DISCONNECT:

                    if (mPublishing) {
                        stopPublish();
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

    public PublishProxy(Activity hostPage, OntGLSurfaceView glSurfaceView, IPublishEventListener publishEventListener) {

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

    public void openCamera(int cameraId) {

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

    public boolean startPublish(String pushUrl, String deviceId) {

        mPublishUrl = pushUrl;
        mDeviceId = deviceId;
        int channelConfig = mOntAudioRecord.initChannelConfig();

        mOntStreamPusher.setWidth(mGLSurfaceView.getResolutionWidth());
        mOntStreamPusher.setHeight(mGLSurfaceView.getResolutionHeight());
        mOntStreamPusher.setChannelConfig(channelConfig == AudioFormat.CHANNEL_IN_STEREO ? 1 : 0);
        mOntStreamPusher.start(pushUrl, deviceId);

        mEncodeMgr.setAudioChannelConfig(channelConfig);
        mEncodeMgr.setVideoWidth(mGLSurfaceView.getResolutionWidth());
        mEncodeMgr.setVideoHeight(mGLSurfaceView.getResolutionHeight());
        mEncodeMgr.setVideoBitrate(mGLSurfaceView.getBitrate());
        mEncodeMgr.start();

        mOntAudioRecord.start();
        mGLSurfaceView.enableRecord();
        mAudioPlayer.setPublishRunning(true);
        mPublishing = true;
        return true;
    }

    public boolean stopPublish() {

        mAudioPlayer.setPublishRunning(false);
        mAudioPlayer.stop();
        mMp4Record.stop();
        mGLSurfaceView.disableRecord();
        mOntAudioRecord.stop();
        mEncodeMgr.stop();
        mOntStreamPusher.stop();

        mPublishing = false;

        if (mPublishEventListener != null) {

            mPublishEventListener.onStopRecord();
        }
        return true;
    }

    public void startRecord() {

        mMp4Record.record(new File(genOntVideoName()));
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
            stopPublish();
        }

        mGLSurfaceView.updateDisplayOrientation();
        if (publishing) {

            startPublish(mPublishUrl, mDeviceId);
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

            startPublish(mPublishUrl, mDeviceId);
        }
    }

    private IGLViewEventListener mGLViewEventListener = new IGLViewEventListener() {

        @Override
        public void onGetVideoFrame(byte[] data, int width, int height) {

            mEncodeMgr.onGetVideoRgbaFrame(data, width, height);
        }

        @Override
        public void onGetCameraSupportResolutions() {

            mPublishEventListener.onCameraResolutionUpdate();
        }

        @Override
        public void onChangeCameraConfirmResolution(boolean isResolutionChange) {

            if (mPublishing && isResolutionChange) {

                stopPublish();
            }
        }
    };

    private String genOntVideoName() {

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return IPathDef.VOD_FILE_PATH + File.separator + format.format(date) + ".mp4";
    }
}
