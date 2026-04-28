package com.example.crashhaloapp.models;

import java.util.List;

public class Incident {
    private String incident_id;
    private String vid; // Foreign Key to Vehicles
    private String uid; // Foreign Key to Users
    private long timestamp;
    private String status;
    private String pdf_url;
    private List<Detection> detections;
    private List<String> images; // List of local file paths

    public Incident() {}

    public Incident(String incident_id, String vid, String uid, long timestamp, String status, String pdf_url, List<Detection> detections, List<String> images) {
        this.incident_id = incident_id;
        this.vid = vid;
        this.uid = uid;
        this.timestamp = timestamp;
        this.status = status;
        this.pdf_url = pdf_url;
        this.detections = detections;
        this.images = images;
    }

    // Getters and Setters
    public String getIncident_id() { return incident_id; }
    public void setIncident_id(String incident_id) { this.incident_id = incident_id; }
    public String getVid() { return vid; }
    public void setVid(String vid) { this.vid = vid; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPdf_url() { return pdf_url; }
    public void setPdf_url(String pdf_url) { this.pdf_url = pdf_url; }
    public List<Detection> getDetections() { return detections; }
    public void setDetections(List<Detection> detections) { this.detections = detections; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
}
