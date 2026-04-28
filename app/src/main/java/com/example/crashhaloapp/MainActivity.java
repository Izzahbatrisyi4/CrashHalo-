package com.example.crashhaloapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.crashhaloapp.adapter.IncidentAdapter;
import com.example.crashhaloapp.databinding.ActivityMainBinding;
import com.example.crashhaloapp.models.Incident;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.utils.LocalDatabase;
import com.example.crashhaloapp.utils.PdfReportGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private LocalDatabase localDatabase;
    private IncidentAdapter adapter;
    private List<Incident> incidentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDatabase = new LocalDatabase(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupRecyclerView();
        loadUserProfileImage();
        loadIncidents();

        binding.imgProfileTop.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraCaptureActivity.class);
            startActivity(intent);
        });

        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnHistory.setOnClickListener(v -> {
            binding.scrollView.smoothScrollTo(0, 0);
            loadIncidents();
            Toast.makeText(this, "History Refreshed", Toast.LENGTH_SHORT).show();
        });

        binding.btnHome.setOnClickListener(v -> {
            binding.scrollView.smoothScrollTo(0, 0);
        });
    }

    private void setupRecyclerView() {
        adapter = new IncidentAdapter(incidentList, incident -> {
            // Number is based on list index (newest is first in list)
            int reportNumber = incidentList.size() - incidentList.indexOf(incident);
            File pdfFile = PdfReportGenerator.generateReport(this, incident, reportNumber);
            if (pdfFile != null) {
                openPdfReport(pdfFile);
            }
        });
        binding.rvIncidents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvIncidents.setAdapter(adapter);
    }

    private void openPdfReport(File file) {
        Uri pdfUri = FileProvider.getUriForFile(this, "com.example.crashhaloapp.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "View Report"));
        } catch (Exception e) {
            sharePdfReport(file);
        }
    }

    private void sharePdfReport(File file) {
        Uri pdfUri = FileProvider.getUriForFile(this, "com.example.crashhaloapp.fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Accident Report"));
    }

    private void loadIncidents() {
        incidentList.clear();
        incidentList.addAll(localDatabase.getAllIncidents());
        
        if (incidentList.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.rvIncidents.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.rvIncidents.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void loadUserProfileImage() {
        User user = localDatabase.getUser();
        if (user != null && user.getProfile_image_url() != null) {
            File imgFile = new File(user.getProfile_image_url());
            if (imgFile.exists()) {
                Glide.with(this)
                        .load(imgFile)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(binding.imgProfileTop);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfileImage();
        loadIncidents();
    }
}