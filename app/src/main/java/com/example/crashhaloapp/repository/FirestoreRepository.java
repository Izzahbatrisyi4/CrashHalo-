package com.example.crashhaloapp.repository;

import com.example.crashhaloapp.models.Incident;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.models.Vehicle;
import com.example.crashhaloapp.models.Workshop;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class FirestoreRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("Users");
    private final CollectionReference vehiclesRef = db.collection("Vehicles");
    private final CollectionReference incidentsRef = db.collection("Incidents");
    private final CollectionReference workshopsRef = db.collection("Workshops");

    // --- USER OPERATIONS ---
    public Task<Void> saveUser(User user) {
        return usersRef.document(user.getUid()).set(user);
    }

    public Task<Void> updateUserProfile(String uid, Map<String, Object> updates) {
        return usersRef.document(uid).update(updates);
    }

    public Task<Void> updateUserName(String uid, String newName) {
        return usersRef.document(uid).update("full_name", newName);
    }

    public Task<Void> updateUserProfileImage(String uid, String imageUrl) {
        return usersRef.document(uid).update("profile_image_url", imageUrl);
    }

    public Task<User> getUser(String uid) {
        return usersRef.document(uid).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                return task.getResult().toObject(User.class);
            }
            return null;
        });
    }

    // --- VEHICLE OPERATIONS ---
    public Task<Void> saveVehicle(Vehicle vehicle) {
        String vid = vehicle.getVid() != null ? vehicle.getVid() : vehiclesRef.document().getId();
        vehicle.setVid(vid);
        return vehiclesRef.document(vid).set(vehicle);
    }

    public Task<Void> updateVehicle(String vid, Map<String, Object> updates) {
        return vehiclesRef.document(vid).update(updates);
    }

    public Task<QuerySnapshot> getVehiclesForUser(String uid) {
        return vehiclesRef.whereEqualTo("uid", uid).get();
    }

    // --- INCIDENT OPERATIONS ---
    public Task<Void> logIncident(Incident incident) {
        String incidentId = incidentsRef.document().getId();
        incident.setIncident_id(incidentId);
        return incidentsRef.document(incidentId).set(incident);
    }

    public Task<QuerySnapshot> getIncidentsForUser(String uid) {
        return incidentsRef.whereEqualTo("uid", uid).get();
    }

    public Task<QuerySnapshot> getIncidentsForVehicle(String vid) {
        return incidentsRef.whereEqualTo("vid", vid).get();
    }

    // --- WORKSHOP OPERATIONS ---
    public Task<QuerySnapshot> getNearbyWorkshops() {
        return workshopsRef.get();
    }
}