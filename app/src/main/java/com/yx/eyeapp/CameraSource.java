package com.yx.eyeapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.yx.eyeapp.util.BitmapUtils;
import com.yx.eyeapp.util.FaceProcessor;
import com.yx.eyeapp.model.FrameMetadata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 相机预览源，负责相机实时预览并绘制人脸检测结果。
 *
 * ImageReader接收相机预览，并更新帧数据。
 * 人脸检测线程维护一张图像帧数据，并进行实时检测。
 * GraphicOverlay绘制检测结果。
 */
public class CameraSource {
    private GraphicOverlay graphicOverlay;
    private Activity activity;
    private static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private CameraDevice opened_camera;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private Size screeenSize;
    private Thread processingThread;
    private final FrameProcessingRunnable processingRunnable;
    private FaceProcessor processor;
    private final Object processorLock = new Object();
    private static final String TAG = "CameraSource";

    public CameraSource(Activity activity, GraphicOverlay graphicOverlay){
        this.activity = activity;
        this.graphicOverlay = graphicOverlay;
        graphicOverlay.clear();
        processingRunnable = new FrameProcessingRunnable();
        processor = new FaceProcessor();
    }

    /**
     * 开始
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public synchronized CameraSource start() throws IOException{
        Log.d(TAG, "start: ");
        if(opened_camera != null) return this;
        //开启预览
        createCamera();
        //开启人脸检测
        processingThread = new Thread(processingRunnable);
        processingRunnable.setActivy(true);
        processingThread.start();
        return this;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private  void createCamera(){
        //选择前置摄像头
        Log.d(TAG, "createCamera: ");
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String frontCameraId = null;
            CameraCharacteristics characteristics = null;
            try {
                for (String cameraId : cameraManager.getCameraIdList()) {
                    characteristics = cameraManager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        frontCameraId = cameraId;
                        break;
                    }
                }
            } catch (CameraAccessException e) {
                Log.d(TAG, "createCamera: no front camera");
            }

            //获取屏幕大小
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screeenSize = new Size(size.x, size.y);

            //设置画笔缩放
            graphicOverlay.setScaleFactor(screeenSize);

            Log.d(TAG, "screen width:" + size.x + "screen height:" + size.y + "   " + (float)size.y/size.x);
            //打开相机
            cameraManager.openCamera(frontCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    opened_camera = camera;
                    Log.d(TAG, "onOpened: ");
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onDisconnected: ");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, null);

        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        Log.d(TAG, "createCamera: camera opened");
    }

    private void startPreview(){
        //设置ImageReader
        imageReader = ImageReader.newInstance(600
                ,600
                ,IMAGE_FORMAT
                ,2);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //更新图像帧数据
                Image image = reader.acquireLatestImage();
                if(image == null) return;
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = BitmapUtils.yuv420ThreePlanesToNV21(planes,600, 600);

                if(image != null) image.close();
                processingRunnable.setNextFrameData(buffer);
            }
        }, null);

        //创建会话
        try {
            CaptureRequest.Builder requestBuilder = opened_camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(imageReader.getSurface());
            opened_camera.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        //开启预览
                        Log.d(TAG, "startPreview: preview session is created.");
                        cameraCaptureSession = session;
                        session.setRepeatingRequest(requestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 设置预设图片，（用于比较确定脸部与屏幕的距离）
     */
    public void setProfile(ByteBuffer image, FrameMetadata metadata) {
        synchronized (processorLock){
            processor.settingTheFaceProfile(true);
            processor.processBytebuffer(image, metadata, null);
            processor.settingTheFaceProfile(false);
        }
        
    }

    /**
     * 人脸检测线程，维护一张图像帧数据，并对其进行检测
     */
    private class FrameProcessingRunnable implements Runnable{
        private Object lock = new Object();
        private boolean activy;
        private ByteBuffer frameData;

        //更新图像帧
        void setNextFrameData(ByteBuffer data){
            synchronized(lock){
                if(frameData != null){
                    frameData = null;
                }
                frameData = data;
                lock.notifyAll();
            }
        }
        @Override
        public void run() {
            ByteBuffer data;
            while(true){
                synchronized (lock){
                    //等待帧数据更新
                    while(activy && frameData == null){
                        try{
                            lock.wait();
                        }catch (Exception e){
                            Log.d(TAG, "Frame process lopp terminated.");
                            e.printStackTrace();
                        }
                    }
                    if(!activy) return;

                    data = frameData;
                    frameData = null;
                }
                //处理图像帧数据
                try {
                    synchronized (processorLock){
                        processor.processBytebuffer(data,
                                new FrameMetadata.Builder()
                                        .setWidth(600)
                                        .setHeight(600)
                                        .setRotation(270)
                                        .build(),
                                graphicOverlay);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        public void setActivy(boolean activy) {
            this.activy = activy;
        }
    }

    /**
     *捕获单张图片
     */
    public ByteBuffer getPhoto(){
        while(true){
            Image image = imageReader.acquireLatestImage();
            if(image != null){
                try{
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = BitmapUtils.yuv420ThreePlanesToNV21(planes,600, 600);
                    return buffer;
                }catch (Exception e){
                    e.printStackTrace();
                    return null;
                }finally {
                    image.close();
                }
            }
        }
    }

    public void stop(){

        processingRunnable.setActivy(false);

        Log.d(TAG, "stop: 3");
        if(cameraCaptureSession != null){
            try {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        Log.d(TAG, "stop: 4");
        if(opened_camera != null) {
            opened_camera.close();
            opened_camera = null;
        }

        Log.d(TAG, "stop: 5");
        graphicOverlay.clear();
    }

}
