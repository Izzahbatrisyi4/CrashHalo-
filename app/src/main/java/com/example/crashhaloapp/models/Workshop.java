package com.example.crashhaloapp.models;

import com.google.firebase.firestore.GeoPoint;

public class Workshop {
    private String id;
    private String name;
    private String address;
    private String phone;
    private GeoPoint location;

    public Workshop() {}

    public Workshop(String id, String name, String address, String phone, GeoPoint location) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.location = location;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }
}