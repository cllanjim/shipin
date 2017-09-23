package com.laifeng.sopcastsdk.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.laifeng.sopcastsdk.R;
import com.laifeng.sopcastsdk.camera.CameraData;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.camera.CameraZoomListener;
import com.laifeng.sopcastsdk.camera.focus.FocusManager;
import com.laifeng.sopcastsdk.camera.focus.FocusPieView;
import com.laifeng.sopcastsdk.utils.WeakHandler;
import com.laifeng.sopcastsdk.video.MyRenderer;

/**
 * @Title: CameraView
 * @Package com.laifeng.sopcastsdk.ui
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:31
 * @Version
 */
public class CameraView extends FrameLayout {
    private Context mContext;
    protected RenderSurfaceView mRenderSurfaceView;
    protected MyRenderer mRenderer;
    private FocusPieView mFocusHudRing;
    private FocusManager mFocusManager;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mZoomGestureDetector;
    private WeakHandler mHandler;
    private boolean mIsFocusing;
    private CameraZoomListener mZoomListener;
    private boolean isFocusTouchMode = false;
    private boolean isMediaOverlay;
    private boolean isRenderSurfaceViewShowing = true;
    private float mAspectRatio = 9.0f/16;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
        initAspectRatio(attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
        initAspectRatio(attrs);
    }

    public CameraView(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    private void initView() {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //加载 GLSurfaceView显示界面
        mInflater.inflate(R.layout.layout_camera_view, this, true);

        mHandler = new WeakHandler();
        // GLSurfaceView  自定义中已经将渲染器Renderer 设置进去
        mRenderSurfaceView = (RenderSurfaceView) findViewById(R.id.render_surface_view);
        // 设置GLSurfaceView 的显示层级
        mRenderSurfaceView.setZOrderMediaOverlay(isMediaOverlay);
        //获取 GLSurfaceView的渲染器Renderer
        mRenderer = mRenderSurfaceView.getRenderer();
        //手指点击聚焦时显示的 图或状态
        mFocusHudRing = (FocusPieView) findViewById(R.id.focus_view);
        //使用FocusManager 监管聚焦状态和重新聚焦
        mFocusManager = new FocusManager();
        //设置聚焦监听 ，处理聚焦时应该做的动作  譬如 显示正在聚焦时-动图，及聚焦完成隐藏动态
        mFocusManager.setListener(new MainFocusListener());
        //手指点击事件
        mGestureDetector = new GestureDetector(mContext, new GestureListener());
        //手指双指放大缩小事件  摄像图跟随缩放
        mZoomGestureDetector = new ScaleGestureDetector(mContext, new ZoomGestureListener());
    }

    private void initAspectRatio(AttributeSet attrs) {
        TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.CameraLivingView);
        mAspectRatio = a.getFloat(R.styleable.CameraLivingView_aspect_ratio, 9.0f / 16);
    }

    public void setOnZoomProgressListener(CameraZoomListener listener) {
        mZoomListener = listener;
    }

    @Override
    public void setVisibility(int visibility) {
        int currentVisibility = getVisibility();
        if(visibility == currentVisibility) {
            return;
        }
        switch (visibility) {
            case VISIBLE:
                addRenderSurfaceView();
                break;
            case GONE:
                removeRenderSurfaceView();
                break;
            case INVISIBLE:
                removeRenderSurfaceView();
                break;
        }
        super.setVisibility(visibility);
    }

    private void addRenderSurfaceView() {
        if(!isRenderSurfaceViewShowing) {
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            addView(mRenderSurfaceView, 0, layoutParams);
            isRenderSurfaceViewShowing = true;
        }
    }

    private void removeRenderSurfaceView() {
        if(isRenderSurfaceViewShowing) {
            removeView(mRenderSurfaceView);
            isRenderSurfaceViewShowing = false;
        }
    }

    /**
     * Focus listener to animate the focus HUD ring from FocusManager events
     *  聚焦过程中显示的一张动态图片 标示正在聚焦
     */
    private class MainFocusListener implements FocusManager.FocusListener {
        @Override
        public void onFocusStart() {
            mIsFocusing = true;
            mFocusHudRing.setVisibility(VISIBLE);
            mFocusHudRing.animateWorking(1500);
            requestLayout();
        }

        @Override
        public void onFocusReturns(final boolean success) {
            mIsFocusing = false;
            mFocusHudRing.setFocusImage(success);
            mFocusHudRing.setVisibility(INVISIBLE);
            requestLayout();
        }
    }
    // 置监听手单击
    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mFocusManager != null) {
                //点击后  移动显示位置
                mFocusHudRing.setPosition(e.getX(), e.getY());
                //开始自动聚焦
                mFocusManager.refocus();
            }
            return super.onSingleTapConfirmed(e);
        }
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
        this.isMediaOverlay = isMediaOverlay;
        if(mRenderSurfaceView != null) {
            mRenderSurfaceView.setZOrderMediaOverlay(isMediaOverlay);
        }
    }

    /**
     *  手势探测器
     * Handles the pinch-to-zoom gesture
     */
    private class ZoomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!mIsFocusing) {
                float progress = 0;
                if (detector.getScaleFactor() > 1.0f) {
                    progress = CameraHolder.instance().cameraZoom(true);
                } else if (detector.getScaleFactor() < 1.0f) {
                    progress = CameraHolder.instance().cameraZoom(false);
                } else {
                    return false;
                }
                if(mZoomListener != null) {
                    mZoomListener.onZoomProgress(progress);
                }
            }
            return true;
        }
    }

    /**
     *  改变对焦模式的UI变化
     *
     */
    protected void changeFocusModeUI() {
        CameraData cameraData = CameraHolder.instance().getCameraData();
        if(cameraData != null && cameraData.supportTouchFocus && cameraData.touchFocusMode) {
            isFocusTouchMode = true;
            if (mFocusManager != null) {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mFocusHudRing.resetPosition();
                        mFocusManager.refocus();
                    }
                }, 1000);
            }
        } else {
            isFocusTouchMode = false;
            mFocusHudRing.setVisibility(INVISIBLE);
        }
    }

    /**
     *  事件代理
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isFocusTouchMode) {
            return mGestureDetector.onTouchEvent(event) || mZoomGestureDetector.onTouchEvent(event);
        } else {
            return mZoomGestureDetector.onTouchEvent(event);
        }
    }

    /**
     *  测量
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if(widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.AT_MOST) {
            heightSpecSize = (int)(widthSpecSize / mAspectRatio);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSpecSize,
                    MeasureSpec.EXACTLY);
        } else if(widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
            widthSpecSize = (int)(heightSpecSize * mAspectRatio);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
                    MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
