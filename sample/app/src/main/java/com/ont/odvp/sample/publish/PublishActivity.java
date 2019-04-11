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
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ont.media.odvp.model.PlatRespDevPS;
import com.ont.media.odvp.model.PublishConfig;
import com.ont.media.odvp.model.Resolution;
import com.ont.odvp.sample.R;
import com.ont.odvp.sample.def.ICallback;
import com.ont.odvp.sample.def.IPublishEventListener;
import com.ont.odvp.sample.def.IResolutonViewEventListener;
import com.ont.odvp.sample.device.OntOdvpProxy;

import java.lang.ref.WeakReference;
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
    private OntSurfaceView mSurfaceView;
    private LinearLayout layoutZoomSeek;
    private VerticalSeekBar seekBarZoom;
    private ImageView ivFocus;
    private ImageView ivZoomAdd;
    private ImageView ivZoomMinus;

    //OnTouch触摸点
    private float x_down = 0;
    private float y_down = 0;
    private float y_move = 0;
    private float x_up = 0;
    private float y_up = 0;
    private int zoom = 0;  //变焦数组下标，最小为0，最大为getMaxZoom();
    private int maxZoom = -1;  //变焦最大值，也是SeekBar最大值，这个值只有在openCamera后才能获取到
    private boolean canFocus = true;   //当前是否能点击屏幕对焦

    private VideoHandler videoHandler;
    private Thread showThread;   //用于专门控制界面上模块显示隐藏的线程
    private boolean flagShowThread = true;   //用于标志退出页面时结束showThread

    private long timeSeek = 0;   //用于控制变焦SeekBar区域显示隐藏
    private long showTimeSeek = 3 * 1000;   //用于控制变焦SeekBar区域等多少秒后消失

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_live_video_broadcaster);
        initViews();

        mTimerHandler = new TimerHandler();
        videoHandler = new VideoHandler(this);
        showThread = new Thread(showRunnable);
        showThread.start();

        mRootView = findViewById(R.id.root_layout);
        mTopTitle = findViewById(R.id.top_title);
        layoutZoomSeek = findViewById(R.id.ll_zoom_seek);
        seekBarZoom = findViewById(R.id.zoom_seek_bar);
        seekBarZoom.setThumb(getResources().getDrawable(R.drawable.shape_zoom_seek_bar_thumb));
        seekBarZoom.setOnSeekBarChangeListener(onSeekBarChangeListener);

        ivFocus = findViewById(R.id.iv_focus);
        ivZoomAdd = findViewById(R.id.iv_zoom_add);
        ivZoomAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(maxZoom == -1){
                    return;
                }

                timeSeek = System.currentTimeMillis(); //让线程控制layoutZoomSeek在显示几秒后消失，重新赋值，让其重新计时

                zoom = zoom + 1;
                if(zoom > maxZoom){
                    zoom = maxZoom;
                }
                mSurfaceView.startZoom(zoom);
                seekBarZoom.setProgress(zoom);
                seekBarZoom.setThumbPosition((zoom*1.0f)/(maxZoom*1.0f)); //滑块位置
            }
        });
        ivZoomMinus = findViewById(R.id.iv_zoom_minus);
        ivZoomMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(maxZoom == -1){
                    return;
                }

                timeSeek = System.currentTimeMillis(); //让线程控制layoutZoomSeek在显示几秒后消失，重新赋值，让其重新计时

                zoom = zoom - 1;
                if(zoom < 0){
                    zoom = 0;
                }
                mSurfaceView.startZoom(zoom);
                seekBarZoom.setProgress(zoom);
                seekBarZoom.setThumbPosition((zoom*1.0f)/(maxZoom*1.0f)); //滑块位置
            }
        });
        mSettingsButton = findViewById(R.id.settings_btn);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetResolutionPopupWindow();
            }
        });

        mChangeButton = findViewById(R.id.change_camera);
        mExitButton = findViewById(R.id.exit_btn);
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
                    finish();
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

                    if (!mPublisher.startRecord()) {
                        return;
                    }
                    mRecordButton.setText("停止录制");
                    mRecordButton.setTextColor(getResources().getColor(android.R.color.white));
                    mRecordButton.setBackgroundResource(R.drawable.btn_background_2);
                    mIsRecording = true;
                }
            }
        });

        mPublisher = new PublishProxy(this, mSurfaceView, new IPublishEventListener() {

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

            @Override
            public void onNetworkWeak() {

//                Toast toast = Toast.makeText(getApplicationContext(), "网络状况不佳，请检查网络连接", Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.show();
            }

            @Override
            public void onNetworkResume() {

//                Toast toast = Toast.makeText(getApplicationContext(), "网络状况已恢复良好", Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.show();
            }

            @Override
            public void onCameraAutoFocusCallback(boolean success) {
                onFocusEnd();
                canFocus = true;
            }

            @Override
            public void onCameraOpen() {

                //设置变焦拖动条最大值，在openCamera后，maxZoom才会有值
                int maxZoom = mSurfaceView.getMaxZoom();
                if(maxZoom > -1){
                    seekBarZoom.setMax(maxZoom);
                    PublishActivity.this.maxZoom = maxZoom;
                }
            }
        });
        PublishConfig publishConfig = new PublishConfig();
        mPublisher.setPublishConfig(publishConfig);
        mPublisher.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK, false);
        initResolutionView();

        mEventReceiver = new EventReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("com.ont.odvp.event.1001");
    }

    private void initViews(){

        mSurfaceView = findViewById(R.id.camera_preview);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {

                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x_down = motionEvent.getX();
                        y_down = motionEvent.getY();
                        y_move = y_down;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        doOnTouchMove(motionEvent);
                        y_move = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        x_up = motionEvent.getX();
                        y_up = motionEvent.getY();
                        doOnTouchUp(motionEvent);
                        break;
                }
                return true;
            }
        });
    }

    private void doOnTouchMove(MotionEvent motionEvent) {
        if(maxZoom == -1){
            return;
        }

        if(Math.abs(motionEvent.getY() - y_move) > 30){  //每30重新设置一遍变焦

            layoutZoomSeek.setVisibility(View.VISIBLE);  //当响应了变焦，显示SeekBar区域
            timeSeek = System.currentTimeMillis(); //让线程控制layoutZoomSeek在显示几秒后消失

            if(motionEvent.getY() - y_move < 0){  //向上滑，变焦变大
                zoom = zoom + 1;
                if(zoom > maxZoom){
                    zoom = maxZoom;
                }
                mSurfaceView.startZoom(zoom);
            } else { //向下滑，变焦变小
                zoom = zoom - 1;
                if(zoom < 0){
                    zoom = 0;
                }
                mSurfaceView.startZoom(zoom);
            }
            Log.e("ww","222222:" + zoom+";"+maxZoom);
            seekBarZoom.setProgress(zoom);
            seekBarZoom.setThumbPosition((zoom*1.0f)/(maxZoom*1.0f)); //滑块位置
        }
    }

    private void doOnTouchUp(MotionEvent motionEvent) {

        if (Math.abs(x_down - x_up) < 30 && Math.abs(y_down - y_up) < 30) {  //x、y轴上均小于30，定义为点击，响应对焦
            if (canFocus) {

                if (mSurfaceView.startFocus(motionEvent.getX(), motionEvent.getY())) {

                    onFocusBegin(motionEvent.getX(), motionEvent.getY());
                    canFocus = false;
                }
            }
        }
    }
    public void onBackPressed(){//重写返回键调用

        OntOdvpProxy.getInstance().disConnect(null);
        finish();
    }

    public void changeCamera(View v) {
        if (!canFocus) {
            return;
        }
        if (mPublisher != null) {
            mPublisher.changeCamera();
        }
    }

    /**
     * 对焦开始的时候把对焦指示器显示出来
     */
    public void onFocusBegin(float x,float y) {
        ivFocus.setX(x-getResources().getDimension(R.dimen.px200)/2);  //px200是ivFocus在xml中的长宽
        ivFocus.setY(y-getResources().getDimension(R.dimen.px200)/2);
        ivFocus.setVisibility(View.VISIBLE);
    }
    /**
     * 对焦结束，隐藏
     */
    public void onFocusEnd() {
        ivFocus.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if (requestCode == PublishProxy.PERMISSIONS_REQUEST_CODE) {

            mPublisher.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK, true);
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
        if(mIsPublishing){
            mPublisher.stopPublish(true);
        }
        mPublisher.closeCamera();
        unregisterReceiver(mEventReceiver);
        flagShowThread = false;
        videoHandler = null;
        mTimerHandler = null;
        showThread = null;
        showRunnable = null;
        stopTimer();
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

        if (!mPublisher.startPublish(pushUrl, deviceId, true)) {
            Toast.makeText(PublishActivity.this, "启动失败", Toast.LENGTH_SHORT).show();
            return;
        }
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

        mPublisher.stopPublish(true);
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
                        if (mTimerHandler != null) {
                            mTimerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();
                        }
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

    private static class VideoHandler extends Handler {

        WeakReference<PublishActivity> mActivityReference;
        VideoHandler(PublishActivity presenter) {
            mActivityReference = new WeakReference<>(presenter);
        }

        @Override
        public void handleMessage(Message msg) {

            final PublishActivity presenter = mActivityReference.get();
            switch (msg.what) {

                case 1002:   //操作变焦SeekBar区域显示隐藏
                    presenter.getLayoutZoomSeek().setVisibility(View.GONE);
                    break;
            }
        }
    }

    /**用于专门控制界面上模块的显示隐藏*/
    private Runnable showRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                while (flagShowThread) {

                    //控制变焦SeekBar区域的
                    if(timeSeek > 0){
                        if (System.currentTimeMillis() - timeSeek > showTimeSeek) {
                            if(videoHandler != null){
                                videoHandler.sendEmptyMessage(1002);
                            }
                        }
                    }

                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            timeSeek = System.currentTimeMillis(); //让线程控制layoutZoomSeek在显示几秒后消失，重新赋值，让其重新计时

            zoom = progress;
            Log.e("ww","111111:" + zoom);
            mSurfaceView.startZoom(zoom);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    public LinearLayout getLayoutZoomSeek() {
        return layoutZoomSeek;
    }
}
