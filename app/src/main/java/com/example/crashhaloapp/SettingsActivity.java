package com.example.crashhaloapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crashhaloapp.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Simple feedback for demo
        binding.switchAutoAlert.setOnCheckedChangeListener((buttonView, isChecked) -> 
                Toast.makeText(this, "Auto-Alert: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        binding.btnClearCache.setOnClickListener(v -> 
                Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show());
    }
}