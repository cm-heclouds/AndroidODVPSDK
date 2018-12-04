package com.ont.odvp.sample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ont.media.odvp.OntDeviceInfoRet;
import com.ont.media.odvp.OntOnvif;
import com.ont.media.odvp.OntRtmp;
import com.ont.media.odvp.model.VodBaseInfo;
import com.ont.media.odvp.model.VodInfo;
import com.ont.odvp.sample.def.ICallback;
import com.ont.odvp.sample.def.ICmdListener;
import com.ont.odvp.sample.def.IPathDef;
import com.ont.odvp.sample.device.DeviceInfo;
import com.ont.odvp.sample.device.FileUtils;
import com.ont.odvp.sample.device.OntOdvpProxy;
import com.ont.odvp.sample.publish.PublishActivity;

import java.util.ArrayList;
import java.util.List;

import static com.ont.odvp.sample.device.FileUtils.getMediaInfoViaRetriver;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    public static final int PUBLISH_REQUEST_CODE = 101;

    private boolean mLoading;
    private EditText regCodeEt;
    private EditText authInfoEt;
    private TextView mTextPush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        bindEditWatcher();

        if (!get()) {

            regCodeEt.setText("xxxxxxxxxxxxxxx");
            authInfoEt.setText("xxxxx");
        }

        OntOdvpProxy.getInstance().init(getString("odvp_device_info", "server_device_id"), getByteArray("odvp_device_info", "server_auth_code"));
        OntOdvpProxy.getInstance().setHostThreadHandler(new Handler());
    }

    private void initViews() {

        regCodeEt = findViewById(R.id.reg_code);
        authInfoEt = findViewById(R.id.auth_info);

        mTextPush = findViewById(R.id.text_push);
        mTextPush.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                changePushStatus(STATUS_LOADING);
                save();

                int ret = 0;
                ret = OntOdvpProxy.getInstance().connect(new DeviceInfo(getRegCode(), getAuthInfo()), cmdListener, new ICallback() {
                    @Override
                    public void onCallback(int ret, Object... parms) {

                        if (parms.length > 0) {

                            OntDeviceInfoRet infoRet = (OntDeviceInfoRet) parms[0];
                            saveString("odvp_device_info", "server_device_id", infoRet.deviceId);
                            saveByteArray("odvp_device_info", "server_auth_code", infoRet.authCode);
                        } else {
                            if (ret == 0) {

                                Intent intent = new Intent(MainActivity.this, PublishActivity.class);
                                startActivityForResult(intent, PUBLISH_REQUEST_CODE);
                            } else {

                                changePushStatus(STATUS_LOGIN);
                                showToast("connect " + ret);
                            }
                        }
                    }
                });
                if (ret == OntOdvpProxy.ERROR_ALREADY_CONNECT) {

                    changePushStatus(STATUS_LOGIN);
                    showToast("connect " + "已在连接状态");
                }
            }
        });
    }

    ICmdListener cmdListener = new ICmdListener() {

        @Override
        public int videoLiveStreamCtrl(long deviceReference, int channel, int level) {

            if (channel == 1) {

                return 0;
            } else {

                return OntOnvif.nativeLiveStreamCtrl(deviceReference, channel, level);//onvif设备
            }
        }

        @Override
        public int videoLiveStreamStart(long deviceReference, int channel, byte proType, int min, String pushUrl) {

            if (1 == channel) {
                return 0;
            } else {
                return OntOnvif.nativeLiveStreamPlay(deviceReference, channel, proType, min, pushUrl);//onvif设备
            }
        }

        @Override
        public int videoVodStreamStart(long deviceReference, String location, int channel, byte proType, String playFlag, String pushUrl, int ttl) {

            return OntRtmp.nativeVodStreamPlay(deviceReference, location, channel, proType, playFlag, pushUrl, ttl);
        }

        @Override
        public int videoDevPtzCtrl(long deviceReference, int channel, int mode, int ptzCmd, int speed) {

            if (channel == 1) {

                return 0;
            } else {

                return OntOnvif.nativeDevPtzCtrl(deviceReference, channel, mode, ptzCmd, speed);//onvif设备
            }
        }

        @Override
        public QueryFilesRet videoDevQueryFiles(int channel, int startIndex, int max, String startTime, String endTime) {
            return videoDevQueryFilesImpl(channel, startIndex, max, startTime, endTime);
        }

        @Override
        public void onDisconnect() {

            Intent intent = new Intent();
            intent.setAction("com.ont.odvp.event.1001");
            sendBroadcast(intent);
            showToast(getResources().getString(R.string.on_disconnect));
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PUBLISH_REQUEST_CODE) {
            changePushStatus(STATUS_LOGIN);
        }
    }

    public ICmdListener.QueryFilesRet videoDevQueryFilesImpl(int channelId, int page, int perPage, String startTime, String endTime) {

        List<VodInfo> totalVodInfos = new ArrayList<>();
        FileUtils.MediaInfo info = getMediaInfoViaRetriver(IPathDef.DEVICE_VOD);
        if (info != null) {

            VodBaseInfo baseInfo = info.toVodInfo(channelId);
            totalVodInfos.add(new VodInfo(baseInfo, IPathDef.DEVICE_VOD));
        }
        return new ICmdListener.QueryFilesRet(totalVodInfos, totalVodInfos.size(), totalVodInfos.size(), 1);
    }

    public String getRegCode() {

        return regCodeEt.getText().toString();
    }

    public String getAuthInfo() {

        return authInfoEt.getText().toString();
    }

    public void save() {

        SharedPreferences sp = getSharedPreferences("odvp_device_info", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("reg_code", getRegCode());
        edit.putString("auth_info", getAuthInfo());
        edit.commit();
    }

    public void saveString(String index, String name, String value) {

        SharedPreferences sp = getSharedPreferences(index, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();

        edit.putString(name, value);
        edit.commit();
    }

    public void saveByteArray(String index, String name, byte[] value_) {

        SharedPreferences sp = getSharedPreferences(index, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();

        String value = new String(Base64.encode(value_,Base64.DEFAULT));
        edit.putString(name, value);
        edit.commit();
    }

    private boolean get() {

        String regCode = "";
        String authInfo = "";

        SharedPreferences sp = getSharedPreferences("odvp_device_info", Context.MODE_PRIVATE);
        if (sp != null) {

            regCode = sp.getString("reg_code", "");
            authInfo = sp.getString("auth_info", "");

            if (TextUtils.isEmpty(regCode) || TextUtils.isEmpty(authInfo)) {

                return false;
            }

            regCodeEt.setText(regCode);
            authInfoEt.setText(authInfo);
            return true;
        } else {

            return false;
        }
    }

    private void bindEditWatcher() {

        regCodeEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (!mLoading) {
                    changePushStatus(STATUS_LOGIN);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        authInfoEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (!mLoading) {
                    changePushStatus(STATUS_LOGIN);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private String getString(String index, String name) {

        String value = "";
        SharedPreferences sp = getSharedPreferences(index, Context.MODE_PRIVATE);

        if (sp != null) {

            value = sp.getString(name, "");
            if (TextUtils.isEmpty(value)) {

                return null;
            }
            return value;
        } else {

            return null;
        }
    }

    private byte[] getByteArray(String index, String name) {

        byte[] value_ = null;
        SharedPreferences sp = getSharedPreferences(index, Context.MODE_PRIVATE);

        if (sp != null) {

            String value = "";
            value = sp.getString(name, "");
            if (TextUtils.isEmpty(value)) {

                return null;
            }

            value_ = Base64.decode(value.getBytes(), Base64.DEFAULT);
            return value_;
        } else {

            return null;
        }
    }

    private final int STATUS_LOGIN = 1;
    private final int STATUS_LOADING = 2;
    private void changePushStatus(int status) {

        switch (status) {

            case STATUS_LOGIN:
                if (TextUtils.isEmpty(getRegCode()) || TextUtils.isEmpty(getAuthInfo())) {

                    mTextPush.setEnabled(false);
                    mTextPush.setTextColor(getResources().getColor(R.color.colorText1));
                    mTextPush.setBackgroundResource(R.drawable.btn_background_0);
                } else {

                    mTextPush.setEnabled(true);
                    mTextPush.setTextColor(getResources().getColor(android.R.color.white));
                    mTextPush.setBackgroundResource(R.drawable.btn_background_2);
                }
                mLoading = false;
                mTextPush.setText(R.string.login);
                break;

            case STATUS_LOADING:
                mLoading = true;
                mTextPush.setEnabled(false);
                mTextPush.setTextColor(getResources().getColor(R.color.colorText1));
                mTextPush.setBackgroundResource(R.drawable.btn_background_0);
                mTextPush.setText(R.string.loading);
                break;

            default:
                break;
        }
    }
}
