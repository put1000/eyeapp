package com.yx.eyeapp.model;

/**
 * 图像帧源数据，描述图像的信息
 */
public class FrameMetadata {
    private final int width;
    private final int height;
    private final int rotation;


    public FrameMetadata(int width, int height, int rotation) {
        this.height = height;
        this.width = width;
        this.rotation = rotation;
    }
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotation() {
        return rotation;
    }

    public static class Builder {
        private int width;
        private int height;
        private int rotation;

        public Builder setWidth(int width){
            this.width = width;
            return this;
        }

        public Builder setHeight(int height){
            this.height = height;
            return this;
        }

        public Builder setRotation(int rotation){
            this.rotation = rotation;
            return this;
        }

        public FrameMetadata build() {
            return new FrameMetadata(width, height, rotation);
        }
    }
}
