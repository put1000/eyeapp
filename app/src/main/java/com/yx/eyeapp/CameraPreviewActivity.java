package com.yx.eyeapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yx.eyeapp.model.FrameMetadata;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraPreviewActivity extends AppCompatActivity {
    private CameraSource cameraSource;
    private GraphicOverlay graphicOverlay;
    private final String TAG = "CameraPreviewActivity";
    private Button btn_take;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        Log.d(TAG, "onCreate: " + TAG);

        graphicOverlay = findViewById(R.id.graphic_view);
        cameraSource = new CameraSource(this, graphicOverlay);
        btn_take = findViewById(R.id.take_photo);

        TextView text = findViewById(R.id.text);
        text.setVisibility(View.VISIBLE);

        TextView text2 = findViewById(R.id.text2);
        text2.setVisibility(View.INVISIBLE);


        btn_take.setOnClickListener(v -> {
            ByteBuffer image = cameraSource.getPhoto();
            if (image == null) {
                Log.d(TAG, "onClick: null");
                return;
            }
            Log.d(TAG, "1");
            cameraSource.setProfile(image, new FrameMetadata(600, 600, 270));

            AlertDialog.Builder builder = new AlertDialog.Builder(CameraPreviewActivity.this);
            builder.setMessage("确定要使用该图片作为测量的阈值吗？");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                cameraSource.stop();
                Intent serviceIntent = new Intent(CameraPreviewActivity.this, BackgroundProcessService.class);

                startService(serviceIntent);
                dialog.dismiss();

                //显示服务已启动
                text2.setVisibility(View.VISIBLE);
                text.setVisibility(View.INVISIBLE);

            });
            builder.setNegativeButton("Retake", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.show();
        });


        checkPermission();
        try {
            cameraSource.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }


    
}