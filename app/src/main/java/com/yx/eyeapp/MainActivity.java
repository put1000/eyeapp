package com.yx.eyeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
//    //TEST=========================================================START
//    private CameraSource cameraSource;
//    private GraphicOverlay graphicOverlay;
//    private CameraManager cameraManager;
//    private ImageView imageView;
//    private SurfaceView surfaceView;
//    private Surface surface;
//    //TEST=========================================================END
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        checkPermission();
//
//        //TEST=========================================================START
//        Intent intent = new Intent(MainActivity.this, CameraPreviewActivity.class);
//        startActivity(intent);
//
//        //TEST=========================================================END
//    }
//
//
//    private void checkPermission(){
//        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
//            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
//
//            }else{
//                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1);
//            }
//        }
//    }
}