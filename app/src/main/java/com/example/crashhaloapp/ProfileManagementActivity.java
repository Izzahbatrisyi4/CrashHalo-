package com.example.crashhaloapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.crashhaloapp.databinding.ActivityProfileManagementBinding;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.models.Vehicle;
import com.example.crashhaloapp.utils.LocalDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class ProfileManagementActivity extends AppCompatActivity {

    private static final String TAG = "ProfileManagementActivity";
    private ActivityProfileManagementBinding binding;
    private LocalDatabase localDatabase;
    private String currentUid;
    private String currentVid;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDatabase = new LocalDatabase(this);

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
                        saveProfileImageLocally(uri);
                    }
                }
        );

        binding.btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        User user = localDatabase.getUser();
        if (user != null) {
            currentUid = user.getUid();
            loadUserData(user);
            loadVehicleData();
        } else {
            currentUid = "local_user";
        }

        binding.btnSaveProfile.setOnClickListener(v -> saveProfileData());
        binding.btnSaveCar.setOnClickListener(v -> saveVehicleData());
    }

    private void loadUserData(User user) {
        binding.editFullName.setText(user.getFull_name());
        binding.editPhone.setText(user.getPhone());
        binding.editAddress.setText(user.getHome_address());
        if (user.getProfile_image_url() != null) {
            File imgFile = new File(user.getProfile_image_url());
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).into(binding.imgProfileManage);
            }
        }
    }

    private void loadVehicleData() {
        List<Vehicle> vehicles = localDatabase.getAllVehicles();
        for (Vehicle v : vehicles) {
            if (v.getUid().equals(currentUid)) {
                currentVid = v.getVid();
                binding.editPlate.setText(v.getPlate());
                binding.editCarType.setText(v.getMake());
                binding.editCarModel.setText(v.getModel());
                break;
            }
        }
    }

    private void saveProfileImageLocally(Uri uri) {
        File outputDir = getExternalFilesDir(null);
        if (outputDir == null) return;

        File profileFile = new File(outputDir, "profile_" + currentUid + ".jpg");

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(profileFile)) {
            
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            
            updateProfileImageUrl(profileFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving profile image", e);
            Toast.makeText(this, "Failed to update photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileImageUrl(String localPath) {
        User user = localDatabase.getUser();
        if (user == null) user = new User();
        user.setProfile_image_url(localPath);
        localDatabase.saveUser(user);
        
        Glide.with(this).load(new File(localPath)).into(binding.imgProfileManage);
        Toast.makeText(this, "Photo updated locally!", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileData() {
        User user = localDatabase.getUser();
        if (user == null) user = new User();
        
        user.setFull_name(binding.editFullName.getText().toString().trim());
        user.setPhone(binding.editPhone.getText().toString().trim());
        user.setHome_address(binding.editAddress.getText().toString().trim());

        localDatabase.saveUser(user);
        Toast.makeText(this, "Profile updated locally", Toast.LENGTH_SHORT).show();
    }

    private void saveVehicleData() {
        if (currentVid == null) {
            currentVid = UUID.randomUUID().toString();
        }

        Vehicle vehicle = new Vehicle(
                currentVid,
                currentUid,
                binding.editCarType.getText().toString().trim(),
                binding.editCarModel.getText().toString().trim(),
                0,
                binding.editPlate.getText().toString().trim()
        );

        localDatabase.saveVehicle(vehicle);
        Toast.makeText(this, "Car details updated locally", Toast.LENGTH_SHORT).show();
    }
}
