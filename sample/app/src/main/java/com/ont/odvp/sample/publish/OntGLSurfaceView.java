package com.ont.odvp.sample.publish;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.Surface;

import com.ont.odvp.sample.def.IGLViewEventListener;
import com.ont.media.odvp.utils.OntLog;
import com.ont.media.odvp.def.IEncodeDef;
import com.ont.media.odvp.model.Resolution;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.ont.odvp.sample.def.IPathDef.VOD_FILE_PATH;

/**
 * Created by betali on 2018/1/12.
 */

public class OntGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer  {

    private final int defaultHeight = 720;

    private GPUImageFilter magicFilter;
    private IntBuffer pictureCopy;
    private float mInputAspectRatio;
    private float mOutputAspectRatio;
    private float[] mProjectionMatrix = new float[16];
    private float[] mTransformMatrix = new float[16];
    private float[] mShotMatrix = new float[16];
    private float[] mShotTransformMatrix = new float[16];
    private ByteBuffer mGLPreviewBuffer;

    private volatile boolean mEnableRecord;
    private volatile boolean mEnableDrawFrame;
    private Object mCameraLock;

    private Activity mHostPage;
    private int mCameraId;
    private Camera mCamera;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private final float[] mSTMatrix = new float[16];

    private int mResolutionWidth;      // 分辨率尺寸，跟屏幕方向相关
    private int mResolutionHeight;
    private int mPreviewWidth;        // 相机预览尺寸，始终宽>高
    private int mPreviewHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mBitrate;

    private ArrayList<Resolution> mSupportPreviewsResolutions;

    private Thread mVideoEncodeThread;
    private ConcurrentLinkedQueue<IntBuffer> mVideoEncodeMsgCache;
    private final Object threadLock = new Object();
    private IGLViewEventListener mGLViewListener;
    private volatile boolean mVideoShot = false;
    private String mFileName;
    private String mFinalFileName;
    private boolean mRetSuccess = false;

    public OntGLSurfaceView(Context context) {
        this(context, null);
    }

