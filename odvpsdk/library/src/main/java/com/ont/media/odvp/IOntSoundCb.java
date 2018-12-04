package com.ont.media.odvp;

/**
 * Created by betali on 2018/8/15.
 */

public interface IOntSoundCb {

    void onSoundNotify(int chunk, int ts, byte[] data, int len);
}
