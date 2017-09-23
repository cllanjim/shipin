package com.laifeng.sopcastsdk.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.blacklist.BlackListHelper;
import com.laifeng.sopcastsdk.utils.SopCastLog;

/**
 * @Title: VideoMediaCodec
 * @Package com.laifeng.sopcastsdk.hw
 * @Description: 视频的编码器
 * @Author Jim
 * @Date 16/6/2
 * @Time 下午6:07
 * @Version
 */
@TargetApi(18)
public class VideoMediaCodec {

    /**
     *  设置编码格式
     * @param videoConfiguration
     * @return
     */
    public static MediaCodec getVideoMediaCodec(VideoConfiguration videoConfiguration) {
        // 宽
        int videoWidth = getVideoSize(videoConfiguration.width);
        // 高
        int videoHeight = getVideoSize(videoConfiguration.height);
        // 设置格式
        MediaFormat format = MediaFormat.createVideoFormat(videoConfiguration.mime, videoWidth, videoHeight);
        //  MediaFormat 中有一个HashMap保存 键值
        // 颜色格式
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoConfiguration.maxBps * 1024);
        int fps = videoConfiguration.fps;
        //设置摄像头预览帧率  OPPO R9 和 Nexus 6P 设置帧率
        if (BlackListHelper.deviceInFpsBlacklisted()) {
            SopCastLog.d(SopCastConstant.TAG, "Device in fps setting black list, so set mediacodec fps 15");
            fps = 15;
        }
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        // 设置 I 帧 关键帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoConfiguration.ifi);
        // 设置 可变码率
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        // 设置
        format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        MediaCodec mediaCodec = null;

        try {
            mediaCodec = MediaCodec.createEncoderByType(videoConfiguration.mime);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
        }
        return mediaCodec;
    }

    // We avoid the device-specific limitations on width and height by using values that
    // are multiples of 16, which all tested devices seem to be able to handle.
    public static int getVideoSize(int size) {
        int multiple = (int) Math.ceil(size / 16.0);
        return multiple * 16;
    }
}
