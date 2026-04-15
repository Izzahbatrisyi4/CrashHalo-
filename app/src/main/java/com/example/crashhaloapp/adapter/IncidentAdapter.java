package com.example.crashhaloapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crashhaloapp.R;
import com.example.crashhaloapp.models.Incident;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private List<Incident> incidentList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Incident incident);
    }

    public IncidentAdapter(List<Incident> incidentList, OnItemClickListener listener) {
        this.incidentList = incidentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident, parent, false);
        return new IncidentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        Incident incident = incidentList.get(position);
        holder.bind(incident, listener);
    }

    @Override
    public int getItemCount() {
        return incidentList.size();
    }

    static class IncidentViewHolder extends RecyclerView.ViewHolder {
        TextView txtReportId, txtVehicleName, txtDateLocation, txtStatus;

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReportId = itemView.findViewById(R.id.txt_report_id);
            txtVehicleName = itemView.findViewById(R.id.txt_vehicle_name);
            txtDateLocation = itemView.findViewById(R.id.txt_date_location);
            txtStatus = itemView.findViewById(R.id.txt_status);
        }

        public void bind(final Incident incident, final OnItemClickListener listener) {
            txtReportId.setText("REPORT #" + (incident.getIncident_id() != null ? incident.getIncident_id().substring(0, 4).toUpperCase() : "0000"));
            txtVehicleName.setText(incident.getVid() != null ? incident.getVid() : "Unknown Vehicle");
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String dateStr = incident.getTimestamp() != null ? sdf.format(incident.getTimestamp().toDate()) : sdf.format(new Date());
            txtDateLocation.setText(dateStr);
            
            txtStatus.setText(incident.getStatus() != null ? incident.getStatus().toUpperCase() : "PENDING");

            itemView.setOnClickListener(v -> listener.onItemClick(incident));
        }
    }
}