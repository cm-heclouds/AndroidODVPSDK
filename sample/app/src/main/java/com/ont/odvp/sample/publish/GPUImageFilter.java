/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ont.odvp.sample.publish;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.ont.odvp.sample.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

public class GPUImageFilter {

    private boolean mIsInitialized;
    private Context mContext;
    private MagicFilterType mType = MagicFilterType.NONE;
    private final LinkedList<Runnable> mRunOnDraw;
    private final int mVertexShaderId;
    private final int mFragmentShaderId;

    private int mGLProgId;
    private int mGLPositionIndex;
    private int mGLInputImageTextureIndex;
    private int mGLTextureCoordinateIndex;
    private int mGLTextureTransformIndex;

    protected int mInputWidth;
    protected int mInputHeight;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;

    private int[] mGLCubeId;
    private int[] mGLTextureCoordinateId;
    private float[] mGLOutputTransformMatrix;
    private float[] mGLShotTransformMatrix;

    private int[] mGLFboId;
    private int[] mGLFboShotId;
    private int[] mGLFboTexId;
    private int[] mGLShotFboTexId;
    private IntBuffer mGLFboBuffer;
    private IntBuffer mGLShotBuffer;

    public GPUImageFilter() {
        this(MagicFilterType.NONE);
    }

    public GPUImageFilter(MagicFilterType type) {
        this(type, R.raw.vertex, R.raw.fragment);
    }

    public GPUImageFilter(MagicFilterType type, int fragmentShaderId) {
        this(type, R.raw.vertex, fragmentShaderId);
    }

    public GPUImageFilter(MagicFilterType type, int vertexShaderId, int fragmentShaderId) {
        mType = type;
        mRunOnDraw = new LinkedList<>();
        mVertexShaderId = vertexShaderId;
        mFragmentShaderId = fragmentShaderId;
    }

    public void init(Context context) {
        mContext = context;
        onInit();
        onInitialized();
    }

    protected void onInit() {
        initVbo();
        loadSamplerShader();
    }

    protected void onInitialized() {

        mIsInitialized = true;
    }

