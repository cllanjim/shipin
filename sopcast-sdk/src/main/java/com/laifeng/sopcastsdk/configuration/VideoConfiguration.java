package com.laifeng.sopcastsdk.configuration;

/**
 * @Title: VideoConfiguration
 * @Package com.laifeng.sopcastsdk.configuration
 * @Description:  视频配置类
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午3:20
 * @Version
 * 采用建造者模式 （Builder）
 */
public final class VideoConfiguration {
    // 默认高
    public static final int DEFAULT_HEIGHT = 640;
    // 默认宽
    public static final int DEFAULT_WIDTH = 360;
    // 默认的fps（每秒中填充图像的帧数(帧/秒)）
    public static final int DEFAULT_FPS = 15;
    // 最大的bps（比特/秒）
    public static final int DEFAULT_MAX_BPS = 1300;
    // 最小的bps
    public static final int DEFAULT_MIN_BPS = 400;

    public static final int DEFAULT_IFI = 2;

    public static final String DEFAULT_MIME = "video/avc";

    public final int height;
    public final int width;
    public final int minBps;
    public final int maxBps;
    public final int fps;
    public final int ifi;
    public final String mime;

    private VideoConfiguration(final Builder builder) {
        height = builder.height;
        width = builder.width;
        minBps = builder.minBps;
        maxBps = builder.maxBps;
        fps = builder.fps;
        ifi = builder.ifi;
        mime = builder.mime;
    }

    public static VideoConfiguration createDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private int height = DEFAULT_HEIGHT;
        private int width = DEFAULT_WIDTH;
        private int minBps = DEFAULT_MIN_BPS;
        private int maxBps = DEFAULT_MAX_BPS;
        private int fps = DEFAULT_FPS;
        private int ifi = DEFAULT_IFI;
        private String mime = DEFAULT_MIME;

        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder setBps(int minBps, int maxBps) {
            this.minBps = minBps;
            this.maxBps = maxBps;
            return this;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder setIfi(int ifi) {
            this.ifi = ifi;
            return this;
        }

        public Builder setMime(String mime) {
            this.mime = mime;
            return this;
        }

        public VideoConfiguration build() {
            return new VideoConfiguration(this);
        }
    }
}
