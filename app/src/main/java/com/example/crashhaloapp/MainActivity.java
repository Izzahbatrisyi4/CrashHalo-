package com.example.crashhaloapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView statusCamera, statusLocation, statusStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        db = FirebaseFirestore.getInstance();

        statusCamera = findViewById(R.id.status_camera);
        statusLocation = findViewById(R.id.status_location);
        statusStorage = findViewById(R.id.status_storage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_scan).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraCaptureActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_map).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WorkshopMapActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        updateIcon(statusCamera, Manifest.permission.CAMERA);
        updateIcon(statusLocation, Manifest.permission.ACCESS_FINE_LOCATION);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateIcon(statusStorage, Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            updateIcon(statusStorage, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void updateIcon(ImageView icon, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            icon.setImageResource(android.R.drawable.presence_online);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_green)));
        } else {
            icon.setImageResource(android.R.drawable.presence_offline);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey_300)));
        }
    }
}