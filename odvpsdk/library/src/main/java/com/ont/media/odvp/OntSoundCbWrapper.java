package com.ont.media.odvp;

import java.lang.ref.WeakReference;

/**
 * Created by betali on 2018/8/15.
 */

public class OntSoundCbWrapper {

    public static void onSoundNotify(Object weakThis, int chunk, int ts, byte[] data, int len) {

        IOntSoundCb soundCb = ((WeakReference<IOntSoundCb>)weakThis).get();
        if (soundCb == null) {

            return;
        }

        soundCb.onSoundNotify(chunk, ts, data, len);
    }
}
