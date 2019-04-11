package com.ont.odvp.sample.publish;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ont.media.odvp.def.IEncodeDef;
import com.ont.media.odvp.model.PublishConfig;
import com.ont.media.odvp.model.Resolution;
import com.ont.media.odvp.utils.OntLog;
import com.ont.odvp.sample.def.IGLViewEventListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ont.odvp.sample.def.IPathDef.VOD_FILE_PATH;

public class OntSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    private final int defaultHeight = 720;

    private int mRotation = 0;
    private SurfaceHolder mSurfaceHolder;

    private volatile boolean mEnableRecord;

    private Activity mHostPage;
    private int mCameraId;
    private Camera mCamera;

    private int mResolutionWidth;      // 分辨率尺寸，跟屏幕方向相关
    private int mResolutionHeight;
    private int mPreviewWidth;        // 相机预览尺寸，始终宽>高
    private int mPreviewHeight;
    private int mBitrate;

    private ArrayList<Resolution> mSupportPreviewsResolutions;

    private Thread mVideoEncodeThread;
    private ConcurrentLinkedQueue<FrameInfo> mVideoEncodeMsgCache;
    private final Object threadLock = new Object();
    private IGLViewEventListener mGLViewListener;
    private volatile boolean mVideoShot = false;
    private String mFileName;
    private String mFinalFileName;
    private boolean mRetSuccess = false;
    private boolean mFlip = false;
    private boolean mPermission = false;
    private boolean mSurfaceCreate = false;
    private FrameInfo mFrameInfo;
    private FrameInfo mSendFrameInfo;
    private int mVideoFrameRate;

    private int maxZoom = -1; //变焦的最大zoom
    private Rect rectFocus;  //点中的对焦区域

    public OntSurfaceView(Context context) {
        this(context, null);
    }

    public OntSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mVideoEncodeMsgCache = new ConcurrentLinkedQueue<>();
        mSupportPreviewsResolutions = new ArrayList<>();
        mFrameInfo = new FrameInfo();
        mSendFrameInfo = new FrameInfo();
    }

    public void setPublishConfig(int videoFrameRate) {

        this.mVideoFrameRate = videoFrameRate;
    }

    public void setGLViewEventListener(IGLViewEventListener glViewEventListener) {

        mGLViewListener = glViewEventListener;
    }

    public void setVideoShot(boolean videoShot){

        if (mEnableRecord) {

            mVideoShot = videoShot;
        }
    }

    public void changeFilename(){
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        mFileName = format.format(date) + ".png";
        mFinalFileName = VOD_FILE_PATH + "/" + mFileName;
    }

    public void setHostPage(Activity hostPage) {
        mHostPage = hostPage;
    }

    public void enableRecord() {

        mVideoEncodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    while (!mVideoEncodeMsgCache.isEmpty()) {
                        mSendFrameInfo = mVideoEncodeMsgCache.poll();
                        if(mSendFrameInfo.getYuvFrame() != null){
                            mGLViewListener.onGetVideoFrame(mSendFrameInfo.getYuvFrame(), mSendFrameInfo.getMirror(), mSendFrameInfo.getRotation(), mPreviewWidth, mPreviewHeight);
                        }
                    }
                    // Waiting for next frame
                    synchronized (threadLock) {
                        try {
                            // isEmpty() may take some time, so we set timeout to detect next frame
                            threadLock.wait(500);
                        } catch (InterruptedException ie) {

                            //Log.e(TAG, "video get thread wait error, quit video get!");
                            mVideoEncodeThread.interrupt();
                        }
                    }
                }
                //Log.e(TAG, "video record quit!");
            }
        });
        mVideoEncodeThread.start();
        mEnableRecord = true;
    }

    public void disableRecord() {

        mEnableRecord = false;
        mVideoEncodeMsgCache.clear();

        if (mVideoEncodeThread != null) {
            mVideoEncodeThread.interrupt();
            try {

                mVideoEncodeThread.join();
            } catch (InterruptedException e) {

                mVideoEncodeThread.interrupt();
            }
            mVideoEncodeThread = null;
        }
    }

    private Camera.PreviewCallback getPreviewCallback() {
        return new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                if(data != null ){
                    if(mEnableRecord){
                        mFrameInfo.setRotation(mRotation);
                        mFrameInfo.setYuvFrame(data);
                        mFrameInfo.setMirror(mFlip);
                        mVideoEncodeMsgCache.add(mFrameInfo);
                        synchronized (threadLock) {
                            threadLock.notifyAll();
                        }
                    }
                    camera.addCallbackBuffer(data);
                }else {
                    camera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
                }
            }
        };
    }

    private void openCamera() {

        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT && !getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {

            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        mCamera = Camera.open(mCameraId);
        Resolution defaultResolution = getDefaultPreviewSize(false).resolution;
        setCameraParameters(defaultResolution.width, defaultResolution.height);

        mPreviewWidth = defaultResolution.width;
        mPreviewHeight = defaultResolution.height;
        updatePreviewResolution();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
        mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
        mCamera.startPreview();

        //mCamera初始化后，获取变焦最大Zoom
        Camera.Parameters parameters = mCamera.getParameters();
        if(!parameters.isZoomSupported()){ //不支持变焦
            maxZoom = -1;
        }
        maxZoom = parameters.getMaxZoom();

        if (mGLViewListener != null) {
            mGLViewListener.onCameraOpen();
        }
    }

    public boolean changeCamera() {

        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {

            return false;
        }
        releaseCamera();
        mGLViewListener.onCameraAutoFocusCallback(true);

        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            mFlip = true;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            mFlip = false;
        }
        mCamera = Camera.open(mCameraId);
        OntSurfaceView.ResultPreviewSize ret = getDefaultPreviewSize(true);
        Resolution defaultResolution = ret.resolution;
        setCameraParameters(defaultResolution.width, defaultResolution.height);
        mGLViewListener.onChangeCameraConfirmResolution(ret.isResolutionChange);

        mPreviewWidth = defaultResolution.width;
        mPreviewHeight = defaultResolution.height;
        updatePreviewResolution();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
        mCamera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
        mCamera.startPreview();

        return ret.isResolutionChange;
    }

    public void updateDisplayOrientation() {//mPreviewWidth, mPreviewHeight不变

        releaseCamera();
        mGLViewListener.onCameraAutoFocusCallback(true);
        mCamera = Camera.open(mCameraId);
        setCameraParameters(mPreviewWidth, mPreviewHeight);
        updatePreviewResolution();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
        mCamera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
        mCamera.startPreview();
    }

    public void setResolution(Resolution size) {

        releaseCamera();
        mGLViewListener.onCameraAutoFocusCallback(true);
        mCamera = Camera.open(mCameraId);
        setCameraParameters(size.width, size.height);
        mPreviewWidth = size.width;
        mPreviewHeight = size.height;
        updatePreviewResolution();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
        mCamera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
        mCamera.startPreview();
    }

    public void closePreview() {

        releaseCamera();
    }

    public ArrayList<Resolution> getPreviewSizeList() {

        return mSupportPreviewsResolutions;
    }

    /**
     * 开始变焦
     * 摄像头的变焦区间是一个数组，数组长度为getZoomRatios()，第一个元素为100，依次变大，变焦的实际值为元素值除以100.
     * 入参i为数组下标，为0到getMaxZoom()。getMaxZoom() = getZoomRatios() - 1;
     * 实际操作的为数组下标
     */
    public void startZoom(int i){
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if(!parameters.isZoomSupported()){ //不支持变焦
            //ToastUtil.showToast("不支持变焦");
            return;
        }
        parameters.setZoom(i);
        mCamera.setParameters(parameters);  //这里一定要重新设置parameters给mCamera
    }

    public int getMaxZoom(){
        return maxZoom;
    }


    /**
     * 开始对焦
     */
    public boolean startFocus(float x,float y){

        if (mCamera == null || mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {

            return false;
        }

        rectFocus = calculateTapArea(x, y);
        List<Camera.Area> mFocusList = new ArrayList<>();
        mFocusList.add(new Camera.Area(rectFocus, 1000));
        List<Camera.Area> mMeteringList = new ArrayList<>();
        mMeteringList.add(new Camera.Area(rectFocus, 1000));

        Camera.Parameters mParams = mCamera.getParameters();
        mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        if (mParams.getMaxNumFocusAreas() > 0) {
            mParams.setFocusAreas(mFocusList);     //设置对焦区域
        }
        if (mParams.getMaxNumMeteringAreas() > 0) {
            mParams.setMeteringAreas(mMeteringList);   //设置测光区域
        }

        mCamera.setParameters(mParams);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(mGLViewListener != null){
                    mGLViewListener.onCameraAutoFocusCallback(success);
                }
            }
        });
        return true;
    }

    /**
     * 把手指触摸坐标转化为对焦坐标，在手机横竖屏放置切换时，会产生4种情况，如果每种情况都是左边为Rect的left，上边为top，右边为right，下边为bottom，会有对应4种不同对焦坐标系
     * 手机标准竖屏：左上角为（-1000，1000），右上角（-1000，-1000），右下角（1000，-1000），左下角（1000，1000）
     * 手机标准横屏：左上角为（-1000，-1000），右上角（1000，-1000），右下角（1000，1000），左下角（-1000，1000）
     * 手机反向竖屏：左上角为（1000，-1000），右上角（1000，1000），右下角（-1000，1000），左下角（-1000，-1000）
     * 手机反向横屏：左上角为（1000，1000），右上角（-1000，1000），右下角（-1000，-1000），左下角（1000，-1000）
     * 参考:https://www.cnblogs.com/panxiaochun/p/5802814.html
     */
    private Rect calculateTapArea(float x, float y) {
        int areaSize = 300;   //对焦区域的相对于Camera坐标系边长

        int left = 0;
        int top = 0;
        int rotation = mHostPage.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                left = clamp(Float.valueOf((y / UIUtils.getScreenHeight(getContext())) * 2000 - 1000).intValue(), areaSize);
                top = clamp(Float.valueOf(((UIUtils.getScreenWidth(getContext()) - x) / UIUtils.getScreenWidth(getContext())) * 2000 - 1000).intValue(), areaSize);
                break;
            case Surface.ROTATION_90:
                left = clamp(Float.valueOf((x / UIUtils.getScreenWidth(getContext())) * 2000 - 1000).intValue(), areaSize);
                top = clamp(Float.valueOf((y / UIUtils.getScreenHeight(getContext())) * 2000 - 1000).intValue(), areaSize);
                break;
            case Surface.ROTATION_180:
                left = clamp(Float.valueOf(1000 - (y / UIUtils.getScreenHeight(getContext())) * 2000).intValue(), areaSize);
                top = clamp(Float.valueOf(1000 - ((UIUtils.getScreenWidth(getContext()) - x) / UIUtils.getScreenWidth(getContext())) * 2000).intValue(), areaSize);
                break;
            case Surface.ROTATION_270:
                left = clamp(Float.valueOf(1000 - (x / UIUtils.getScreenWidth(getContext()) * 2000)).intValue(), areaSize);
                top = clamp(Float.valueOf(1000 - (y / UIUtils.getScreenHeight(getContext()) * 2000)).intValue(), areaSize);
                break;
        }

        return new Rect(left, top, left + areaSize, top + areaSize);
    }

    /**
     * 确保所选Camera区域不会超过手机边界外
     * touchCoordinateInCamera:相对于Camera坐标系触摸值
     * focusAreaSize:相对于Camera坐标系区域边长
     */
    private int clamp(int touchCoordinateInCamera, int focusAreaSize) {
        int result;
        if(touchCoordinateInCamera > 0){
            if(touchCoordinateInCamera + focusAreaSize/2 > 1000){
                result = 1000 - focusAreaSize ;
            } else {
                result = touchCoordinateInCamera - focusAreaSize/2;
            }
        } else {
            if(touchCoordinateInCamera - focusAreaSize/2 < -1000){
                result = -1000;
            } else {
                result = touchCoordinateInCamera - focusAreaSize/2;
            }
        }
        return result;
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public int getResolutionWidth() {
        return mResolutionWidth;
    }

    public int getResolutionHeight() {
        return mResolutionHeight;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public void openCamera(int id){

        mCameraId = id;
        if (mSurfaceCreate) {
            openCamera();
        } else {
            mPermission = true;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (mPermission) {
            openCamera();
        } else {
            mSurfaceCreate = true;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private Bitmap invertImage(int[] pixels, int width, int height){

        int[] finalArray = new int[width * height];

        for(int i = 0; i < finalArray.length; i++) {
            int red = Color.red(pixels[i]);
            int green = Color.green(pixels[i]);
            int blue = Color.blue(pixels[i]);
            finalArray[i] = Color.rgb(blue, green, red);
        }
        Bitmap bitmap = Bitmap.createBitmap(finalArray, width, height, Bitmap.Config.ARGB_8888);

        return bitmap;
    }

    public class PicShotAsyncTask extends AsyncTask<IntBuffer, Void, Boolean> {

        protected void onPreExecute() {
            OntLog.i("AsyncTask", "AsyncTask will start.");
        }

        protected Boolean doInBackground(IntBuffer... params) {

            Boolean asyncRet = false;
            IntBuffer PixelBuffer = (IntBuffer) params[0];

            PixelBuffer.position(0);//这里要把读写位置重置下
            int pix[] = new int[mResolutionWidth * mResolutionHeight];
            PixelBuffer.get(pix);//这是将intbuffer中的数据赋值到pix数组中

            Bitmap bmp = invertImage(pix, mResolutionWidth, mResolutionHeight);//700毫秒延迟

            android.graphics.Matrix m = new android.graphics.Matrix();
            //翻转是90° 下面三个选其中之一 或者根据需求
            //m.postScale(-(sx * sx), (sy * sy));
            m.postScale(-1, 1);   //镜像水平翻转
            m.postRotate(-180);  //旋转-180度

            Bitmap bmp2 = Bitmap.createBitmap(bmp, 0, 0, mResolutionWidth, mResolutionHeight, m, true);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mFinalFileName);//注意app的sdcard读写权限问题
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            bmp2.compress(Bitmap.CompressFormat.PNG, 100, fos);//压缩成png,100%显示效果
            try {
                fos.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            /*if (OntOdvp.nativeDeviceUploadPicture(OntOdvpProxy.getInstance().getDevice(), mFinalFileName, mFileName, 1) == 0) {
                asyncRet = true;
            }*/

            return asyncRet;
        }

        protected void onPostExecute(Boolean asyncRet) {

            mRetSuccess = asyncRet;
        }
    }

    private void releaseCamera() {

        try {
            if (mCamera != null) {
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setCameraParameters(int previewWidth, int previewHeight) {

        if (mCamera == null) {
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        int[] requestedFrameRate = new int[]{mVideoFrameRate * 1000, mVideoFrameRate * 1000};
        int[] bestFps = findBestFrameRate(parameters.getSupportedPreviewFpsRange(), requestedFrameRate);
        parameters.setPreviewFpsRange(bestFps[0], bestFps[1]);

        parameters.setPreviewSize(previewWidth, previewHeight);
        parameters.setRecordingHint(true);

        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(getCameraDisplayOrientation());
    }

    private OntSurfaceView.ResultPreviewSize getDefaultPreviewSize(boolean isChange) {

        if (mCamera == null) {

            return null;
        }
        mSupportPreviewsResolutions.clear();

        List<Camera.Size> previewSizeList = mCamera.getParameters().getSupportedVideoSizes();
        if (previewSizeList == null) {

            previewSizeList = mCamera.getParameters().getSupportedPreviewSizes();
        }
        Collections.sort(previewSizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.height == rhs.height) {
                    return lhs.width == rhs.width ? 0 : (lhs.width > rhs.width ? 1 : -1);
                } else if (lhs.height > rhs.height) {
                    return 1;
                }
                return -1;
            }
        });

        OntSurfaceView.ResultPreviewSize ret = new OntSurfaceView.ResultPreviewSize(null, true);
        int diff = Integer.MAX_VALUE;
        for (int i = 0; i < previewSizeList.size(); i++) {

            Camera.Size size = previewSizeList.get(i);
            if ((size.width % 16 == 0) && (size.height % 16 == 0) && size.width >= 480 && size.width <= 1280) {

                Resolution currentSize = new Resolution(size.width, size.height);
                mSupportPreviewsResolutions.add(0, currentSize);

                int currentDiff = 0;
                if(!isChange || currentSize.height != mPreviewHeight || currentSize.width != mPreviewWidth) {

                    currentDiff = Math.abs(currentSize.height - defaultHeight);
                } else {

                    ret.isResolutionChange = false;
                }

                if (currentDiff < diff) {

                    diff = currentDiff;
                    ret.resolution = currentSize;
                }
            }
        }

        mGLViewListener.onGetCameraSupportResolutions();
        return ret;
    }

    private void updatePreviewResolution()
    {
        int rotation = mHostPage.getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {

            mResolutionWidth = mPreviewHeight;
            mResolutionHeight = mPreviewWidth;
        }
        else {
            mResolutionWidth = mPreviewWidth;
            mResolutionHeight = mPreviewHeight;
        }

        /*if (mPreviewHeight >= 720) {

            mBitrate = 1024 * 1024;  // 1024KB
        } else */
        if (mPreviewHeight >= 480) {

            mBitrate = 1024 * 1024;  // 500KB
        } else if (mPreviewHeight >= 360) {

            mBitrate = 500 * 1024;
        } else if (mPreviewHeight >= 240) {

            mBitrate = 250 * 1024;
        } else {

            mBitrate = 120 * 1024;
        }
    }

    private int getCameraDisplayOrientation() {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = mHostPage.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                    mRotation = 90;
                } else {
                    mRotation = 270;
                }
                break;
            case Surface.ROTATION_90: degrees = 90; mRotation = 0; break;
            case Surface.ROTATION_180: degrees = 180; mRotation = 0; break;
            case Surface.ROTATION_270: degrees = 270; mRotation = 180; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private int[] findBestFrameRate(List<int[]> frameRateList, int[] requestedFrameRate) {
        int[] bestRate = frameRateList.get(0);
        int requestedAverage = (requestedFrameRate[0] + requestedFrameRate[1]) / 2;
        int bestRateAverage = (bestRate[0] + bestRate[1]) / 2;

        int size = frameRateList.size();
        for (int i=1; i < size; i++) {
            int[] rate = frameRateList.get(i);

            int rateAverage = (rate[0] + rate[1]) / 2;

            if (Math.abs(requestedAverage - bestRateAverage) >= Math.abs(requestedAverage - rateAverage)) {

                if ((Math.abs(requestedFrameRate[0] - rate[0]) <=
                        Math.abs(requestedFrameRate[0] - bestRate[0])) ||
                        (Math.abs(requestedFrameRate[1] - rate[1]) <=
                                Math.abs(requestedFrameRate[1] - bestRate[1]))) {
                    bestRate = rate;
                    bestRateAverage = rateAverage;
                }
            }
        }

        return bestRate;
    }

    private static class ResultPreviewSize {

        public Resolution resolution;
        public boolean isResolutionChange;

        public ResultPreviewSize(Resolution resolution, boolean isResolutionChange) {
            this.resolution = resolution;
            this.isResolutionChange = isResolutionChange;
        }
    }

    private int calculateFrameSize(int format) {
        return mPreviewWidth * mPreviewHeight * ImageFormat.getBitsPerPixel(format) / 8;
    }

    private class FrameInfo{
        int rotation;
        byte[] yuvframe;
        boolean mirror;

        public void setRotation(int rotation){
            this.rotation = rotation;
        }

        public void setYuvFrame(byte[] yuvframe){
            this.yuvframe = yuvframe;
        }

        public void setMirror(boolean mirror){
            this.mirror = mirror;
        }

        public int getRotation(){
            return rotation;
        }

        public byte[] getYuvFrame(){
            return yuvframe;
        }

        public boolean getMirror(){
            return mirror;
        }
    }
}
