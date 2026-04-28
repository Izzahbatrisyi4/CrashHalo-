package com.example.crashhaloapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.crashhaloapp.databinding.ActivityCameraCaptureBinding;
import com.example.crashhaloapp.ml.YoloDetector;
import com.example.crashhaloapp.models.Detection;
import com.example.crashhaloapp.models.Incident;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.utils.LocalDatabase;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraCaptureActivity extends AppCompatActivity {

    private static final String TAG = "CameraCaptureActivity";
    private ActivityCameraCaptureBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private int currentStep = 1;
    private final String[] steps = {
        "STEP 1: TAKE PHOTO FROM 45°",
        "STEP 2: TAKE PHOTO OF VIN NUMBER",
        "STEP 3: TAKE PHOTO OF DAMAGE AREA"
    };

    private List<String> photoPaths = new ArrayList<>();
    private List<Detection> aiDetections = new ArrayList<>();
    private String incidentId;
    private LocalDatabase localDatabase;
    private YoloDetector detector;
    private boolean isCameraReady = false;

    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDatabase = new LocalDatabase(this);
        incidentId = UUID.randomUUID().toString();

        try {
            detector = new YoloDetector(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load AI model", e);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleGalleryImage(uri);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }

        binding.captureButton.setOnClickListener(v -> {
            if (isCameraReady) {
                takePhoto();
            } else {
                Toast.makeText(this, "Initializing camera, please wait...", Toast.LENGTH_SHORT).show();
            }
        });

        binding.galleryButton.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        
        updateGuide();
    }

    private void handleGalleryImage(Uri uri) {
        File outputDir = getExternalFilesDir(null); 
        if (outputDir == null) return;
        
        File photoFile = new File(outputDir, "incident_" + incidentId + "_step_" + currentStep + ".jpg");
        
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(photoFile)) {
            
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            
            saveLocalPath(photoFile);
            
            if (currentStep == 3) {
                cameraExecutor.execute(() -> runAiInference(photoFile));
            }
            
            advanceStep();
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving gallery image", e);
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLocalPath(File file) {
        photoPaths.add(file.getAbsolutePath());
        Toast.makeText(this, "Step " + currentStep + " Saved Locally", Toast.LENGTH_SHORT).show();
    }

    private void advanceStep() {
        if (currentStep < steps.length) {
            currentStep++;
            updateGuide();
        } else {
            saveIncidentLocally();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                
                isCameraReady = true;
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraX failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        File outputDir = getExternalFilesDir(null); 
        if (outputDir == null) return;
        
        File photoFile = new File(outputDir, "incident_" + incidentId + "_step_" + currentStep + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                saveLocalPath(photoFile);
                if (currentStep == 3) {
                    cameraExecutor.execute(() -> runAiInference(photoFile));
                }
                advanceStep();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                String error = "Capture Failed: " + exception.getMessage();
                Log.e(TAG, error, exception);
                Toast.makeText(CameraCaptureActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void runAiInference(File file) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                List<YoloDetector.Recognition> results = detector.detect(bitmap);
                for (YoloDetector.Recognition r : results) {
                    aiDetections.add(new Detection(r.label, r.confidence));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "AI Error", e);
        }
    }

    private void saveIncidentLocally() {
        User currentUser = localDatabase.getUser();
        String uid = (currentUser != null) ? currentUser.getUid() : "local_user";
        
        Incident incident = new Incident();
        incident.setIncident_id(incidentId);
        incident.setUid(uid);
        incident.setImages(new ArrayList<>(photoPaths)); 
        incident.setDetections(new ArrayList<>(aiDetections));
        incident.setTimestamp(System.currentTimeMillis());
        incident.setStatus("Assessment Complete (Local)");
        
        localDatabase.saveIncident(incident);
        Toast.makeText(this, "Report Saved Locally!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateGuide() {
        binding.guideText.setText(steps[currentStep - 1]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else if (requestCode == 10) {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
