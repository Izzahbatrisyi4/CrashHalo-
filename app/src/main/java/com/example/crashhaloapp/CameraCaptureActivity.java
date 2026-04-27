package com.example.crashhaloapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.crashhaloapp.databinding.ActivityCameraCaptureBinding;
import com.example.crashhaloapp.ml.YoloDetector;
import com.example.crashhaloapp.models.Detection;
import com.example.crashhaloapp.models.Incident;
import com.example.crashhaloapp.repository.FirestoreRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
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
    private FusedLocationProviderClient fusedLocationClient;

    private int currentStep = 1;
    private final String[] steps = {
        "STEP 1: TAKE PHOTO FROM 45°",
        "STEP 2: TAKE PHOTO OF VIN NUMBER",
        "STEP 3: TAKE PHOTO OF DAMAGE AREA"
    };

    private List<String> photoUrls = new ArrayList<>();
    private List<Detection> aiDetections = new ArrayList<>();
    private String incidentId;
    private GeoPoint crashLocation;
    private FirebaseStorage storage;
    private FirestoreRepository firestoreRepository;
    private YoloDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = FirebaseStorage.getInstance();
        firestoreRepository = new FirestoreRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        incidentId = UUID.randomUUID().toString();

        try {
            detector = new YoloDetector(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model", e);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Request camera and location permissions
        if (allPermissionsGranted()) {
            startCamera();
            fetchCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }

        binding.captureButton.setOnClickListener(v -> takePhoto());
        updateGuide();
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    crashLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "Location fetched: " + location.getLatitude() + ", " + location.getLongitude());
                }
            });
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
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                Log.d(TAG, "Camera started and bound to lifecycle");

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null, camera might not be ready");
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDir = getExternalFilesDir(null);
        if (outputDir == null) {
            Log.e(TAG, "External files directory is null");
            return;
        }
        
        File photoFile = new File(outputDir, "incident_" + incidentId + "_step_" + currentStep + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        Log.d(TAG, "Taking photo for step " + currentStep);
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.d(TAG, "Photo saved successfully: " + photoFile.getAbsolutePath());
                Toast.makeText(CameraCaptureActivity.this, "Step " + currentStep + " captured", Toast.LENGTH_SHORT).show();
                
                if (currentStep == 3) {
                    cameraExecutor.execute(() -> runAiInference(photoFile));
                }
                uploadPhotoToFirebase(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                Toast.makeText(CameraCaptureActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Log.d(TAG, "AI Detected: " + r.label + " with confidence " + r.confidence);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "AI Inference failed", e);
        }
    }

    private void uploadPhotoToFirebase(File file) {
        String fileName = "incidents/" + incidentId + "/step_" + currentStep + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        Log.d(TAG, "Uploading photo to Firebase: " + fileName);
        storageRef.putFile(Uri.fromFile(file))
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    photoUrls.add(uri.toString());
                    Log.d(TAG, "Upload successful, URL: " + uri.toString());
                    if (currentStep < steps.length) {
                        currentStep++;
                        updateGuide();
                    } else {
                        saveIncidentToFirestore();
                    }
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase Upload failed", e);
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveIncidentToFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                     FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        
        Incident incident = new Incident();
        incident.setIncident_id(incidentId);
        incident.setUid(uid);
        incident.setImages(photoUrls);
        incident.setDetections(aiDetections);
        incident.setLocation(crashLocation);
        incident.setStatus("Assessment Complete");
        
        Log.d(TAG, "Saving incident to Firestore");
        firestoreRepository.logIncident(incident)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Incident saved successfully");
                    Toast.makeText(this, "AI Assessment Complete!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save incident", e));
    }

    private void updateGuide() {
        binding.guideText.setText(steps[currentStep - 1]);
    }

    private boolean allPermissionsGranted() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Permissions - Camera: " + cameraGranted + ", Location: " + locationGranted);
        return cameraGranted && locationGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (allPermissionsGranted()) {
                startCamera();
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}