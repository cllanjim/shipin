package com.laifeng.sopcastsdk.video.effect;

import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.text.TextUtils;

import com.laifeng.sopcastsdk.camera.CameraData;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.GlUtil;

import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * @Title: Effect
 * @Package com.laifeng.sopcastsdk.video.effert
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:10
 * @Version
 */
public abstract class Effect {
    private final FloatBuffer mVtxBuf = GlUtil.createSquareVtx();
    private final float[]     mPosMtx = GlUtil.createIdentityMtx();

    protected int mTextureId = -1;
    private int mProgram            = -1;
    private int maPositionHandle    = -1;
    private int maTexCoordHandle    = -1;
    private int muPosMtxHandle      = -1;
    private int muTexMtxHandle      = -1;

    private final int[]       mFboId  = new int[]{0};
    private final int[]       mRboId  = new int[]{0};
    private final int[]       mTexId  = new int[]{0};

    private int mWidth  = -1;
    private int mHeight = -1;
    private float mAngle = 270;

    private final LinkedList<Runnable> mRunOnDraw;
    private String mVertex;
    private String mFragment;

    public Effect() {
        mRunOnDraw = new LinkedList<>();
    }

    public void setShader(String vertex, String fragment) {
        mVertex = vertex;
        mFragment = fragment;
    }

    public void prepare() {
        //加载着色器 及 编译 连接  获取 着色器中的参数 id 加一控制
        loadShaderAndParams(mVertex, mFragment);
        initSize();  // 获取camera的尺寸 大小   初始化视图的大小
        createEffectTexture(); //创建纹理
    }

    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }
    //加载着色器及参数
    private void loadShaderAndParams(String vertex, String fragment) {
        if(TextUtils.isEmpty(vertex) || TextUtils.isEmpty(fragment)) {
            vertex = SopCastConstant.SHARDE_NULL_VERTEX;
            fragment = SopCastConstant.SHARDE_NULL_FRAGMENT;
            SopCastLog.e(SopCastConstant.TAG, "Couldn't load the shader, so use the null shader!");
        }
        GlUtil.checkGlError("initSH_S");
        //通过着色器  获取编译连接后的结果  创建着色器程序容器
        mProgram = GlUtil.createProgram(vertex, fragment);
        //获取着色器程序内成员变量的position
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        //获取着色器程序内成员变量的inputTextureCoordinate
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        //指定为uniform类型变量的uPosMtx
        muPosMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        //指定为uniform类型变量的uTexMtx
        muTexMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uTexMtx");

        //获取的指向着色器相应数据成员的各个id
        // 就能将我们自己定义的顶点数据、颜色数据等等各种数据传递到着色器当中了。

        loadOtherParams();
        GlUtil.checkGlError("initSH_E");
    }

    protected void loadOtherParams() {
        //do nothing
    }

    private void initSize() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        CameraData cameraData = CameraHolder.instance().getCameraData();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        if(CameraHolder.instance().isLandscape()) {
            mWidth = Math.max(width, height);
            mHeight = Math.min(width, height);
        } else {
            mWidth = Math.min(width, height);
            mHeight = Math.max(width, height);
        }

    }
    //创建有效果的 纹理
    private void createEffectTexture() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        GlUtil.checkGlError("initFBO_S");
        //创建帧缓冲对象：
        GLES20.glGenFramebuffers(1, mFboId, 0);
        //创建帧缓冲对象：
        GLES20.glGenRenderbuffers(1, mRboId, 0);
        //获取纹理id
        GLES20.glGenTextures(1, mTexId, 0);

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRboId[0]);
        //
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);
        //绑定帧缓冲区：
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRboId[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//启用纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //指定一个二维的纹理图片
        /**
         * glTexImage2D—— 指定一个二维的纹理图片
         * target     指定目标纹理，这个值必须是GL_TEXTURE_2D。
         * level       执行细节级别。0是最基本的图像级别，你表示第N级贴图细化级别。
         * internalformat     指定纹理中的颜色组件，这个取值和后面的format取值必须相同。可选的值有
         *            GL_ALPHA,
                 GL_RGB,
                GL_RGBA,
                GL_LUMINANCE,
                GL_LUMINANCE_ALPHA 等几种。
         *width     指定纹理图像的宽度，必须是2的n次方。纹理图片至少要支持64个材质元素的宽度
         height     指定纹理图像的高度，必须是2的m次方。纹理图片至少要支持64个材质元素的高度
         border    指定边框的宽度。必须为0。
         format    像素数据的颜色格式，必须和internalformatt取值必须相同。可选的值有
         GL_ALPHA,
         GL_RGB,
         GL_RGBA,
         GL_LUMINANCE,
         GL_LUMINANCE_ALPHA 等几种。
         type        指定像素数据的数据类型。可以使用的值有
         GL_UNSIGNED_BYTE,
         GL_UNSIGNED_SHORT_5_6_5,
         GL_UNSIGNED_SHORT_4_4_4_4,
         GL_UNSIGNED_SHORT_5_5_5_1
         pixels      指定内存中指向图像数据的指针
         */
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);


        // 绑定纹理：把一幅纹理图像关联到一个FBO。第一个参数一定是GL_FRAMEBUFFER_，第二个参数是关联纹理图像的关联点。
        // 一个帧缓冲区对象可以有多个颜色关联点(GL_COLOR_ATTACHMENT0, ..., GL_COLOR_ATTACHMENTn),L_DEPTH_ATTACHMENT,
        // 和GL_STENCIL_ATTACHMENT。第三个参数textureTarget在多数情况下是GL_TEXTURE_2D。第四个参数是纹理对象的ID号。
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexId[0], 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("glCheckFramebufferStatus()");
        }
        GlUtil.checkGlError("initFBO_E");
    }

    public int getEffertedTextureId() {
        return mTexId[0];
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    public void draw(final float[] tex_mtx) {
        if (-1 == mProgram || mTextureId == -1 || mWidth == -1) {
            return;
        }

        GlUtil.checkGlError("draw_S");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);

        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        /// 使用shader程序
        GLES20.glUseProgram(mProgram);
        runPendingOnDrawTasks();
        // 设置缓冲区起始位置
        mVtxBuf.position(0);
        //// 顶点位置数据传入着色器
        GLES20.glVertexAttribPointer(maPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        mVtxBuf.position(3);
        // 顶点位置数据传入着色器
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        // 允许使用顶点坐标数组 // 允许使用定点纹理数组
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        if(muPosMtxHandle>= 0)
            //// 将最终变换矩阵传入shader程序   // 应用投影和视口变换
            GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);

        if(muTexMtxHandle>= 0)
            //// 将最终变换矩阵传入shader程序    // 应用投影和视口变换
            GLES20.glUniformMatrix4fv(muTexMtxHandle, 1, false, tex_mtx, 0);
// 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        // 图形三角形绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkGlError("draw_E");
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

    public void release() {
        if (-1 != mProgram) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = -1;
        }
    }
}
