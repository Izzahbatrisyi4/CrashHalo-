package com.example.crashhaloapp.models;

public class Vehicle {
    private String vid;
    private String uid; // Foreign Key to Users
    private String make;
    private String model;
    private int year;
    private String plate;

    public Vehicle() {} // Required for Firestore

    public Vehicle(String vid, String uid, String make, String model, int year, String plate) {
        this.vid = vid;
        this.uid = uid;
        this.make = make;
        this.model = model;
        this.year = year;
        this.plate = plate;
    }

    // Getters and Setters
    public String getVid() { return vid; }
    public void setVid(String vid) { this.vid = vid; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
}