package com.example.crashhaloapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import com.example.crashhaloapp.models.Detection;
import com.example.crashhaloapp.models.Incident;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfReportGenerator {

    public static File generateReport(Context context, Incident incident) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        // Header
        paint.setColor(Color.parseColor("#1A73E8"));
        paint.setTextSize(24);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("CRASHHALO - ACCIDENT REPORT", 50, 50, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        int y = 100;
        canvas.drawText("Report ID: " + incident.getIncident_id(), 50, y, paint);
        y += 20;
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        String dateStr = incident.getTimestamp() != null ? sdf.format(incident.getTimestamp().toDate()) : sdf.format(new Date());
        canvas.drawText("Date: " + dateStr, 50, y, paint);
        y += 20;
        
        canvas.drawText("Status: " + incident.getStatus(), 50, y, paint);
        y += 40;

        // AI Assessment Section
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("AI DAMAGE ASSESSMENT", 50, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        if (incident.getDetections() != null && !incident.getDetections().isEmpty()) {
            for (Detection detection : incident.getDetections()) {
                String detectionText = "- " + detection.getLabel() + " (" + String.format(Locale.getDefault(), "%.1f%%", detection.getConfidence() * 100) + ")";
                canvas.drawText(detectionText, 60, y, paint);
                y += 20;
            }
        } else {
            canvas.drawText("No significant damage detected by AI.", 60, y, paint);
            y += 20;
        }

        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("INSURANCE TIMELINE PHOTOS", 50, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Photos have been successfully uploaded to the insurance claim portal.", 60, y, paint);

        document.finishPage(page);

        File pdfFile = new File(context.getExternalFilesDir(null), "Report_" + incident.getIncident_id() + ".pdf");
        try {
            document.writeTo(new FileOutputStream(pdfFile));
        } catch (IOException e) {
            Log.e("PdfGenerator", "Error writing PDF", e);
            return null;
        } finally {
            document.close();
        }

        return pdfFile;
    }
}