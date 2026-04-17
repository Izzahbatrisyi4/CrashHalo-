package com.example.crashhaloapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.crashhaloapp.databinding.ActivitySettingsBinding;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.repository.AuthRepository;
import com.example.crashhaloapp.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private FirestoreRepository firestoreRepository;
    private AuthRepository authRepository;
    private String currentUid;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreRepository = new FirestoreRepository();
        authRepository = new AuthRepository();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadUserProfile();

        // Initialize Image Picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
        );

        binding.imgProfileBig.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnEditProfile.setOnClickListener(v -> showEditNameDialog());

        binding.switchAutoAlert.setOnCheckedChangeListener((buttonView, isChecked) -> 
                Toast.makeText(this, "Auto-Alert: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        binding.btnClearCache.setOnClickListener(v -> 
                Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show());
    }

    private void loadUserProfile() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            currentUid = user.getUid();
            binding.txtProfileEmail.setText(user.getEmail());
            
            firestoreRepository.getUser(currentUid).addOnSuccessListener(userModel -> {
                if (userModel != null) {
                    binding.txtProfileName.setText(userModel.getFull_name());
                    if (userModel.getProfile_image_url() != null) {
                        Glide.with(this)
                                .load(userModel.getProfile_image_url())
                                .placeholder(R.drawable.ic_user)
                                .into(binding.imgProfileBig);
                    }
                } else {
                    binding.txtProfileName.setText("Guest User");
                }
            });
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        if (currentUid == null) return;
        
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUid + ".jpg");

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> 
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                updateProfileImageUrl(downloadUrl);
            })
        ).addOnFailureListener(e -> Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show());
    }

    private void updateProfileImageUrl(String url) {
        firestoreRepository.updateUserProfileImage(currentUid, url)
                .addOnSuccessListener(aVoid -> {
                    Glide.with(this).load(url).into(binding.imgProfileBig);
                    Toast.makeText(this, "Photo Updated", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_name, null);
        EditText editName = view.findViewById(R.id.edit_full_name);
        
        editName.setText(binding.txtProfileName.getText().toString());
        
        builder.setView(view)
                .setTitle("Edit Name")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateName(String newName) {
        if (currentUid == null) return;
        
        firestoreRepository.updateUserName(currentUid, newName)
                .addOnSuccessListener(aVoid -> {
                    binding.txtProfileName.setText(newName);
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        updateIcon(binding.statusCamera, Manifest.permission.CAMERA);
        updateIcon(binding.statusLocation, Manifest.permission.ACCESS_FINE_LOCATION);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateIcon(binding.statusStorage, Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            updateIcon(binding.statusStorage, Manifest.permission.READ_EXTERNAL_STORAGE);
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