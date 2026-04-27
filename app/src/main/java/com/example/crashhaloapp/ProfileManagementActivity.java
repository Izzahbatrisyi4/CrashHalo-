package com.example.crashhaloapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.crashhaloapp.databinding.ActivityProfileManagementBinding;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.models.Vehicle;
import com.example.crashhaloapp.repository.AuthRepository;
import com.example.crashhaloapp.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileManagementActivity extends AppCompatActivity {

    private ActivityProfileManagementBinding binding;
    private FirestoreRepository firestoreRepository;
    private AuthRepository authRepository;
    private String currentUid;
    private String currentVid;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreRepository = new FirestoreRepository();
        authRepository = new AuthRepository();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Image Picker Logic
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
        );

        binding.btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            loadUserData();
            loadVehicleData();
        }

        binding.btnSaveProfile.setOnClickListener(v -> saveProfileData());
        binding.btnSaveCar.setOnClickListener(v -> saveVehicleData());
    }

    private void loadUserData() {
        firestoreRepository.getUser(currentUid).addOnSuccessListener(user -> {
            if (user != null) {
                binding.editFullName.setText(user.getFull_name());
                binding.editPhone.setText(user.getPhone());
                binding.editAddress.setText(user.getHome_address());
                if (user.getProfile_image_url() != null) {
                    Glide.with(this).load(user.getProfile_image_url()).into(binding.imgProfileManage);
                }
            }
        });
    }

    private void loadVehicleData() {
        firestoreRepository.getVehiclesForUser(currentUid).addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                Vehicle vehicle = querySnapshot.toObjects(Vehicle.class).get(0);
                currentVid = vehicle.getVid();
                binding.editPlate.setText(vehicle.getPlate());
                binding.editCarType.setText(vehicle.getMake());
                binding.editCarModel.setText(vehicle.getModel());
            }
        });
    }

    private void uploadProfileImage(Uri imageUri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUid + ".jpg");

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> 
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                firestoreRepository.updateUserProfileImage(currentUid, downloadUrl)
                        .addOnSuccessListener(aVoid -> {
                            Glide.with(this).load(downloadUrl).into(binding.imgProfileManage);
                            Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show();
                        });
            })
        ).addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileData() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", binding.editFullName.getText().toString().trim());
        updates.put("phone", binding.editPhone.getText().toString().trim());
        updates.put("home_address", binding.editAddress.getText().toString().trim());

        firestoreRepository.updateUserProfile(currentUid, updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void saveVehicleData() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("plate", binding.editPlate.getText().toString().trim());
        updates.put("make", binding.editCarType.getText().toString().trim());
        updates.put("model", binding.editCarModel.getText().toString().trim());

        if (currentVid != null) {
            firestoreRepository.updateVehicle(currentVid, updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Car details updated", Toast.LENGTH_SHORT).show());
        } else {
            Vehicle newVehicle = new Vehicle(null, currentUid, 
                    binding.editCarType.getText().toString().trim(),
                    binding.editCarModel.getText().toString().trim(),
                    0, 
                    binding.editPlate.getText().toString().trim());
            firestoreRepository.saveVehicle(newVehicle)
                    .addOnSuccessListener(aVoid -> {
                        currentVid = newVehicle.getVid();
                        Toast.makeText(this, "Car details saved", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}