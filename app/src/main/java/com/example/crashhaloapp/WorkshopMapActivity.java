package com.example.crashhaloapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.crashhaloapp.databinding.ActivityWorkshopMapBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class WorkshopMapActivity extends AppCompatActivity {

    private ActivityWorkshopMapBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint userLocation;
    private GeoPoint crashLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load OSM configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        binding = ActivityWorkshopMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if a crash location was passed
        double lat = getIntent().getDoubleExtra("crash_lat", 0.0);
        double lng = getIntent().getDoubleExtra("crash_lng", 0.0);
        if (lat != 0.0 && lng != 0.0) {
            crashLocation = new GeoPoint(lat, lng);
        }

        setupMap();
        checkPermissionsAndGetLocation();
    }

    private void setupMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        binding.map.getController().setZoom(15.0);
        binding.map.setMultiTouchControls(true);
        binding.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
    }

    private void checkPermissionsAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    
                    // Center on crash location if available, otherwise user location
                    if (crashLocation != null) {
                        binding.map.getController().animateTo(crashLocation);
                        addCrashMarker();
                    } else {
                        binding.map.getController().animateTo(userLocation);
                    }
                    
                    addWorkshopMarkers();
                }
            });
        }
    }

    private void addCrashMarker() {
        Marker marker = new Marker(binding.map);
        marker.setPosition(crashLocation);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("CRASH LOCATION");
        // Optional: marker.setIcon(getResources().getDrawable(R.drawable.ic_crash_marker));
        binding.map.getOverlays().add(marker);
    }

    private void addWorkshopMarkers() {
        // Use userLocation if crashLocation is null, otherwise use crashLocation for "nearby" logic
        GeoPoint center = (crashLocation != null) ? crashLocation : userLocation;

        addMarker(new GeoPoint(center.getLatitude() + 0.005, center.getLongitude() + 0.005), "Top Gear Auto Clinic");
        addMarker(new GeoPoint(center.getLatitude() - 0.004, center.getLongitude() - 0.003), "Safety First Repair Shop");
        addMarker(new GeoPoint(center.getLatitude() + 0.002, center.getLongitude() - 0.006), "Expert Body Works");
    }

    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(binding.map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        
        marker.setOnMarkerClickListener((m, mapView) -> {
            showWorkshopDetails(m);
            return true;
        });
        
        binding.map.getOverlays().add(marker);
    }

    private void showWorkshopDetails(Marker marker) {
        binding.workshopDetailsCard.setVisibility(View.VISIBLE);
        binding.txtWorkshopName.setText(marker.getTitle());
        
        // Calculate Distance from user's current location to workshop
        float[] results = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), 
                marker.getPosition().getLatitude(), marker.getPosition().getLongitude(), results);
        
        float distanceKm = results[0] / 1000;
        int etaMinutes = (int) (distanceKm * 4); 

        binding.txtWorkshopEta.setText(String.format(Locale.getDefault(), "Distance: %.1f km | ETA: %d mins", distanceKm, etaMinutes));

        binding.btnGetDirections.setOnClickListener(v -> {
            String uri = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", 
                    marker.getPosition().getLatitude(), marker.getPosition().getLongitude());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                String geoUri = "geo:" + marker.getPosition().getLatitude() + "," + marker.getPosition().getLongitude();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.map.onPause();
    }
}