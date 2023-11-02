package com.yx.eyeapp;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.yx.eyeapp.util.BitmapUtils;
import com.yx.eyeapp.util.FaceProcessor;
import com.yx.eyeapp.model.FrameMetadata;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 在服务中进行人脸采样，并于预设人脸信息比较，根据比较结果显示提示信息
 * 明明已经设置为前台服务了，但是切换到其他app时，检测能够正确进行，
 * 日志中能正常的打印检测结果，但是toast提示信息有时不显示？？
 *
 * 实现与CmaeraSourc类似
 */
public class BackgroundProcessService extends Service {
    private final String TAG = "BackgroundProcessService";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private final int IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private FaceProcessor faceProcessor;
    private CameraDevice opened_camera;
    private ImageReader imageReader;
    private Handler handler;
    private ProcessRunnable runnable;
    private Thread processThread;
    private final Object processLock = new Object();
    //检测时间间隔
    private static final long INTERVAL = 300;
    private boolean shouldShowToast = false;
    private Toast currentToast;
    private Context context;


    /**
     * 人脸检测线程
     */
    private class ProcessRunnable implements Runnable{
        private final Object lock = new Object();
        private boolean activy;
        private ByteBuffer frameData;

        public void setNextFrameData(ByteBuffer data){
            synchronized(lock){
                if(frameData != null){
                    frameData = null;
                    Log.d(TAG, "update the frmeData");
                }
                frameData = data;
                lock.notifyAll();
            }
        }
        @Override
        public void run() {
            ByteBuffer data;

            while (true) {
                synchronized (lock) {
                    //等待帧数据更新
                    while (activy && frameData == null) {
                        try {
                            lock.wait();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!activy) return;

                    data = frameData;
                    frameData = null;
                }
                //处理图像帧数据
                try {
                    synchronized (processLock) {
                        Log.d(TAG, "run: processing");
                        faceProcessor.processBytebuffer(data, new FrameMetadata.Builder()
                                .setWidth(600)
                                .setHeight(600)
                                .setRotation(270)
                                .build(), null);
                        Log.d(TAG, "run: process done");
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

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = createNotification();
        startForeground(1, notification);
        // 创建 Handler 对象
        handler = new Handler();

        // 创建检测线程对象
        runnable = new ProcessRunnable();
        processThread = new Thread(runnable);
        context = this;
    }



    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // 初始化相机源和人脸检测器
        faceProcessor = new FaceProcessor(this);
        createCamera();
        //开始检测
        runnable.setActivy(true);
        processThread.start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //打开相机
    @RequiresPermission(Manifest.permission.CAMERA)
    private  void createCamera(){
        Log.d(TAG, "createCamera: ");
        CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
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

            //打开相机
            cameraManager.openCamera(frontCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    opened_camera = camera;
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
    }

    //开始捕获图像
    private void startPreview(){
        //设置ImageReader
        imageReader = ImageReader.newInstance(600
                ,600
                ,IMAGE_FORMAT
                ,3);
        Log.d(TAG, "imagereader width:" + imageReader.getWidth() + "  " + imageReader.getHeight());
        //创建会话
        try {
            CaptureRequest.Builder requestBuilder = opened_camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(imageReader.getSurface());
            opened_camera.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                // 开启预览
                                Log.d(TAG, "startPreview: preview session is created.");
                                session.setRepeatingRequest(requestBuilder.build(), null, null);
                                // 添加定时发送照片的任务
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendImage();
                                        // 每0.3秒发送一张照片
                                        handler.postDelayed(this, INTERVAL);
                                    }

                                }, INTERVAL);
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
     * 更新图像帧
     */
    private void sendImage(){
        Image image = imageReader.acquireLatestImage();
        if (image == null) return;
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = BitmapUtils.yuv420ThreePlanesToNV21(planes, 600, 600);
        runnable.setNextFrameData(buffer);
        image.close();
    }


    public void showToast(){
        if (currentToast == null){
            Log.d(TAG, "compareWithProfile: =====================嘿，离远点！================");
            currentToast = Toast.makeText(context, "嘿，离远点！", Toast.LENGTH_SHORT);
            Log.d(TAG, "showToast: =================================================================");
            currentToast.show();
            distoryToast();
        }

    }
    private void distoryToast() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentToast  != null)
                    currentToast = null;
            }
        }, 2000);
    }

    public void stopToast(){
        if(currentToast != null){
            Log.d(TAG, "compareWithProfile: ============stop show the toast================");
            currentToast.cancel();
            currentToast = null;
        }
    }

    //创建tian
    private Notification createNotification() {
        // 创建通知渠道（适用于 Android 8.0 及更高版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // 创建前台服务通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("eyeapp")
                .setContentText("服务正在运行")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
    }

}
