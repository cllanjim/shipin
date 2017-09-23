package com.laifeng.sopcastsdk.camera;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.laifeng.sopcastsdk.camera.exception.CameraHardwareException;
import com.laifeng.sopcastsdk.camera.exception.CameraNotSupportException;
import com.laifeng.sopcastsdk.configuration.CameraConfiguration;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Title: CameraHolder
 * @Package com.youku.crazytogether.app.modules.sopCastV2
 * @Description:  摄像头的管理类   管理所有摄像头的操作
 * @Author Jim
 * @Date 16/3/23
 * @Time 上午11:57
 * @Version
 *  对camera的过程控制  主要是针对状态的处理
 *   及camera 开启过程中的状态处理
 */
@TargetApi(14)
public class CameraHolder {
    private static final String TAG = "CameraHolder";
    private final static int FOCUS_WIDTH = 80;
    private final static int FOCUS_HEIGHT = 80;

    private List<CameraData> mCameraDatas;
    private Camera mCameraDevice;
    private CameraData mCameraData;
    private State mState;
    private SurfaceTexture mTexture;
    private boolean isTouchMode = false;
    private boolean isOpenBackFirst = false;
    private CameraConfiguration mConfiguration = CameraConfiguration.createDefault();

    public enum State {
        INIT,
        OPENED,
        PREVIEW
    }

    private static CameraHolder sHolder;
    public static synchronized CameraHolder instance() {
        if (sHolder == null) {
            sHolder = new CameraHolder();
        }
        return sHolder;
    }

    private CameraHolder() {
        mState = State.INIT;
    }

