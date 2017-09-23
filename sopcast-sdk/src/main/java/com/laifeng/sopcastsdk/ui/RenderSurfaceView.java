package com.laifeng.sopcastsdk.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.MyRenderer;
import com.laifeng.sopcastsdk.video.effect.Effect;

/**
 * @Title: RenderSurfaceView
 * @Package com.laifeng.sopcastsdk.ui
 * @Description:  自定义 GLSurfaceView
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:12
 * @Version
 */
public class RenderSurfaceView extends GLSurfaceView {
    private MyRenderer mRenderer;

    public RenderSurfaceView(Context context) {
        super(context);
        init();
    }

    public RenderSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     *  初始化操作
     */
    private void init() {
        //初始化渲染器
        mRenderer = new MyRenderer(this);
        // 摄者EGL 版本
        setEGLContextClientVersion(2);
        // 设置渲染器
        setRenderer(mRenderer);
        // 这是渲染模式
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // 获取SurfaceHolder
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //设置回调
        surfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    public MyRenderer getRenderer() {
        return mRenderer;
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        // surface 销毁
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            SopCastLog.d(SopCastConstant.TAG, "SurfaceView destroy");
           // 摄像头停止预览
            CameraHolder.instance().stopPreview();
            // 释放摄像头资源
            CameraHolder.instance().releaseCamera();
        }
        // surface 创建
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            SopCastLog.d(SopCastConstant.TAG, "SurfaceView created");
        }
        // surface 变化
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            SopCastLog.d(SopCastConstant.TAG, "SurfaceView width:" + width + " height:" + height);
        }
    };

    /***
     * 设置美颜效果
     * @param effect
     */

    public void setEffect(final Effect effect) {
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != mRenderer) {
                    mRenderer.setEffect(effect);
                }
            }
        });
    }
}
