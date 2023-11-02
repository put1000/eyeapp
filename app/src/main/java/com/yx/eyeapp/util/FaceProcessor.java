package com.yx.eyeapp.util;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.yx.eyeapp.BackgroundProcessService;
import com.yx.eyeapp.GraphicOverlay;
import com.yx.eyeapp.graphic.CameraGraphic;
import com.yx.eyeapp.graphic.FaceGraphic;
import com.yx.eyeapp.model.FaceProfile;
import com.yx.eyeapp.model.FrameMetadata;
import com.yx.eyeapp.model.TheFaceProfile;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 人脸检测器，对输入图像进行各种处理
 */
public class FaceProcessor {

    private FaceDetector faceDetector;
    private Bitmap bitmap;

    private BackgroundProcessService service;
    @GuardedBy("this")
    private ByteBuffer processingImage;
    @GuardedBy("this")
    private FrameMetadata processingMetadata;
    private final ScopedExecutor executor;
    private Object detecteLock = new Object();
    private static final String TAG = "FaceProcessor";
    private boolean isSetFaceProfile = false;
    private final TheFaceProfile theFaceProfile = TheFaceProfile.getInstance();

    public FaceProcessor(){
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        faceDetector = FaceDetection.getClient(options);
        executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    }

    public FaceProcessor(BackgroundProcessService service){
        this();
        this.service = service;
    }




    @SuppressLint("SuspiciousIndentation")
    public synchronized void processBytebuffer(
            ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay)
        {
        if((processingImage != null || processingMetadata != null) && !isSetFaceProfile) {
            Log.d(TAG, "processBytebuffer: isprocessing");
            return;
        }

        processingImage = data;
        processingMetadata = frameMetadata;
        bitmap = BitmapUtils.getBitmap(data, frameMetadata);

        InputImage image = InputImage.fromByteBuffer(
                data,
                frameMetadata.getWidth(),
                frameMetadata.getHeight(),
                frameMetadata.getRotation(),
                InputImage.IMAGE_FORMAT_NV21);

        if(graphicOverlay != null){
            Log.d(TAG, "processBytebuffer: 1");
            detectInImage(image).addOnSuccessListener(executor, result -> {
                        onSuccess(result, graphicOverlay);
                        graphicOverlay.redraw();
                        processingImage = null;
                        processingMetadata = null;
                    });
        }else if(!isSetFaceProfile){
//            Log.d(TAG, "processBytebuffer: 2");
            detectInImage(image).addOnSuccessListener(result -> {
                        compareWithProfile(result);
                        processingImage = null;
                        processingMetadata = null;
                
                    });
        }else {
//            Log.d(TAG, "3");
            detectInImage(image).addOnSuccessListener(result -> {
                setProfile(result);
                processingImage = null;
                processingMetadata = null;
            });
        }
    }

    protected Task<List<Face>> detectInImage(InputImage image) {
        synchronized (detecteLock){
            return faceDetector.process(image);
        }
    }
    protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay overlay){
        overlay.clear();
        overlay.add(new CameraGraphic(bitmap, overlay));
        for(Face face : faces){
            overlay.add(new FaceGraphic(face, overlay));
        }
    }

    protected  void compareWithProfile(@NonNull List<Face> faces){
        if(faces.isEmpty()) return;
        FaceProfile profile = new FaceProfile(faces.get(0));
        if(profile.compareTo(theFaceProfile.getFaceProfile()) > 0){
            service.showToast();
        }else {
            service.stopToast();
        }

    }

    public void setProfile(@NonNull List<Face> faces) {
        if(!faces.isEmpty()){
            theFaceProfile.setFaceProfile(faces.get(0));
        }
        Log.d(TAG, "null");
    }

    public void settingTheFaceProfile(boolean isSetProfile){
        isSetFaceProfile = isSetProfile;
        Log.d(TAG, "settingTheFaceProfile: " + isSetFaceProfile);
    }
}
