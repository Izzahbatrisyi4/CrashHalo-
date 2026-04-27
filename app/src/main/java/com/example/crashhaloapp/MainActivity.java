package com.example.crashhaloapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.crashhaloapp.databinding.ActivityMainBinding;
import com.example.crashhaloapp.repository.AuthRepository;
import com.example.crashhaloapp.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AuthRepository authRepository;
    private FirestoreRepository firestoreRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();
        firestoreRepository = new FirestoreRepository();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadUserProfileImage();

        binding.imgProfileTop.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraCaptureActivity.class);
            startActivity(intent);
        });

        binding.btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WorkshopMapActivity.class);
            startActivity(intent);
        });

        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnHistory.setOnClickListener(v -> {
            // Scroll to top to show the incident list (which is the history)
            binding.scrollView.smoothScrollTo(0, 0);
            Toast.makeText(this, "Refreshing History...", Toast.LENGTH_SHORT).show();
            // You can also add a logic here to re-fetch the Firestore data
        });

        binding.btnHome.setOnClickListener(v -> {
            binding.scrollView.smoothScrollTo(0, 0);
        });
    }

    private void loadUserProfileImage() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            firestoreRepository.getUser(user.getUid()).addOnSuccessListener(userModel -> {
                if (userModel != null && userModel.getProfile_image_url() != null) {
                    Glide.with(this)
                            .load(userModel.getProfile_image_url())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(binding.imgProfileTop);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfileImage();
    }
}