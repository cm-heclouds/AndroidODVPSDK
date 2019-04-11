package com.ont.odvp.sample.device;

/**
 * Created by betali on 2018/1/31.
 */

public class DeviceInfo {

    public String regCode;
    public String authInfo;
    public String ip;
    public String port;

    public DeviceInfo() {
    }

    public DeviceInfo(String regCode, String authInfo, String ip, String port) {

        this.regCode = regCode;
        this.authInfo = authInfo;
        this.ip = ip;
        this.port = port;
    }
}
