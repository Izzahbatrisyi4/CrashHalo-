package com.example.crashhaloapp.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class YoloDetector {

    private static final String MODEL_FILE = "best_float16.tflite";
    private static final String LABEL_FILE = "labels.txt";
    
    private Interpreter interpreter;
    private List<String> labels;
    private final int inputSize = 640; // Default YOLOv8 size

    public YoloDetector(Context context) throws IOException {
        MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE);
        Interpreter.Options options = new Interpreter.Options();
        interpreter = new Interpreter(modelBuffer, options);
        labels = FileUtil.loadLabels(context, LABEL_FILE);
    }

    public List<Recognition> detect(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(inputSize, inputSize, ResizeOp.Method.BILINEAR))
                .add(new NormalizeOp(0f, 255f)) // Normalize to [0,1]
                .build();

        tensorImage = imageProcessor.process(tensorImage);

        // Output shape for YOLOv8 is usually [1, classes + 4, 8400]
        float[][][] output = new float[1][labels.size() + 4][8400];
        interpreter.run(tensorImage.getBuffer(), output);

        return parseBestResult(output);
    }

    private List<Recognition> parseBestResult(float[][][] output) {
        List<Recognition> recognitions = new ArrayList<>();
        // Note: This is a simplified parser. For a production app, 
        // you would implement Non-Maximum Suppression (NMS).
        
        float maxConfidence = 0;
        int bestClassId = -1;

        for (int i = 0; i < 8400; i++) {
            for (int c = 0; c < labels.size(); c++) {
                float confidence = output[0][c + 4][i];
                if (confidence > maxConfidence && confidence > 0.3) {
                    maxConfidence = confidence;
                    bestClassId = c;
                }
            }
        }

        if (bestClassId != -1) {
            recognitions.add(new Recognition(labels.get(bestClassId), maxConfidence));
        }

        return recognitions;
    }

    public static class Recognition {
        public String label;
        public float confidence;

        public Recognition(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}