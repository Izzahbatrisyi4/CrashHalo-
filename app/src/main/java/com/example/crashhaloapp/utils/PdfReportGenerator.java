package com.example.crashhaloapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import com.example.crashhaloapp.models.Detection;
import com.example.crashhaloapp.models.Incident;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportGenerator {

    public static File generateReport(Context context, Incident incident, int reportNumber) {
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
        // Fixed: timestamp is now a 'long', so we check if it's > 0 and use new Date(long)
        String dateStr = (incident.getTimestamp() > 0) ? sdf.format(new Date(incident.getTimestamp())) : sdf.format(new Date());
        canvas.drawText("Date: " + dateStr, 50, y, paint);
        y += 20;
        
        canvas.drawText("Status: " + incident.getStatus(), 50, y, paint);
        y += 40;

        // AI Assessment Section
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("AI DAMAGE ASSESSMENT", 50, y, paint);
        y += 25;
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

        y += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("EVIDENCE PHOTOS", 50, y, paint);
        y += 20;

        List<String> images = incident.getImages();
        String[] labels = {"1. 45 Degree View", "2. VIN Number", "3. Crash/Damage Area"};
        
        if (images != null) {
            for (int i = 0; i < images.size() && i < 3; i++) {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(labels[i], 60, y, paint);
                y += 10;

                File imgFile = new File(images.get(i));
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (bitmap != null) {
                        // Scale bitmap to fit in PDF
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 150, false);
                        canvas.drawBitmap(scaledBitmap, 60, y, null);
                        y += 170; // Move y down after image
                    }
                } else {
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
                    canvas.drawText("[Image not found locally]", 70, y + 10, paint);
                    y += 30;
                }
                
                // Add new page if we run out of space
                if (y > 750 && i < images.size() - 1) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }
            }
        }

        document.finishPage(page);

        // Naming format: CrashHalo Report 1.pdf
        File pdfFile = new File(context.getExternalFilesDir(null), "CrashHalo Report " + reportNumber + ".pdf");
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
