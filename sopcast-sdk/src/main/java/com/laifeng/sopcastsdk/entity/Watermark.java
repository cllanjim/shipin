package com.laifeng.sopcastsdk.entity;

import android.graphics.Bitmap;

/**
 * @Title: Watermark
 * @Package com.laifeng.sopcastsdk.video
 * @Description:  水印
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午2:32
 * @Version
 */
public class Watermark {
    // 图片
    public Bitmap markImg;
    // 宽
    public int width;
    // 高
    public int height;
    // 方位（左上 右上 ....）
    public int orientation;
    // 垂直的Margin
    public int vMargin;
    // 水平的Margin
    public int hMargin;

    public Watermark(Bitmap img, int width, int height, int orientation, int vmargin, int hmargin) {
        markImg = img;
        this.width = width;
        this.height = height;
        this.orientation = orientation;
        vMargin = vmargin;
        hMargin = hmargin;
    }
}
