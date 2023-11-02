package com.yx.eyeapp.model;

import com.google.mlkit.vision.face.Face;

/**
 * 人脸信息，实现了Cmopareble接口以便于比较(测距）
 */
public class FaceProfile implements Comparable<FaceProfile>{
    private  float width;
    private  float height;

    private float area;
    private static final String TAG = "FaceProfile";

    public FaceProfile(Face face){
        this.height = face.getBoundingBox().height();
        this.width = face.getBoundingBox().width();
        this.area = height * width;
    }

    @Override
    public int compareTo(FaceProfile o) {

        return (this.width > o.getWidth() ||
                this.height > o.getHeight() ||
                this.area > o.getArea()) ? 1 : 0;
    }
    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public float getArea() {
        return area;
    }

}