    public final void destroy() {

        mIsInitialized = false;
        destroyFboTexture();
        destroyVbo();
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    protected void onDestroy() {

    }

    public void onInputSizeChanged(final int width, final int height) {

        initFboTexture(width, height);
        mInputWidth = width;
        mInputHeight = height;
    }

    public void onDisplaySizeChanged(final int width, final int height) {

        mOutputWidth = width;
        mOutputHeight = height;
    }

    private void loadSamplerShader() {

        mGLProgId = OpenGLUtils.loadProgram(OpenGLUtils.readShaderFromRawResource(getContext(), mVertexShaderId),
            OpenGLUtils.readShaderFromRawResource(getContext(), mFragmentShaderId));
        mGLPositionIndex = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLTextureCoordinateIndex = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
        mGLTextureTransformIndex = GLES20.glGetUniformLocation(mGLProgId, "textureTransform");
        mGLInputImageTextureIndex = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
    }

    private void initVbo() {

        final float VEX_CUBE[] = {

            -1.0f, -1.0f, // Bottom left.
            1.0f, -1.0f, // Bottom right.
            -1.0f, 1.0f, // Top left.
            1.0f, 1.0f, // Top right.
        };

        final float TEX_COORD[] = {

            0.0f, 0.0f, // Bottom left.
            1.0f, 0.0f, // Bottom right.
            0.0f, 1.0f, // Top left.
            1.0f, 1.0f // Top right.
        };

        mGLCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(VEX_CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(TEX_COORD).position(0);

        mGLCubeId = new int[1];
        mGLTextureCoordinateId = new int[1];

        GLES20.glGenBuffers(1, mGLCubeId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mGLCubeBuffer.capacity() * 4, mGLCubeBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glGenBuffers(1, mGLTextureCoordinateId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mGLTextureBuffer.capacity() * 4, mGLTextureBuffer, GLES20.GL_STATIC_DRAW);
    }

    private void destroyVbo() {

        if (mGLCubeId != null) {
            GLES20.glDeleteBuffers(1, mGLCubeId, 0);
            mGLCubeId = null;
        }
        if (mGLTextureCoordinateId != null) {
            GLES20.glDeleteBuffers(1, mGLTextureCoordinateId, 0);
            mGLTextureCoordinateId = null;
        }
    }

    private void initFboTexture(int width, int height) {
        if (mGLFboId != null && (mInputWidth != width || mInputHeight != height)) {
            destroyFboTexture();
        }

        if (width == 0 || height == 0) {
            return;
        }

        mGLFboId = new int[1];
        mGLFboShotId = new int[1];
        mGLFboTexId = new int[1];
        mGLShotFboTexId = new int[1];
        mGLFboBuffer = IntBuffer.allocate(width * height);
        mGLShotBuffer = IntBuffer.allocate(width * height);

        GLES20.glGenFramebuffers(1, mGLFboId, 0);
        GLES20.glGenFramebuffers(1, mGLFboShotId, 0);
        GLES20.glGenTextures(1, mGLFboTexId, 0);
        GLES20.glGenTextures(1, mGLShotFboTexId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGLFboTexId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mGLFboTexId[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGLShotFboTexId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 480, 640, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboShotId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mGLShotFboTexId[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void destroyFboTexture() {
        if (mGLFboTexId != null) {
            GLES20.glDeleteTextures(1, mGLFboTexId, 0);
            mGLFboTexId = null;
        }
        if (mGLShotFboTexId != null) {
            GLES20.glDeleteTextures(1, mGLShotFboTexId, 0);
            mGLShotFboTexId = null;
        }
        if (mGLFboId != null) {
            GLES20.glDeleteFramebuffers(1, mGLFboId, 0);
            mGLFboId = null;
        }
        if (mGLFboShotId != null) {
            GLES20.glDeleteFramebuffers(1, mGLFboShotId, 0);
            mGLFboShotId = null;
        }
    }

    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer, final FloatBuffer textureBuffer) {
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();

        GLES20.glEnableVertexAttribArray(mGLPositionIndex);
        GLES20.glVertexAttribPointer(mGLPositionIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, cubeBuffer);

        GLES20.glEnableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glVertexAttribPointer(mGLTextureCoordinateIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, textureBuffer);

        if (textureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLInputImageTextureIndex, 0);
        }

        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        onDrawArraysAfter();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisableVertexAttribArray(mGLPositionIndex);
        GLES20.glDisableVertexAttribArray(mGLTextureCoordinateIndex);

        return OpenGLUtils.ON_DRAWN;
    }

    public int onDrawFrame(int cameraTextureId, boolean videoShot) {
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        if (mGLFboId == null) {
            return OpenGLUtils.NO_TEXTURE;
        }

        if (mGLFboShotId == null) {
            return OpenGLUtils.NO_TEXTURE;
        }

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);//将第二个参数设置为想操作内存的标识
        GLES20.glEnableVertexAttribArray(mGLPositionIndex);//允许顶点着色器读取GPU（服务器端）数据,参数就是着色器中的要获取顶点属性各元素的变量
        GLES20.glVertexAttribPointer(mGLPositionIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);//指定了渲染时索引值为 index 的顶点属性及数组的数据格式和位置

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        GLES20.glEnableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glVertexAttribPointer(mGLTextureCoordinateIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);//定义顶点属性数组

        GLES20.glUniformMatrix4fv(mGLTextureTransformIndex, 1, false, mGLOutputTransformMatrix, 0);//下标位置，矩阵数量，是否进行转置，矩阵
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//选择当前活跃的纹理单元
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);//将后者（文理）绑定到前者上，后者纹理的名称在当前的应用中不能被再次使用
        GLES20.glUniform1i(mGLInputImageTextureIndex, 0);//uniform采样器对应着正确的纹理单元，mGLInputImageTextureIndex对应纹理第一层

        onDrawArraysPre();//onDrawArrays前的最后一波操作

        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);//手机显示的分辨率//X，Y指定了视口的左下角（在第一象限内，以（0，0）为原点的）位置；width，height表示这个视口矩形的宽度和高度，根据窗口的实时变化重绘窗口
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);//一个数据数组中提取数据渲染基本图元;GL_TRIANGLE_STRIP - OpenGL的使用将最开始的两个顶点出发，然后遍历每个顶点，这些顶点将使用前2个顶点一起组成一个三角形。

        GLES20.glUniformMatrix4fv(mGLTextureTransformIndex, 1, false, mGLShotTransformMatrix, 0);
        GLES20.glViewport(0, 0, mInputWidth, mInputHeight);//摄像头取出的分辨率
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);//把framebuffer object绑定到target
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLFboBuffer);//把已经绘制好的像素（它可能已经被保存到显卡的显存中）读取到内存,堡村子啊mGLFboBuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);//清空target

        if(videoShot){
            GLES20.glViewport(0, 0, mInputHeight, mInputWidth);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboShotId[0]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glReadPixels(0, 0, mInputHeight, mInputWidth, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLShotBuffer);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        onDrawArraysAfter();

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glDisableVertexAttribArray(mGLPositionIndex);
        GLES20.glDisableVertexAttribArray(mGLTextureCoordinateIndex);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);//将当前活动内存设置为空

        return mGLFboTexId[0];
    }

    protected void onDrawArraysPre() {}

    protected void onDrawArraysAfter() {}
    
    private void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
    
    public int getProgram() {
        return mGLProgId;
    }

    public IntBuffer getGLFboBuffer() {
        return mGLFboBuffer;
    }

    public IntBuffer getGLShotBuffer() { return mGLShotBuffer; }

    protected Context getContext() {
        return mContext;
    }

    protected MagicFilterType getFilterType() {
        return mType;
    }
    
    public void setOutputTransformMatrix(float[] mtx){
        mGLOutputTransformMatrix = mtx;
    }

    public void setGLShotTransformMatrix(float[] mtx) {
        this.mGLShotTransformMatrix = mtx;
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }
}

