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
    private boolean isCameraReady = false;

    private ActivityResultLauncher<String> galleryLauncher;

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
            Log.e(TAG, "Failed to load AI model", e);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleGalleryImage(uri);
            }
        });

        if (allPermissionsGranted()) {
            startCamera();
            fetchCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
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
            
            Toast.makeText(this, "Step " + currentStep + " Selected", Toast.LENGTH_SHORT).show();
            
            if (currentStep == 3) {
                cameraExecutor.execute(() -> runAiInference(photoFile));
            }
            uploadPhotoToFirebase(photoFile);
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving gallery image", e);
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    crashLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
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
                Toast.makeText(CameraCaptureActivity.this, "Step " + currentStep + " Captured", Toast.LENGTH_SHORT).show();
                if (currentStep == 3) {
                    cameraExecutor.execute(() -> runAiInference(photoFile));
                }
                uploadPhotoToFirebase(photoFile);
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

    private void uploadPhotoToFirebase(File file) {
        String fileName = "incidents/" + incidentId + "/step_" + currentStep + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(Uri.fromFile(file))
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    photoUrls.add(uri.toString());
                    if (currentStep < steps.length) {
                        currentStep++;
                        updateGuide();
                    } else {
                        saveIncidentToFirestore();
                    }
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(this, "Storage Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        
        firestoreRepository.logIncident(incident)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report Saved!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show());
    }

    private void updateGuide() {
        binding.guideText.setText(steps[currentStep - 1]);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && allPermissionsGranted()) {
            startCamera();
            fetchCurrentLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}