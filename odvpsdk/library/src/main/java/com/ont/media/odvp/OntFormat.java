package com.ont.media.odvp;

/**
 * Created by betali on 2018/8/14.
 */

public class OntFormat {

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("ont_odvp");
    }

    public static native byte[] RGBAToI420(byte[] frame, int width, int height, boolean flip, int rotate);
    public static native byte[] RGBAToNV12(byte[] frame, int width, int height, boolean flip, int rotate);
    private native byte[] ARGBToI420Scaled(int[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    private native byte[] ARGBToNV12Scaled(int[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    private native byte[] ARGBToI420(int[] frame, int width, int height, boolean flip, int rotate);
    private native byte[] ARGBToNV12(int[] frame, int width, int height, boolean flip, int rotate);
    private native byte[] NV21ToNV12Scaled(byte[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    private native byte[] NV21ToI420Scaled(byte[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    public static native void setEncoderResolution(int out_width, int out_height);
}
