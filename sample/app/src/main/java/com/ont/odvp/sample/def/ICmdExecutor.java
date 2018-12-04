package com.ont.odvp.sample.def;

/**
 * Created by betali on 2018/2/2.
 */

public abstract class ICmdExecutor {

    public interface TaskType {

        int TYPE_CONNECT = 1;
        int TYPE_DISCONNECT = 2;
        int TYPE_GET_CHANNELS = 3;
        int TYPE_DELETE_CHANNEL = 4;
        int TYPE_UPDATE_DATA = 5;
        int TYPE_KEEP_ALIVE = 6;
        int TYPE_REQUEST_PUSH = 7;
    }

    public abstract void stopLoop();
    public abstract void startLoop();
}
