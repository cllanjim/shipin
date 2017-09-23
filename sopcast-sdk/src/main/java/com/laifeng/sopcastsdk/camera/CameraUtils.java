package com.laifeng.sopcastsdk.camera;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;

import com.laifeng.sopcastsdk.configuration.CameraConfiguration;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.blacklist.BlackListHelper;
import com.laifeng.sopcastsdk.camera.exception.CameraDisabledException;
import com.laifeng.sopcastsdk.camera.exception.CameraNotSupportException;
import com.laifeng.sopcastsdk.camera.exception.NoCameraException;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import java.util.ArrayList;
import java.util.List;


/**
 * @Title: CameraUtils
 * @Package com.youku.crazytogether.app.modules.sopCastV2
 * @Description:
 * @Author Jim
 * @Date 16/3/23
 * @Time 下午12:01
 * @Version
 *  camera 设置相关类
 */
@TargetApi(14)
public class CameraUtils {

    //获取所支持的camera 封装对象
    public static List<CameraData> getAllCamerasData(boolean isBackFirst) {
        ArrayList<CameraData> cameraDatas = new ArrayList<>();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                CameraData cameraData = new CameraData(i, CameraData.FACING_FRONT);
                if(isBackFirst) {
                    cameraDatas.add(cameraData);
                } else {
                    cameraDatas.add(0, cameraData);
                }
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                CameraData cameraData = new CameraData(i, CameraData.FACING_BACK);
                if(isBackFirst) {
                    cameraDatas.add(0, cameraData);
                } else {
                    cameraDatas.add(cameraData);
                }
            }
        }
        return cameraDatas;
    }
    //设置摄像头的参数
    public static void initCameraParams(Camera camera, CameraData cameraData, boolean isTouchMode, CameraConfiguration configuration)
            throws CameraNotSupportException {
        boolean isLandscape = (configuration.orientation != CameraConfiguration.Orientation.PORTRAIT);
        int cameraWidth = Math.max(configuration.height, configuration.width);
        int cameraHeight = Math.min(configuration.height, configuration.width);
        Camera.Parameters parameters = camera.getParameters();
        //设置图片格式
        setPreviewFormat(camera, parameters);
        //设置帧率
        setPreviewFps(camera, configuration.fps, parameters);
        //设置合适的尺寸
        setPreviewSize(camera, cameraData, cameraWidth, cameraHeight, parameters);
        //闪光灯
        cameraData.hasLight = supportFlash(camera);
        //设置摄像头的矫正角度
        setOrientation(cameraData, isLandscape, camera);
        //设置聚焦方式   自动聚焦和手动聚焦
        setFocusMode(camera, cameraData, isTouchMode);
    }

    public static void setPreviewFormat(Camera camera, Camera.Parameters parameters) throws CameraNotSupportException{
        //设置预览回调的图片格式
        try {
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        } catch (Exception e) {
            throw new CameraNotSupportException();
        }
    }
    //设置摄像头预览帧率
    public static void setPreviewFps(Camera camera, int fps, Camera.Parameters parameters) {
        if(BlackListHelper.deviceInFpsBlacklisted()) {
            SopCastLog.d(SopCastConstant.TAG, "Device in fps setting black list, so set the camera fps 15");
            fps = 15;
        }
        try {
            /* 每秒从摄像头捕获15帧画面， */
            parameters.setPreviewFrameRate(fps);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //???  预览帧数从min到max，这个值再*1000.
        int[] range = adaptPreviewFps(fps, parameters.getSupportedPreviewFpsRange());

        try {
            parameters.setPreviewFpsRange(range[0], range[1]);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //调整帧频范围
    private static int[] adaptPreviewFps(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }
    //设置预览尺寸
    public static void setPreviewSize(Camera camera, CameraData cameraData, int width, int height,
                                      Camera.Parameters parameters) throws CameraNotSupportException {
        Camera.Size size = getOptimalPreviewSize(camera, width, height);
        if(size == null) {
            throw new CameraNotSupportException();
        }else {
            cameraData.cameraWidth = size.width;
            cameraData.cameraHeight = size.height;
        }
        //设置预览大小
        SopCastLog.d(SopCastConstant.TAG, "Camera Width: " + size.width + "    Height: " + size.height);
        try {
            parameters.setPreviewSize(cameraData.cameraWidth, cameraData.cameraHeight);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //设置摄像头的矫正度数
    private static void setOrientation(CameraData cameraData, boolean isLandscape, Camera camera) {
        int orientation = getDisplayOrientation(cameraData.cameraID);
        if(isLandscape) {
            orientation = orientation - 90;
        }
        camera.setDisplayOrientation(orientation);
    }
    //设置聚焦模式
    private static void setFocusMode(Camera camera, CameraData cameraData, boolean isTouchMode) {
        cameraData.supportTouchFocus = supportTouchFocus(camera);
        if(!cameraData.supportTouchFocus) {
            setAutoFocusMode(camera);
        } else {
            if(!isTouchMode) {
                cameraData.touchFocusMode = false;
                setAutoFocusMode(camera);
            }else {
                cameraData.touchFocusMode = true;
            }
        }
    }
    //是否支持触摸聚焦
    public static boolean supportTouchFocus(Camera camera) {
        if(camera != null) {
            return (camera.getParameters().getMaxNumFocusAreas() != 0);
        }
        return false;
    }
    //设置自动聚焦
    public static void setAutoFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //设置触摸聚焦
    public static void setTouchFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 设置与目标相差在最小的 合适的支出的 尺寸
    public static Camera.Size getOptimalPreviewSize(Camera camera, int width, int height) {
        Camera.Size optimalSize = null;
        double minHeightDiff = Double.MAX_VALUE;
        double minWidthDiff = Double.MAX_VALUE;
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return null;
        //找到宽度差距最小的
        for(Camera.Size size:sizes){
            if (Math.abs(size.width - width) < minWidthDiff) {
                minWidthDiff = Math.abs(size.width - width);
            }
        }
        //在宽度差距最小的里面，找到高度差距最小的
        for(Camera.Size size:sizes){
            if(Math.abs(size.width - width) == minWidthDiff) {
                if(Math.abs(size.height - height) < minHeightDiff) {
                    optimalSize = size;
                    minHeightDiff = Math.abs(size.height - height);
                }
            }
        }
        return optimalSize;
    }

    //根据摄像头 是前置 环视 后置 计算 矫正角度
    public static int getDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation + 360) % 360;
        }
        return result;
    }
    //是否有摄像头
    public static void checkCameraService(Context context)
            throws CameraDisabledException, NoCameraException {
        // Check if device policy has disabled the camera.
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
        List<CameraData> cameraDatas = getAllCamerasData(false);
        if(cameraDatas.size() == 0) {
            throw new NoCameraException();
        }
    }
        //闪光灯
    public static boolean supportFlash(Camera camera){
        Camera.Parameters params = camera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if(flashModes == null) {
            return false;
        }
        for(String flashMode : flashModes) {
            if(Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                return true;
            }
        }
        return false;
    }
}