    /**
     * 获取摄像头数量
     * @return
     */
    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }


    public CameraData getCameraData() {
        return mCameraData;
    }

    /**
     * 是否横屏
     * @return
     */
    public boolean isLandscape() {
        return (mConfiguration.orientation != CameraConfiguration.Orientation.PORTRAIT);
    }

    /**
     * 打开摄像头
     * @return
     * @throws CameraHardwareException
     * @throws CameraNotSupportException
     */

    public synchronized Camera openCamera()
            throws CameraHardwareException, CameraNotSupportException {
        if(mCameraDatas == null || mCameraDatas.size() == 0) {
            //获取所有的摄像头    isOpenBackFirst默认是false 所以获取的第一个不是后置摄像头
            mCameraDatas = CameraUtils.getAllCamerasData(isOpenBackFirst);
        }
        //获取封装的摄像头类  支取第一个
        CameraData cameraData = mCameraDatas.get(0);
        if(mCameraDevice != null && mCameraData == cameraData) {
            return mCameraDevice;
        }
        if (mCameraDevice != null) {
            releaseCamera();
        }
        try {
            SopCastLog.d(TAG, "open camera " + cameraData.cameraID);
            //打开摄像头
            mCameraDevice = Camera.open(cameraData.cameraID);
        } catch (RuntimeException e) {
            SopCastLog.e(TAG, "fail to connect Camera");
            throw new CameraHardwareException(e);
        }
        if(mCameraDevice == null) {
            throw new CameraNotSupportException();
        }
        try {
            CameraUtils.initCameraParams(mCameraDevice, cameraData, isTouchMode, mConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
            mCameraDevice.release();
            mCameraDevice = null;
            throw new CameraNotSupportException();
        }
        mCameraData = cameraData;
        mState = State.OPENED;
        return mCameraDevice;
    }

    /**
     * 设置 SurfaceTexture
     * @param texture
     */
    public void setSurfaceTexture(SurfaceTexture texture) {
        mTexture = texture;
        if(mState == State.PREVIEW && mCameraDevice != null && mTexture != null) {
            try {
                mCameraDevice.setPreviewTexture(mTexture);
            } catch (IOException e) {
                releaseCamera();
            }
        }
    }

    public State getState() {
        return mState;
    }

    /**
     *  设置CameraConfiguration
     * @param configuration
     */
    public void setConfiguration(CameraConfiguration configuration) {
        isTouchMode = (configuration.focusMode != CameraConfiguration.FocusMode.AUTO);
        isOpenBackFirst = (configuration.facing != CameraConfiguration.Facing.FRONT);
        mConfiguration = configuration;
    }

    /**
     * 开始预览
     */

    public synchronized void startPreview() {
        if(mState != State.OPENED) {
            return;
        }
        if(mCameraDevice == null) {
            return;
        }
        if(mTexture == null) {
            return;
        }
        try {
            mCameraDevice.setPreviewTexture(mTexture);
            mCameraDevice.startPreview();
            mState = State.PREVIEW;
        } catch (Exception e) {
            releaseCamera();
            e.printStackTrace();
        }
    }

    /**
     * 关闭预览
     */

    public synchronized void stopPreview() {
        if(mState != State.PREVIEW) {
            return;
        }
        if(mCameraDevice == null) {
            return;
        }
        mCameraDevice.setPreviewCallback(null);
        Camera.Parameters cameraParameters = mCameraDevice.getParameters();
        if (cameraParameters != null && cameraParameters.getFlashMode() != null
                && !cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        mCameraDevice.setParameters(cameraParameters);
        mCameraDevice.stopPreview();
        mState = State.OPENED;
    }

    /**
     * 释放摄像头
     */

    public synchronized void releaseCamera() {
        if(mState == State.PREVIEW) {
            //停止预览
            stopPreview();
        }
        if(mState != State.OPENED) {
            return;
        }
        if(mCameraDevice == null) {
            return;
        }
        //释放摄像头资源
        mCameraDevice.release();
        mCameraDevice = null;
        mCameraData = null;
        //重置摄像头状态
        mState = State.INIT;
    }

    /**
     * 释放资源
     */
    public void release() {
        mCameraDatas = null;
        mTexture = null;
        isTouchMode = false;
        isOpenBackFirst = false;
        mConfiguration = CameraConfiguration.createDefault();
    }

    /**
     * 设置焦点
     * @param x
     * @param y
     */
    public void setFocusPoint(int x, int y) {
        if(mState != State.PREVIEW || mCameraDevice == null) {
            return;
        }
        if (x < -1000 || x > 1000 || y < -1000 || y > 1000) {
            SopCastLog.w(TAG, "setFocusPoint: values are not ideal " + "x= " + x + " y= " + y);
            return;
        }
        Camera.Parameters params = mCameraDevice.getParameters();

        if (params != null && params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusArea = new ArrayList<Camera.Area>();
            focusArea.add(new Camera.Area(new Rect(x, y, x + FOCUS_WIDTH, y + FOCUS_HEIGHT), 1000));

            params.setFocusAreas(focusArea);

            try {
                mCameraDevice.setParameters(params);
            } catch (Exception e) {
                // Ignore, we might be setting it too
                // fast since previous attempt
            }
        } else {
            SopCastLog.w(TAG, "Not support Touch focus mode");
        }
    }

    /**
     * 自动对焦
     * @param focusCallback
     * @return
     */
    public boolean doAutofocus(Camera.AutoFocusCallback focusCallback) {
        if(mState != State.PREVIEW || mCameraDevice == null) {
            return false;
        }
        // Make sure our auto settings aren't locked
        Camera.Parameters params = mCameraDevice.getParameters();
        if (params.isAutoExposureLockSupported()) {
            params.setAutoExposureLock(false);
        }

        if (params.isAutoWhiteBalanceLockSupported()) {
            params.setAutoWhiteBalanceLock(false);
        }

        mCameraDevice.setParameters(params);
        mCameraDevice.cancelAutoFocus();
        mCameraDevice.autoFocus(focusCallback);
        return true;
    }

    /**
     *  改变焦点模式
     * @param touchMode
     */
    public void changeFocusMode(boolean touchMode) {
        if(mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
            return;
        }
        isTouchMode = touchMode;
        mCameraData.touchFocusMode = touchMode;
        if(touchMode) {
            CameraUtils.setTouchFocusMode(mCameraDevice);
        } else {
            CameraUtils.setAutoFocusMode(mCameraDevice);
        }
    }

    /**
     * 切换焦点模式
     */
    public void switchFocusMode() {
        changeFocusMode(!isTouchMode);
    }

    /**
     * 缩放
     * @param isBig
     * @return
     */
    public float cameraZoom(boolean isBig) {
        if(mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
            return -1;
        }
        Camera.Parameters params = mCameraDevice.getParameters();
        if(isBig) {
            //设置缩放值
            params.setZoom(Math.min(params.getZoom() + 1, params.getMaxZoom()));
        } else {
            params.setZoom(Math.max(params.getZoom() - 1, 0));
        }
        mCameraDevice.setParameters(params);
        //计算缩放比例
        return (float) params.getZoom()/params.getMaxZoom();
    }

    /**
     * 切换摄像头
     * @return
     */
    public boolean switchCamera() {
        if(mState != State.PREVIEW) {
            return false;
        }
        try {
            CameraData camera = mCameraDatas.remove(1);
            mCameraDatas.add(0, camera);
            openCamera();
            startPreview();
            return true;
        } catch (Exception e) {
            CameraData camera = mCameraDatas.remove(1);
            mCameraDatas.add(0, camera);
            try {
                openCamera();
                startPreview();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 切换闪光灯
     * @return
     */
    public boolean switchLight() {
        if(mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
            return false;
        }
        if(!mCameraData.hasLight) {
            return false;
        }
        Camera.Parameters cameraParameters = mCameraDevice.getParameters();
        if (cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        try {
            mCameraDevice.setParameters(cameraParameters);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
