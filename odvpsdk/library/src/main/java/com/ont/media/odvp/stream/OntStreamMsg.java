package com.ont.media.odvp.stream;

/**
 * Created by betali on 2018/1/15.
 */

public class OntStreamMsg {

    public static final byte ON_SEND_METADATA = 0x04;
    public static final byte ON_SEND_VIDEO = 0x05;
    public static final byte ON_SEND_AUDIO = 0x06;


    public int what;
    public Object obj;

    public OntStreamMsg(int what) {
        this.what = what;
    }

    public OntStreamMsg(int what, Object obj) {
        this.what = what;
        this.obj = obj;
    }
}
