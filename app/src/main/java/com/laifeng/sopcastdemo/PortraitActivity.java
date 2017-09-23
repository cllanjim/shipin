package com.laifeng.sopcastdemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.laifeng.sopcastdemo.ui.MultiToggleImageButton;
import com.laifeng.sopcastsdk.camera.CameraListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.entity.Watermark;
import com.laifeng.sopcastsdk.entity.WatermarkPosition;
import com.laifeng.sopcastsdk.stream.packer.flv.FlvPacker;
import com.laifeng.sopcastsdk.stream.sender.local.LocalSender;
import com.laifeng.sopcastsdk.ui.CameraLivingView;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.effect.GrayEffect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;

/**
 * 竖屏
 *
 */
public class PortraitActivity extends Activity {
    // 直播 View（最主要的）
    private CameraLivingView mLFLiveView;
    private MultiToggleImageButton mMicBtn;
    private MultiToggleImageButton mFlashBtn;
    private MultiToggleImageButton mFaceBtn;
    private MultiToggleImageButton mBeautyBtn;
    private MultiToggleImageButton mFocusBtn;
    // 手势探测器
    private GestureDetector mGestureDetector;
    // 灰度效果
    private GrayEffect mGrayEffect;
    // 无效果
    private NullEffect mNullEffect;
    // 录制按钮
    private ImageButton mRecordBtn;
    // 是否是灰
    private boolean isGray;
    // 是否正在录制
    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portrait);
        initEffects();
        initViews();
        initListeners();
        initLiveView();
    }

    /**
     * 初始化效果
     */
    private void initEffects() {
        mGrayEffect = new GrayEffect(this);
        mNullEffect = new NullEffect(this);
    }

    /**
     * 初始化View
     */
    private void initViews() {
        mLFLiveView = (CameraLivingView) findViewById(R.id.liveView);
        mMicBtn = (MultiToggleImageButton) findViewById(R.id.record_mic_button);
        mFlashBtn = (MultiToggleImageButton) findViewById(R.id.camera_flash_button);
        mFaceBtn = (MultiToggleImageButton) findViewById(R.id.camera_switch_button);
        mBeautyBtn = (MultiToggleImageButton) findViewById(R.id.camera_render_button);
        mFocusBtn = (MultiToggleImageButton) findViewById(R.id.camera_focus_button);
        mRecordBtn = (ImageButton) findViewById(R.id.btnRecord);
    }

    /**
     * 初始化监听器
     *
     */
    private void initListeners() {
        /**
         * 静音
         */
        mMicBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.mute(true);
            }
        });
        /**
         * 切换闪光灯
         */
        mFlashBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.switchTorch();
            }
        });
        /**
         * 切换摄像头
         */
        mFaceBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.switchCamera();
            }
        });
        /**
         * 设置灰度效果
         */
        mBeautyBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if(isGray) {
                    mLFLiveView.setEffect(mNullEffect);
                    isGray = false;
                } else {
                    mLFLiveView.setEffect(mGrayEffect);
                    isGray = true;
                }
            }
        });
        /**
         * 设置焦点模式
         */
        mFocusBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.switchFocusMode();
            }
        });
        /**
         * 录制按钮
         */
        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording) {
                    mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
                    mLFLiveView.stop();
                    isRecording = false;
                } else {
                    mRecordBtn.setBackgroundResource(R.mipmap.ic_record_stop);
                    mLFLiveView.start();
                    isRecording = true;
                }
            }
        });
    }

    //1
    private void initLiveView() {
        SopCastLog.isOpen(true);
        mLFLiveView.init();
        //设置预览监听
        mLFLiveView.setCameraOpenListener(new CameraListener() {
            @Override
            public void onOpenSuccess() {
                Toast.makeText(PortraitActivity.this, "摄像头开启成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOpenFail(int error) {
                Toast.makeText(PortraitActivity.this, "摄像头开启失败", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCameraChange() {
                Toast.makeText(PortraitActivity.this, "摄像头切换", Toast.LENGTH_LONG).show();
            }
        });
        //设置水印
        Bitmap watermarkImg = BitmapFactory.decodeResource(getResources(), R.mipmap.watermark);
        Watermark watermark = new Watermark(watermarkImg, 50, 25, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
        mLFLiveView.setWatermark(watermark);

        //初始化flv打包器
        FlvPacker packer = new FlvPacker();
        packer.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        packer.initVideoParams(360, 640, 24);
        mLFLiveView.setPacker(packer);
        //设置发送器
        mLFLiveView.setSender(new LocalSender());
        //设置手势识别
        mGestureDetector = new GestureDetector(this, new GestureListener());
        mLFLiveView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling left
                Toast.makeText(PortraitActivity.this, "Fling Left", Toast.LENGTH_SHORT).show();
            } else if (e2.getX() - e1.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling right
                Toast.makeText(PortraitActivity.this, "Fling Right", Toast.LENGTH_SHORT).show();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLFLiveView.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLFLiveView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLFLiveView.stop();
        mLFLiveView.release();
    }
}
