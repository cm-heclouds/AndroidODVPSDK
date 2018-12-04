package com.ont.odvp.sample.publish;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ont.media.odvp.model.PlatRespDevPS;
import com.ont.odvp.sample.R;
import com.ont.odvp.sample.def.ICallback;
import com.ont.odvp.sample.def.IPublishEventListener;
import com.ont.media.odvp.model.Resolution;
import com.ont.odvp.sample.def.IResolutonViewEventListener;
import com.ont.odvp.sample.device.OntOdvpProxy;

import java.util.Timer;
import java.util.TimerTask;

public class PublishActivity extends AppCompatActivity {

    private ViewGroup mRootView;
    boolean mIsRecording = false;
    boolean mIsPublishing = false;
    private Timer mTimer;
    private long mElapsedTime;
    public TimerHandler mTimerHandler;
    private RelativeLayout mTopTitle;
    private ImageButton mSettingsButton;
    private ImageButton mChangeButton;
    private  ImageButton mExitButton;
    private PublishResolutionView mCameraResolutionsView;
    private TextView mStreamLiveStatus;
    private PublishProxy mPublisher;
    private Button mPublishButton;
    private Button mRecordButton;

    private EventReceiver mEventReceiver;
    private IntentFilter mIntentFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_live_video_broadcaster);

        mTimerHandler = new TimerHandler();

        mRootView = findViewById(R.id.root_layout);
        mTopTitle = findViewById(R.id.top_title);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetResolutionPopupWindow();
            }
        });

        mChangeButton = findViewById(R.id.changeCameraButton);
        mExitButton = findViewById(R.id.exitButton);
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OntOdvpProxy.getInstance().disConnect(null);
                finish();
            }
        });

        mStreamLiveStatus = findViewById(R.id.stream_live_status);
        mPublishButton = findViewById(R.id.toggle_broadcasting);
        mPublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mIsPublishing) {

                    stopPublish();
                    return;
                }

                int ret = OntOdvpProxy.getInstance().requestPush(1, 1, new ICallback() {
                    @Override
                    public void onCallback(int ret, Object... parms) {

                        if (ret != 0) {
                            showToast("request push " + ret);
                        }

                        PlatRespDevPS resp = (PlatRespDevPS)parms[0];
                        startPublish(resp.push_url, OntOdvpProxy.getInstance().getDeviceId());
                    }
                });
                if (ret == OntOdvpProxy.ERROR_UNCONNECT) {
                    showToast("request " + "不在连接状态");
                }
            }
        });

        mRecordButton = findViewById(R.id.toggle_record);
        mRecordButton.setEnabled(false);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mIsRecording) {

                    mPublisher.stopRecord();
                    mRecordButton.setText(R.string.start_recording);
                    mRecordButton.setTextColor(getResources().getColor(R.color.colorActivatedLine));
                    mRecordButton.setBackgroundResource(R.drawable.btn_background_1);
                    mIsRecording = false;
                } else {

                    mPublisher.startRecord();
                    mRecordButton.setText("停止录制");
                    mRecordButton.setTextColor(getResources().getColor(android.R.color.white));
                    mRecordButton.setBackgroundResource(R.drawable.btn_background_2);
                    mIsRecording = true;
                }
            }
        });

        mPublisher = new PublishProxy(this, (OntGLSurfaceView) findViewById(R.id.cameraPreview_surfaceView), new IPublishEventListener() {

            @Override
            public void onStopRecord() {

                mRecordButton.setText(R.string.start_recording);
                mRecordButton.setTextColor(getResources().getColor(R.color.colorActivatedLine));
                mRecordButton.setBackgroundResource(R.drawable.btn_background_1);
                mIsRecording = false;
            }

            @Override
            public void onDisconnect() {

                mRecordButton.setEnabled(false);
                mPublishButton.setText(R.string.start_broadcasting);
                mPublishButton.setTextColor(getResources().getColor(R.color.colorActivatedLine));
                mPublishButton.setBackgroundResource(R.drawable.btn_background_1);
                mStreamLiveStatus.setVisibility(View.GONE);
                mStreamLiveStatus.setText(R.string.live_indicator);
                mSettingsButton.setVisibility(View.VISIBLE);
                stopTimer();

                mIsPublishing = false;
                //                    Snackbar.make(mRootView, "连接断开", -1).show();
                Toast toast = Toast.makeText(getApplicationContext(),
                        "连接断开", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            @Override
            public void onCameraResolutionUpdate() {

                if (mCameraResolutionsView != null) {

                    mCameraResolutionsView.updateCameraResolutions(mPublisher.getPreviewSizeList());
                }
            }
        });
        mPublisher.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        initResolutionView();

        mEventReceiver = new EventReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("com.ont.odvp.event.1001");
    }

    public void onBackPressed(){//重写返回键调用

        OntOdvpProxy.getInstance().disConnect(null);
        finish();
    }

    public void changeCamera(View v) {
        if (mPublisher != null) {
            mPublisher.changeCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if (requestCode == PublishProxy.PERMISSIONS_REQUEST_CODE) {

            mPublisher.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCameraResolutionsView.updateCameraResolutions(mPublisher.getPreviewSizeList());
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        mPublisher.onResume();
        registerReceiver(mEventReceiver, mIntentFilter);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.closeCamera();
        unregisterReceiver(mEventReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            mPublisher.updateDisplayOrientation();
        }
    }

    public void initResolutionView() {

        mCameraResolutionsView = new PublishResolutionView(this);
        mCameraResolutionsView.setResolutionViewEventListener(new IResolutonViewEventListener() {
            @Override
            public void onSelectResolutionUpdate(Resolution resolution) {

                mPublisher.setResolution(resolution);
            }
        });

        mCameraResolutionsView.updateCameraResolutions(mPublisher.getPreviewSizeList());
    }

    public void showSetResolutionPopupWindow() {

        float scale = getResources().getDisplayMetrics().density;
        mCameraResolutionsView.showAsDropDown(mChangeButton, mSettingsButton.getWidth() / 2, (int)(8 * scale + 0.5f));
        mCameraResolutionsView.setSelectedResolution(mPublisher.getPreviewResolution());
    }

    public void startPublish(String pushUrl, String deviceId) {

        if(mIsPublishing){
            return;
        }
        mPublisher.startPublish(pushUrl, deviceId);
        mRecordButton.setEnabled(true);
        mPublishButton.setText(R.string.stop_broadcasting);
        mPublishButton.setTextColor(getResources().getColor(android.R.color.white));
        mPublishButton.setBackgroundResource(R.drawable.btn_background_2);
        mStreamLiveStatus.setVisibility(View.VISIBLE);
        mSettingsButton.setVisibility(View.GONE);
        startTimer();

        mIsPublishing = true;
    }

    public void stopPublish() {

        if(!mIsPublishing){
            return;
        }
        mPublisher.stopPublish();
        mRecordButton.setEnabled(false);
        mPublishButton.setText(R.string.start_broadcasting);
        mPublishButton.setTextColor(getResources().getColor(R.color.colorActivatedLine));
        mPublishButton.setBackgroundResource(R.drawable.btn_background_1);
        mStreamLiveStatus.setVisibility(View.GONE);
        mStreamLiveStatus.setText(R.string.live_indicator);
        mSettingsButton.setVisibility(View.VISIBLE);
        stopTimer();

        mIsPublishing = false;
    }

    //This method starts a mTimer and updates the textview to show elapsed time for recording
    public void startTimer() {

        if(mTimer == null) {
            mTimer = new Timer();
        }

        mElapsedTime = 0;
        mTimer.scheduleAtFixedRate(
                new TimerTask() {

                    public void run() {
                        mElapsedTime += 1; //increase every sec
                        mTimerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();

                    }
        }, 0, 1000);
    }

    public void stopTimer()
    {
        if (mTimer != null) {
            this.mTimer.cancel();
        }
        this.mTimer = null;
        this.mElapsedTime = 0;
    }

    private class TimerHandler extends Handler {
        static final int INCREASE_TIMER = 1;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INCREASE_TIMER:
                    mStreamLiveStatus.setText(getString(R.string.live_indicator) + " : " + getDurationString((int) mElapsedTime));
                    break;
                default:
                    break;
            }
        }
    }

    public static String getDurationString(int seconds) {

        if(seconds < 0 || seconds > 2000000)//there is an codec problem and duration is not set correctly,so display meaningfull string
            seconds = 0;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if(hours == 0)
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        else
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    public static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PublishActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class EventReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if("com.ont.odvp.event.1001".equals(intent.getAction())){
                finish();
            }
        }
    }
}
