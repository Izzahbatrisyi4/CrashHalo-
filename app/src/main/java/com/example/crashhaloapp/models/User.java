package com.example.crashhaloapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class User {
    private String uid;
    private String full_name;
    private String email;
    private String phone;
    private String profile_image_url;
    @ServerTimestamp
    private Timestamp created_at;

    public User() {} // Required for Firestore

    public User(String uid, String full_name, String email, String phone) {
        this.uid = uid;
        this.full_name = full_name;
        this.email = email;
        this.phone = phone;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProfile_image_url() { return profile_image_url; }
    public void setProfile_image_url(String profile_image_url) { this.profile_image_url = profile_image_url; }
    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }
}