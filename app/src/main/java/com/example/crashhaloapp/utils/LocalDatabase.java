package com.example.crashhaloapp.utils;

import android.content.Context;
import com.example.crashhaloapp.models.Incident;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.models.Vehicle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LocalDatabase {
    private static final String INCIDENTS_FILE = "incidents.json";
    private static final String PROFILE_FILE = "profile.json";
    private static final String VEHICLES_FILE = "vehicles.json";
    private final Context context;
    private final Gson gson;

    public LocalDatabase(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void saveIncident(Incident incident) {
        List<Incident> list = getAllIncidents();
        list.add(0, incident); // Add newest to top
        saveList(list);
    }

    public List<Incident> getAllIncidents() {
        File file = new File(context.getFilesDir(), INCIDENTS_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<ArrayList<Incident>>() {}.getType();
            List<Incident> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void saveList(List<Incident> list) {
        File file = new File(context.getFilesDir(), INCIDENTS_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException ignored) {}
    }

    public void saveUser(User user) {
        File file = new File(context.getFilesDir(), PROFILE_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(user, writer);
        } catch (IOException ignored) {}
    }

    public User getUser() {
        File file = new File(context.getFilesDir(), PROFILE_FILE);
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, User.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void saveVehicle(Vehicle vehicle) {
        List<Vehicle> list = getAllVehicles();
        // Simple logic: update if exists (by ID), otherwise add
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getVid().equals(vehicle.getVid())) {
                list.set(i, vehicle);
                found = true;
                break;
            }
        }
        if (!found) list.add(vehicle);
        
        File file = new File(context.getFilesDir(), VEHICLES_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException ignored) {}
    }

    public List<Vehicle> getAllVehicles() {
        File file = new File(context.getFilesDir(), VEHICLES_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<ArrayList<Vehicle>>() {}.getType();
            List<Vehicle> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
