package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Looper;

import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.camera.CameraListener;
import com.laifeng.sopcastsdk.camera.CameraUtils;
import com.laifeng.sopcastsdk.camera.exception.CameraDisabledException;
import com.laifeng.sopcastsdk.camera.exception.CameraHardwareException;
import com.laifeng.sopcastsdk.camera.exception.CameraNotSupportException;
import com.laifeng.sopcastsdk.camera.exception.NoCameraException;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.entity.Watermark;
import com.laifeng.sopcastsdk.mediacodec.VideoMediaCodec;
import com.laifeng.sopcastsdk.utils.WeakHandler;
import com.laifeng.sopcastsdk.video.effect.Effect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @Title: MyRenderer
 * @Package com.laifeng.sopcastsdk.video
 * @Description: RenderSurfaceView 的渲染器
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:06
 * @Version
 */
@TargetApi(18)
public class MyRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private int mSurfaceTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    // 水印
    private Watermark mWatermark;
    // 渲染到屏幕
    private RenderScreen mRenderScreen;
    //
    private RenderSrfTex mRenderSrfTex;
    //
    private CameraListener mCameraOpenListener;
    //
    private WeakHandler mHandler = new WeakHandler(Looper.getMainLooper());
    //
    private GLSurfaceView mView;
    //
    private boolean isCameraOpen;
    //
    private Effect mEffect;
    //
    private int mEffectTextureId;
    //
    private VideoConfiguration mVideoConfiguration;
    //
    private boolean updateSurface = false;
    //
    private final float[] mTexMtx = GlUtil.createIdentityMtx();

    private int mVideoWidth;
    private int mVideoHeight;

    /**
     * 初始化NullEffect
     *
     * @param view
     */
    public MyRenderer(GLSurfaceView view) {
        mView = view;
        mEffect = new NullEffect(mView.getContext());
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        this.mCameraOpenListener = cameraOpenListener;
    }

    /**
     * 设置 VideoConfiguration  视屏配置
     *
     * @param videoConfiguration
     */
    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
        mVideoConfiguration = videoConfiguration;
        mVideoWidth = VideoMediaCodec.getVideoSize(mVideoConfiguration.width);
        mVideoHeight = VideoMediaCodec.getVideoSize(mVideoConfiguration.height);
        if (mRenderScreen != null) {
            mRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
        }
    }

    /**
     * 设置 录制者
     *
     * @param recorder
     */
    public void setRecorder(MyRecorder recorder) {
        synchronized (this) {
            if (recorder != null) {
                mRenderSrfTex = new RenderSrfTex(mEffectTextureId, recorder);
                mRenderSrfTex.setVideoSize(mVideoWidth, mVideoHeight);
                if (mWatermark != null) {
                    mRenderSrfTex.setWatermark(mWatermark);
                }
            } else {
                mRenderSrfTex = null;
            }
        }
    }

    /**
     *
     * camera 回调来 当有数据上来后会进到
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            updateSurface = true;
        }
        mView.requestRender();
    }

    //2
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initSurfaceTexture();
    }

    //3
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        startCameraPreview();
        if (isCameraOpen) { //可以预览 表示可以进行下一步动作
            if (mRenderScreen == null) {
                // 有关纹理的处理
                initScreenTexture();
            }
            //设置宽高  初始化纹理坐标
            mRenderScreen.setSreenSize(width, height);
            if (mVideoConfiguration != null) {  //
                mRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
            }
            if (mWatermark != null) { //
                mRenderScreen.setWatermark(mWatermark);
            }
        }
    }

    /**
     * 回到绘制每一帧
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTexMtx);
                updateSurface = false;
            }
        }
        mEffect.draw(mTexMtx);
        if (mRenderScreen != null) {
            mRenderScreen.draw();
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.draw();
        }
    }

    /**
     * 初始化 SurfaceTexture
     */
    private void initSurfaceTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        //生成纹理id，可以一次生成多个，后续操作纹理全靠这个id
        mSurfaceTextureId = textures[0];
        /**
         * SurfaceTexture 可以从camera preview或者video decode里面获取图像流（image stream）。
         * 但是，和SurfaceView不同的是，SurfaceTexture在接收图像流之后，不需要显示出来
         * SurfaceTexture不需要显示到屏幕上，因此我们可以用SurfaceTexture接收来自camera的图像流，
         * */
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        //设置帧回调
        mSurfaceTexture.setOnFrameAvailableListener(this);
        //关闭深度检测
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        //关闭剔除操作效果 1.glCullFace()参数包括GL_FRONT和GL_BACK。
        // 表示禁用多边形正面或者背面上的光照、阴影和颜色计算及操作，消除不必要的渲染计算。
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        //关闭混合 ？？？？
        GLES20.glDisable(GLES20.GL_BLEND);
        //选择活动纹理单元。 默认值   // 启用纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //操作纹理，传入纹理id作为参数，每次bind之后，后续操作的纹理都是该纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);

        /****
         * glTexParameterf 是设置纹理贴图的参数属性
         比如target，表示你使用的1d纹理还是2d纹理，就是一维的，还是二维的，在pc上，还有3d纹理，立方体贴图和球面贴图等，手机上估计只有1d和2d；
         pname设置环绕方式；
         param纹理过滤方式，如线性过滤和双线性插值等

         pname：
         GL_TEXTURE_MIN_FILTER 设置最小过滤，第三个参数决定用什么过滤；
         GL_TEXTURE_MAG_FILTER设置最大过滤，也是第三个参数决定；
         GL_TEXTURE_WRAP_S；纹理坐标一般用str表示，分别对应xyz，2d纹理用st表示
         GL_TEXTURE_WRAP_T   接上面，纹理和你画的几何体可能不是完全一样大的，在边界的时候如何处理呢？就是这两个参数决定的，wrap表示环绕，
         可以理解成让纹理重复使用，直到全部填充完成；
         param；与第二个参数配合使用，一般取GL_LINEAR和GL_NEAREST，过滤形式


         * target  GLES11Ext.GL_TEXTURE_EXTERNAL_OES:目标纹理
         * pname: 用来设置纹理映射过程中像素映射的问题等，取值可以为：GL_TEXTURE_MIN_FILTER
         * GL_TEXTURE_MAG_FILTER、GL_TEXTURE_WRAP_S、GL_TEXTURE_WRAP_T，详细含义可以查看MSDN
         * param:实际上就是pname的值，可以参考MSDN
         */
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //确定如何把图象从纹理图象空间映射到帧缓冲图象空间(如：映射时为了避免多边形上的图像失真，而重新构造纹理图像等)
        // 只是方向不同  s方向
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        // 只是方向不同 t方向
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void initScreenTexture() {
        //设置纹路id
        mEffect.setTextureId(mSurfaceTextureId);

        mEffect.prepare();
        mEffectTextureId = mEffect.getEffertedTextureId();
        mRenderScreen = new RenderScreen(mEffectTextureId);
    }

    /**
     * 开启摄像头预览
     */
    private void startCameraPreview() {
        try {
            //是否有摄像头 是否可用
            CameraUtils.checkCameraService(mView.getContext());
        } catch (CameraDisabledException e) {
            postOpenCameraError(CameraListener.CAMERA_DISABLED);
            e.printStackTrace();
            return;
        } catch (NoCameraException e) {
            postOpenCameraError(CameraListener.NO_CAMERA);
            e.printStackTrace();
            return;
        }
        //初始化与camera 相关类的初始化状态 init
        CameraHolder.State state = CameraHolder.instance().getState();
        //设置camera设置 SurfaceTexture   貌似在此处没用其实不然 主要是讲mSurfaceTexture 设置给CameraHolder的成员变量mTexture
        //要不然  步骤--开始预览----->mTexture ==null
        CameraHolder.instance().setSurfaceTexture(mSurfaceTexture);
        //根据初始化状态设置预览
        if (state != CameraHolder.State.PREVIEW) {
            try {
                // 打开摄像头 ----过程较多  1 2 3
                CameraHolder.instance().openCamera();
                //开始预览
                CameraHolder.instance().startPreview();
                if (mCameraOpenListener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCameraOpenListener.onOpenSuccess();
                        }
                    });
                }
                isCameraOpen = true;
            } catch (CameraHardwareException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_OPEN_FAILED);
            } catch (CameraNotSupportException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_NOT_SUPPORT);
            }
        }
    }
    //摄像头打开失败 回调error:错误原因
    private void postOpenCameraError(final int error) {
        if (mCameraOpenListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCameraOpenListener != null) {
                        mCameraOpenListener.onOpenFail(error);
                    }
                }
            });
        }
    }

    public boolean isCameraOpen() {
        return isCameraOpen;
    }

    /**
     * 设置水印
     *
     * @param watermark
     */
    public void setWatermark(Watermark watermark) {
        mWatermark = watermark;
        if (mRenderScreen != null) {
            mRenderScreen.setWatermark(watermark);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setWatermark(watermark);
        }
    }

    /**
     * 设置美颜效果
     *
     * @param effect
     */
    public void setEffect(Effect effect) {
        mEffect.release();
        mEffect = effect;
        effect.setTextureId(mSurfaceTextureId);
        effect.prepare();
        mEffectTextureId = effect.getEffertedTextureId();
        if (mRenderScreen != null) {
            mRenderScreen.setTextureId(mEffectTextureId);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setTextureId(mEffectTextureId);
        }
    }
}
