package com.example.crashhaloapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.crashhaloapp.databinding.ActivitySettingsBinding;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.repository.AuthRepository;
import com.example.crashhaloapp.utils.LocalDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private ActivitySettingsBinding binding;
    private LocalDatabase localDatabase;
    private AuthRepository authRepository;
    private String currentUid;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDatabase = new LocalDatabase(this);
        authRepository = new AuthRepository(this);

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
                        saveProfileImageLocally(uri);
                    }
                }
        );

        binding.imgProfileBig.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileManagementActivity.class);
            startActivity(intent);
        });

        binding.btnClearCache.setOnClickListener(v -> 
                Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show());

        binding.btnLogout.setOnClickListener(v -> {
            authRepository.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        User user = localDatabase.getUser();
        if (user != null) {
            currentUid = user.getUid();
            binding.txtProfileEmail.setText(user.getEmail());
            binding.txtProfileName.setText(user.getFull_name());
            
            if (user.getProfile_image_url() != null) {
                File imgFile = new File(user.getProfile_image_url());
                if (imgFile.exists()) {
                    Glide.with(this)
                            .load(imgFile)
                            .placeholder(R.drawable.ic_user)
                            .into(binding.imgProfileBig);
                }
            }
        } else {
            binding.txtProfileName.setText("Guest User");
            binding.txtProfileEmail.setText("No email found");
        }
    }

    private void saveProfileImageLocally(Uri uri) {
        if (currentUid == null) {
            currentUid = "local_user";
        }
        
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
        if (user == null) {
            user = new User();
            user.setUid("local_user");
        }
        
        user.setProfile_image_url(localPath);
        localDatabase.saveUser(user);
        
        Glide.with(this).load(new File(localPath)).into(binding.imgProfileBig);
        Toast.makeText(this, "Photo Updated Locally", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        updateIcon(binding.statusCamera, Manifest.permission.CAMERA);
        
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
