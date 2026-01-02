package com.example.habitwhisper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class FeatureMedicalTermActivity extends AppCompatActivity {
    private static final int PICK_REPORT_REQUEST = 2;
    private Button uploadBtn;
    private TextView resultText;
    private Uri reportUri;

    // Use OpenRouter API key for consistent implementation
    private static final String OPENROUTER_API_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_medical_term);
        uploadBtn = findViewById(R.id.btn_upload_blood);
        resultText = findViewById(R.id.medical_term_result);

        // Back to Home click
        android.widget.LinearLayout backToHome = findViewById(R.id.back_to_home);
        if (backToHome != null) {
            backToHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*"); // Allow all file types
                String[] mimeTypes = {"image/jpeg", "image/png", "image/*", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Medical Report or Image"), PICK_REPORT_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_REPORT_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            reportUri = data.getData();
            resultText.setText("Analyzing file...");
            new AnalyzeMedicalFileTask().execute(reportUri);
        }
    }

    // Add this AsyncTask for file analysis
    private class AnalyzeMedicalFileTask extends android.os.AsyncTask<Uri, Void, String> {
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
                String prompt = "You are a medical AI assistant. Analyze the attached medical file and provide a clear, accurate, and patient-friendly explanation of the findings. Use simple language, explain what is happening, and suggest possible solutions or next steps. Avoid medical jargon.";
                
                // Create OpenRouter API request
                org.json.JSONObject jsonBody = new org.json.JSONObject();
                jsonBody.put("model", "openai/gpt-4o");
                
                org.json.JSONArray contentArray = new org.json.JSONArray();
                org.json.JSONObject textContent = new org.json.JSONObject();
                textContent.put("type", "text");
                textContent.put("text", prompt);
                contentArray.put(textContent);
                
                // Add image part if it's an image
                if (fileType != null && fileType.startsWith("image/")) {
                    org.json.JSONObject imageContent = new org.json.JSONObject();
                    imageContent.put("type", "image_url");
                    org.json.JSONObject imageUrl = new org.json.JSONObject();
                    imageUrl.put("url", "data:" + fileType + ";base64," + base64File);
                    imageContent.put("image_url", imageUrl);
                    contentArray.put(imageContent);
                } else {
                    // For documents, add as text reference
                    org.json.JSONObject docContent = new org.json.JSONObject();
                    docContent.put("type", "text");
                    docContent.put("text", "[Document data attached]");
                    contentArray.put(docContent);
                }
                
                jsonBody.put("messages", new org.json.JSONArray()
                    .put(new org.json.JSONObject()
                        .put("role", "user")
                        .put("content", contentArray)
                    )
                );
                jsonBody.put("max_tokens", 700);

                java.net.URL url = new java.net.URL("https://openrouter.ai/api/v1/chat/completions");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + OPENROUTER_API_KEY);
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
                android.util.Log.d("OpenRouter_MedicalTerm", response);
                
                if (responseCode != 200) {
                    try {
                        org.json.JSONObject errorJson = new org.json.JSONObject(response);
                        if (errorJson.has("error")) {
                            return "❌ Sorry, the analysis could not be completed: " + errorJson.getJSONObject("error").optString("message", "(API error)");
                        }
                    } catch (Exception ex) {}
                    return "❌ Sorry, the analysis could not be completed. (API error)";
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
            resultText.setText("✅ Analysis Complete\n\n" + result);
        }
    }
}
