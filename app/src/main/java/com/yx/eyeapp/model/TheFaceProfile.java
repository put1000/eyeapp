package com.yx.eyeapp.model;


import android.util.Log;

import com.google.mlkit.vision.face.Face;

/**
 * 预设的人脸信息，采用单例模式，与采样的人脸进行比较从而确定距离
 */
public class TheFaceProfile {
    private static String TAG = "TheProfileImage";
    private static TheFaceProfile instance;
    private static FaceProfile faceProfile;

    private TheFaceProfile(){
    };
    public synchronized static TheFaceProfile getInstance() {
        if (instance == null) {
            instance = new TheFaceProfile();
        }
        return instance;
    }
    public void setFaceProfile(Face face) {
        faceProfile = new FaceProfile(face);
        Log.d(TAG, "setFaceProfile: height:" + faceProfile.getHeight() + " width:" + faceProfile.getWidth() + " area:" + faceProfile.getArea());

    }
    public FaceProfile getFaceProfile() {
        return faceProfile;
    }
}
