package com.example.habitwhisper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FeatureXrayActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView resultText;
    private Button uploadBtn;
    private LinearLayout backToHome;
    private Uri imageUri;
    
    // OpenRouter API key
    private static final String OPENROUTER_API_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_xray);
        
        // Initialize views to match the UI layout
        resultText = findViewById(R.id.xray_result);
        uploadBtn = findViewById(R.id.btn_upload_xray);
        backToHome = findViewById(R.id.back_to_home);

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        // Back to Home click - LinearLayout from the UI
        if (backToHome != null) {
            backToHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            String fileType = getContentResolver().getType(imageUri);
            if (fileType != null && (fileType.startsWith("image/"))) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    resultText.setText("🔍 Analyzing image...");
                    new AnalyzeXrayTask().execute(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    resultText.setText("❌ Failed to load image.");
                }
            } else if (fileType != null && (fileType.equals("application/pdf") || fileType.equals("application/msword") || fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                resultText.setText("📄 Analyzing document...");
                new AnalyzeDocTask().execute(imageUri);
            } else {
                resultText.setText("❌ Selected file is not a supported type. Please select an image, PDF, or DOC file.");
            }
        }
    }

    // ================= OPENROUTER API TASKS =================

    private class AnalyzeXrayTask extends AsyncTask<Bitmap, Void, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            try {
                Bitmap bitmap = bitmaps[0];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                String prompt = "You are a world-class medical AI. Analyze the attached X-ray or medical image and provide a clear, detailed, and patient-friendly explanation of the findings. Use very simple language, avoid medical jargon, and explain what is happening in the image. Also, suggest possible solutions or next steps for the patient. If the image is unclear, do your best to provide helpful information based on what you can see.";
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", "openai/gpt-4o");
                jsonBody.put("messages", new org.json.JSONArray()
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", new org.json.JSONArray()
                                        .put(new JSONObject().put("type", "text").put("text", prompt))
                                        .put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64Image)))
                                )
                        )
                );
                jsonBody.put("max_tokens", 700);
                URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + OPENROUTER_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("HTTP-Referer", "https://healthwhisper.app");
                conn.setRequestProperty("X-Title", "HealthWhisper");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes());
                os.flush();
                os.close();
                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) baos2.write(buffer, 0, len);
                is.close();
                String response = baos2.toString();
                android.util.Log.d("OpenRouter_XRAY", response);
                if (responseCode != 200) {
                    try {
                        JSONObject errorJson = new JSONObject(response);
                        if (errorJson.has("error")) {
                            return "❌ Sorry, the analysis could not be completed: " + errorJson.getJSONObject("error").optString("message", "(API error)");
                        }
                    } catch (Exception ex) {}
                    return "❌ Sorry, the analysis could not be completed. (API error)";
                }
                JSONObject jsonResponse = new JSONObject(response);
                String result = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                return result.trim();
            } catch (Exception e) {
                e.printStackTrace();
                return "❌ Sorry, the analysis could not be completed due to a technical error. Please check your internet connection and API key, then try again.";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            resultText.setText("✅ Analysis Complete\n\n" + result);
        }
    }

    private class AnalyzeDocTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uris[0]);
                byte[] buffer = new byte[4096];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = inputStream.read(buffer)) != -1) baos.write(buffer, 0, len);
                inputStream.close();
                String base64Doc = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                String prompt = "You are a medical AI assistant. Analyze the attached medical report (PDF or DOC) and provide a clear, accurate, and patient-friendly explanation of the findings. Use simple language, explain what is happening, and suggest possible solutions or next steps. Avoid medical jargon.";
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", "openai/gpt-4o");
                jsonBody.put("messages", new org.json.JSONArray()
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", new org.json.JSONArray()
                                        .put(new JSONObject().put("type", "text").put("text", prompt))
                                        .put(new JSONObject().put("type", "file_url").put("file_url", new JSONObject().put("url", "data:application/octet-stream;base64," + base64Doc)))
                                )
                        )
                );
                jsonBody.put("max_tokens", 600);
                URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + OPENROUTER_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("HTTP-Referer", "https://healthwhisper.app");
                conn.setRequestProperty("X-Title", "HealthWhisper");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes());
                os.flush();
                os.close();
                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                byte[] buffer2 = new byte[1024];
                int len2;
                while ((len2 = is.read(buffer2)) != -1) baos2.write(buffer2, 0, len2);
                is.close();
                String response = baos2.toString();
                android.util.Log.d("OpenRouter_DOC", response);
                if (responseCode != 200) {
                    try {
                        JSONObject errorJson = new JSONObject(response);
                        if (errorJson.has("error")) {
                            return "❌ Sorry, the analysis could not be completed: " + errorJson.getJSONObject("error").optString("message", "(API error)");
                        }
                    } catch (Exception ex) {}
                    return "❌ Sorry, the analysis could not be completed. (API error)";
                }
                JSONObject jsonResponse = new JSONObject(response);
                String result = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                return result.trim();
            } catch (Exception e) {
                e.printStackTrace();
                return "❌ Sorry, the analysis could not be completed. Please try again with a clear document.";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            resultText.setText("✅ Analysis Complete\n\n" + result);
        }
    }
}
