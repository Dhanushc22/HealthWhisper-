package com.example.habitwhisper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class FeatureTabletActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 3;
    private ImageView imageView;
    private TextView resultText;
    private Button uploadBtn;
    private Uri imageUri;

    // Use the same API key as Xray feature
    private final String OPENAI_API_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_tablet);
        imageView = new ImageView(this);
        resultText = findViewById(R.id.tablet_result);
        uploadBtn = findViewById(R.id.btn_upload_tablet);

        // Back to Home click
        TextView backToHome = findViewById(R.id.back_to_home);
        if (backToHome != null) {
            backToHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            // Set the left arrow icon programmatically for compatibility
            backToHome.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow_back_24, 0, 0, 0);
            backToHome.setText("");
        }

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*"); // Allow all file types
                String[] mimeTypes = {"image/jpeg", "image/png", "image/*", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Tablet Image or File"), PICK_IMAGE_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            resultText.setText("Analyzing file...");
            new AnalyzeTabletFileTask().execute(imageUri);
        }
    }

    // Add this AsyncTask for file analysis
    private class AnalyzeTabletFileTask extends android.os.AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            try {
                android.content.ContentResolver resolver = getContentResolver();
                String fileType = resolver.getType(uris[0]);
                java.io.InputStream inputStream = resolver.openInputStream(uris[0]);
                byte[] buffer = new byte[4096];
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                int len;
                while ((len = inputStream.read(buffer)) != -1) baos.write(buffer, 0, len);
                inputStream.close();
                String base64File = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
                String prompt = "You are a medical AI assistant. Analyze the attached tablet image or file. Identify the tablet, explain what it is used for, and provide clear, patient-friendly information about its purpose, usage, and any important warnings. Use simple language.";
                org.json.JSONObject jsonBody = new org.json.JSONObject();
                jsonBody.put("model", "openai/gpt-4o");
                org.json.JSONArray contentArr = new org.json.JSONArray();
                contentArr.put(new org.json.JSONObject().put("type", "text").put("text", prompt));
                if (fileType != null && fileType.startsWith("image/")) {
                    contentArr.put(new org.json.JSONObject().put("type", "image_url").put("image_url", new org.json.JSONObject().put("url", "data:image/jpeg;base64," + base64File)));
                } else {
                    contentArr.put(new org.json.JSONObject().put("type", "file_url").put("file_url", new org.json.JSONObject().put("url", "data:application/octet-stream;base64," + base64File)));
                }
                jsonBody.put("messages", new org.json.JSONArray().put(new org.json.JSONObject().put("role", "user").put("content", contentArr)));
                jsonBody.put("max_tokens", 700);
                java.net.URL url = new java.net.URL("https://openrouter.ai/api/v1/chat/completions");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("HTTP-Referer", "https://healthwhisper.app");
                conn.setRequestProperty("X-Title", "HealthWhisper");
                conn.setDoOutput(true);
                java.io.OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes());
                os.flush();
                os.close();
                int responseCode = conn.getResponseCode();
                java.io.InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                java.io.ByteArrayOutputStream baos2 = new java.io.ByteArrayOutputStream();
                byte[] buffer2 = new byte[1024];
                int len2;
                while ((len2 = is.read(buffer2)) != -1) baos2.write(buffer2, 0, len2);
                is.close();
                String response = baos2.toString();
                if (responseCode != 200) {
                    try {
                        org.json.JSONObject errorJson = new org.json.JSONObject(response);
                        if (errorJson.has("error")) {
                            return "Sorry, the analysis could not be completed: " + errorJson.getJSONObject("error").optString("message", "(API error)");
                        }
                    } catch (Exception ex) {}
                    return "Sorry, the analysis could not be completed. (API error)";
                }
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                String result = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                return result.trim();
            } catch (Exception e) {
                e.printStackTrace();
                return "Sorry, the analysis could not be completed. Please try again with a clear file.";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            resultText.setText(result);
        }
    }
}
