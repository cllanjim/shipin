package com.laifeng.sopcastsdk.camera;

/**
 * @Title: CameraListener
 * @Package com.laifeng.sopcastsdk.camera
 * @Description:
 * @Author Jim
 * @Date 16/7/18
 * @Time 上午10:42
 * @Version
 * 开启camera时的回调   重获取检测到开启 设置预览的状态 的过程监控   主要啊是针对过程中可出现的异常分析所得
 *
 */
public interface CameraListener {
    int CAMERA_NOT_SUPPORT = 1;
    int NO_CAMERA = 2;
    int CAMERA_DISABLED = 3;
    int CAMERA_OPEN_FAILED = 4;

    void onOpenSuccess();
    void onOpenFail(int error);
    void onCameraChange();
}