    public OntGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        setRenderer(this);//渲染线程
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mVideoEncodeMsgCache = new ConcurrentLinkedQueue<>();
        mSupportPreviewsResolutions = new ArrayList<>();
        mCameraLock = new Object();
    }

    public void setGLViewEventListener(IGLViewEventListener glViewEventListener) {

        mGLViewListener = glViewEventListener;
    }

    public void changeFilename(){
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
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
                        IntBuffer picture = mVideoEncodeMsgCache.poll();
                        if(picture != null){
                            mGLPreviewBuffer.asIntBuffer().put(picture.array());
                            mGLViewListener.onGetVideoFrame(mGLPreviewBuffer.array(), mPreviewWidth, mPreviewHeight);
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

    public void openCamera(int cameraId) {

        enableDrawFrame(false);
        if(cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT && !getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {

            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        mCameraId = cameraId;
        mCamera = Camera.open(mCameraId);
        Resolution defaultResolution = getDefaultPreviewSize(false).resolution;
        setCameraParameters(defaultResolution.width, defaultResolution.height);

        mPreviewWidth = defaultResolution.width;
        mPreviewHeight = defaultResolution.height;
        mInputAspectRatio = mPreviewWidth > mPreviewHeight ? (float) mPreviewWidth / mPreviewHeight : (float) mPreviewHeight / mPreviewWidth;
        onAspectRatioChanged();
        updatePreviewResolution();

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        //安全地用于在UI线程和渲染线程之间进行交流通信
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mGLPreviewBuffer = ByteBuffer.allocate(mPreviewWidth * mPreviewHeight * 4);
                if (magicFilter != null) {
                    magicFilter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
                }
                enableDrawFrame(true);
            }
        });
    }

    public boolean changeCamera() {

        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {

            return false;
        }

        enableDrawFrame(false);
        releaseCamera();
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        mCamera = Camera.open(mCameraId);
        ResultPreviewSize ret = getDefaultPreviewSize(true);
        Resolution defaultResolution = ret.resolution;
        setCameraParameters(defaultResolution.width, defaultResolution.height);
        mGLViewListener.onChangeCameraConfirmResolution(ret.isResolutionChange);

        mPreviewWidth = defaultResolution.width;
        mPreviewHeight = defaultResolution.height;
        updatePreviewResolution();
        mInputAspectRatio = mPreviewWidth > mPreviewHeight ? (float) mPreviewWidth / mPreviewHeight : (float) mPreviewHeight / mPreviewWidth;
        onAspectRatioChanged();

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        //安全地用于在UI线程和渲染线程之间进行交流通信
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mGLPreviewBuffer = ByteBuffer.allocate(mPreviewWidth * mPreviewHeight * 4);
                if (magicFilter != null) {
                    magicFilter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
                }
                enableDrawFrame(true);
            }
        });
        return ret.isResolutionChange;
    }

    public void updateDisplayOrientation() {

        enableDrawFrame(false);
        releaseCamera();
        mCamera = Camera.open(mCameraId);
        setCameraParameters(mPreviewWidth, mPreviewHeight);
        updatePreviewResolution();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        enableDrawFrame(true);
    }

    public void setResolution(Resolution size) {

        enableDrawFrame(false);
        releaseCamera();
        mCamera = Camera.open(mCameraId);
        setCameraParameters(size.width, size.height);
        mPreviewWidth = size.width;
        mPreviewHeight = size.height;
        updatePreviewResolution();
        mInputAspectRatio = mPreviewWidth > mPreviewHeight ? (float) mPreviewWidth / mPreviewHeight : (float) mPreviewHeight / mPreviewWidth;
        onAspectRatioChanged();

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        queueEvent(new Runnable() {
            @Override
            public void run() {
                mGLPreviewBuffer = ByteBuffer.allocate(mPreviewWidth * mPreviewHeight * 4);
                if (magicFilter != null) {
                    magicFilter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
                }
                enableDrawFrame(true);
            }
        });
    }

    public void closePreview() {

        queueEvent(new Runnable() {
            @Override
            public void run() {
                releaseCamera();
                releaseTexture();
                releaseFilter();
            }
        });
        onPause();
    }

    public ArrayList<Resolution> getPreviewSizeList() {

        return mSupportPreviewsResolutions;
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

    //渲染初始化
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);

        magicFilter = new GPUImageFilter(MagicFilterType.NONE);
        magicFilter.init(getContext().getApplicationContext());
        magicFilter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);

        mTextureId = OpenGLUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();//turn to onDrawFrame
            }
        });

        // For camera preview on activity creation
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    //Surface大小被改变时被调用
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        GLES20.glViewport(0, 0, width, height);
        magicFilter.onDisplaySizeChanged(width, height);
        mOutputAspectRatio = width > height ? (float) width / height : (float) height / width;

        mSurfaceWidth = width;
        mSurfaceHeight = height;

        onAspectRatioChanged();
    }

    //真正进行渲染
    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();

        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        Matrix.multiplyMM(mTransformMatrix, 0, mSTMatrix, 0, mProjectionMatrix, 0);
        Matrix.multiplyMM(mShotTransformMatrix, 0, mSTMatrix, 0, mShotMatrix, 0);
        magicFilter.setOutputTransformMatrix(mTransformMatrix);
        magicFilter.setGLShotTransformMatrix(mShotTransformMatrix);
        magicFilter.onDrawFrame(mTextureId, mVideoShot);

        if (!mEnableDrawFrame) {

            return;
        }

        if (mEnableRecord) {
            if (mVideoShot) {
                pictureCopy = magicFilter.getGLShotBuffer().duplicate();
                mVideoEncodeMsgCache.add(magicFilter.getGLFboBuffer());
                new PicShotAsyncTask().execute(pictureCopy);

                mVideoShot = false;
                synchronized (threadLock) {
                    threadLock.notifyAll();
                }
            }
            else{
                mVideoEncodeMsgCache.add(magicFilter.getGLFboBuffer());
                synchronized (threadLock) {
                    threadLock.notifyAll();
                }
            }
        }
        if (mRetSuccess) {
            OntLog.i("TAG:PICTURE SENT", "picture was sent.");
            mRetSuccess = false;
        }
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

    public void setVideoShot(boolean videoShot){

        if (mEnableRecord) {

            mVideoShot = videoShot;
        }
    }

    private void releaseFilter() {

        if (magicFilter != null) {

            magicFilter.destroy();
            magicFilter = null;
        }
    }

    private void releaseTexture() {

        if (mSurfaceTexture != null) {

            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    private void releaseCamera() {

        try {
            if (mCamera != null) {

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
        int[] requestedFrameRate = new int[]{IEncodeDef.VIDEO_FRAME_RATE * 1000, IEncodeDef.VIDEO_FRAME_RATE * 1000};
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

    private ResultPreviewSize getDefaultPreviewSize(boolean isChange) {

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

        ResultPreviewSize ret = new ResultPreviewSize(null, true);
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
        } else */if (mPreviewHeight >= 480) {

            mBitrate = 1024 * 1024;  // 500KB
        } else if (mPreviewHeight >= 360) {

            mBitrate = 500 * 1024;
        } else if (mPreviewHeight >= 240) {

            mBitrate = 250 * 1024;
        } else {

            mBitrate = 120 * 1024;
        }
    }

    public int getCameraDisplayOrientation() {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = mHostPage.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
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

    public int[] findBestFrameRate(List<int[]> frameRateList, int[] requestedFrameRate) {
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

    private void onAspectRatioChanged() {

        float aspectRatio = 0.0f;
        if (Math.abs(mOutputAspectRatio - 0.0f) < 0.00000001 || Math.abs(mInputAspectRatio - 0.0f) < 0.00000001){
            return;
        }

        aspectRatio = mOutputAspectRatio / mInputAspectRatio;
        if (mSurfaceWidth > mSurfaceHeight) {
            Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -aspectRatio, aspectRatio, -1.0f, 1.0f);
        } else {
            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, -1.0f, 1.0f);
        }
        Matrix.orthoM(mShotMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
    }

    private static class ResultPreviewSize {

        public Resolution resolution;
        public boolean isResolutionChange;

        public ResultPreviewSize(Resolution resolution, boolean isResolutionChange) {
            this.resolution = resolution;
            this.isResolutionChange = isResolutionChange;
        }
    }

    private void enableDrawFrame(boolean enable) {

        synchronized (mCameraLock) {

            mEnableDrawFrame = enable;
        }
    }
}
