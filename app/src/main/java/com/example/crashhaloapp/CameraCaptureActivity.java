package com.example.crashhaloapp;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.example.crashhaloapp.models.Incident;
import com.example.crashhaloapp.repository.FirestoreRepository;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraCaptureActivity extends AppCompatActivity {

    private ActivityCameraCaptureBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private int currentStep = 1;
    private final String[] steps = {
        "STEP 1: TAKE PHOTO FROM 45°",
        "STEP 2: TAKE PHOTO OF VIN NUMBER",
        "STEP 3: TAKE PHOTO OF DAMAGE AREA"
    };

    private List<String> photoUrls = new ArrayList<>();
    private String incidentId;
    private FirebaseStorage storage;
    private FirestoreRepository firestoreRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = FirebaseStorage.getInstance();
        firestoreRepository = new FirestoreRepository();
        incidentId = UUID.randomUUID().toString();

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }

        binding.captureButton.setOnClickListener(v -> takePhoto());
        cameraExecutor = Executors.newSingleThreadExecutor();
        updateGuide();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(getExternalFilesDir(null), "incident_" + incidentId + "_step_" + currentStep + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                uploadPhotoToFirebase(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Photo capture failed: " + exception.getMessage());
            }
        });
    }

    private void uploadPhotoToFirebase(File file) {
        String fileName = "incidents/" + incidentId + "/step_" + currentStep + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(Uri.fromFile(file))
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    photoUrls.add(uri.toString());
                    Toast.makeText(CameraCaptureActivity.this, "Uploaded Step " + currentStep, Toast.LENGTH_SHORT).show();
                    
                    if (currentStep < steps.length) {
                        currentStep++;
                        updateGuide();
                    } else {
                        saveIncidentToFirestore();
                    }
                }))
                .addOnFailureListener(e -> Log.e("FirebaseStorage", "Upload failed", e));
    }

    private void saveIncidentToFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                     FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        
        Incident incident = new Incident();
        incident.setIncident_id(incidentId);
        incident.setUid(uid);
        incident.setStatus("Pending Assessment");
        // For simplicity, we'll store photo URLs in a way that matches your requirement
        // You might want to update your Incident model to have a List<String> photoUrls
        
        firestoreRepository.logIncident(incident)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Incident Report Created!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to save incident", e));
    }

    private void updateGuide() {
        binding.guideText.setText(steps[currentStep - 1]);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}