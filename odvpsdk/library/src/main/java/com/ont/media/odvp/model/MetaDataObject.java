package com.ont.media.odvp.model;

/**
 * Created by betali on 2019/2/18.
 */
public class MetaDataObject {

    public final int width;
    public final int height;
    public final boolean isEnableAudio;

    public MetaDataObject(int width, int height, boolean isEnableAudio) {
        this.width = width;
        this.height = height;
        this.isEnableAudio = isEnableAudio;
    }
}
