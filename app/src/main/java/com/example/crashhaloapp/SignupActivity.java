package com.example.crashhaloapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crashhaloapp.databinding.ActivitySignupBinding;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.repository.AuthRepository;
import com.example.crashhaloapp.repository.FirestoreRepository;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private ActivitySignupBinding binding;
    private AuthRepository authRepository;
    private FirestoreRepository firestoreRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();
        firestoreRepository = new FirestoreRepository();

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

            authRepository.signUp(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        User newUser = new User(uid, name, email, "");
                        firestoreRepository.saveUser(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore Error: ", e);
                                    Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        String errorMsg = "Signup Failed";
                        if (e instanceof FirebaseAuthException) {
                            errorMsg += " Code: " + ((FirebaseAuthException) e).getErrorCode();
                        } else if (e instanceof FirebaseNetworkException) {
                            errorMsg += ": Network error. Check internet.";
                        } else {
                            errorMsg += ": " + e.getLocalizedMessage();
                        }
                        
                        Log.e(TAG, "Auth Error Details: ", e);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    });
        });

        binding.txtLogin.setOnClickListener(v -> finish());
    }
}