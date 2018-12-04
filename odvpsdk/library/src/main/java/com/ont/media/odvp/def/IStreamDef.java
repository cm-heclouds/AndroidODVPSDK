package com.ont.media.odvp.def;

/**
 * Created by betali on 2018/1/17.
 */

public interface IStreamDef {

    byte ON_SOUND_MSG_START = 0x01;
    byte ON_SOUND_MSG_STOP = 0x02;
    // 网络连接
    byte ON_PUBLISH_STREAM_DISCONNECT = 0x04;
}
