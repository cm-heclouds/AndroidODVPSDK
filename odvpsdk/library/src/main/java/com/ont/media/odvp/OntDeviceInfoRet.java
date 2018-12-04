package com.ont.media.odvp;

/**
 * Created by betali on 2018/8/14.
 */

public class OntDeviceInfoRet {

    public String deviceId;
    public byte[] authCode;

    public OntDeviceInfoRet(String deviceId, byte[] authCode) {
        this.deviceId = deviceId;
        this.authCode = authCode;
    }
}
