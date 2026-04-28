package com.example.crashhaloapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crashhaloapp.databinding.ActivitySignupBinding;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.utils.LocalDatabase;

import java.util.UUID;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private LocalDatabase localDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDatabase = new LocalDatabase(this);

        binding.btnSignup.setOnClickListener(v -> {
            String name = binding.editName.getText().toString().trim();
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Local-only Signup: Save to internal JSON storage
            String uid = UUID.randomUUID().toString();
            User newUser = new User(uid, name, email, ""); // Initializing with empty phone
            newUser.setCreated_at(System.currentTimeMillis());
            
            localDatabase.saveUser(newUser);
            
            Toast.makeText(this, "Account Created Locally!", Toast.LENGTH_SHORT).show();
            
            // Navigate to Home
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.txtLogin.setOnClickListener(v -> finish());
    }
}
