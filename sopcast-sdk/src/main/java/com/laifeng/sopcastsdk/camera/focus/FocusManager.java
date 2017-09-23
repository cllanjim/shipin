package com.laifeng.sopcastsdk.camera.focus;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;

import com.laifeng.sopcastsdk.camera.CameraHolder;

/***
 *  聚焦过程中 的逻辑处理   监听类
 *   包括重新自动聚焦
 */
public class FocusManager implements AutoFocusCallback {
    public final static String TAG = "FocusManager";
    //聚焦状态的监听  开始聚焦和聚焦完毕状态返回
    public interface FocusListener {
        void onFocusStart();
        void onFocusReturns(boolean success);
    }

    private FocusListener mListener;

    public void setListener(FocusListener listener) {
        mListener = listener;
    }
    //重新聚焦
    public void refocus() {
        boolean focusResult = CameraHolder.instance().doAutofocus(this);
        if (focusResult) {
            if (mListener != null) {
                mListener.onFocusStart();
            }
        }
    }
    //自动聚焦的回调
    @Override
    public void onAutoFocus(boolean success, Camera cam) {
        if (mListener != null) {
            mListener.onFocusReturns(success);
        }
    }
}
