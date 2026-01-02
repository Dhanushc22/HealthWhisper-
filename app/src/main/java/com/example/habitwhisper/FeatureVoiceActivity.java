package com.example.habitwhisper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;

public class FeatureVoiceActivity extends AppCompatActivity {
    private EditText questionEditText;
    private Button getAdviceBtn;
    private TextView responseText;
    private Button recordBtn;
    private boolean isRecording = false;
    private MediaRecorder recorder;
    private String audioFilePath;
    private static final int REQUEST_MICROPHONE = 200;
    // Use OpenRouter API key for consistent implementation
    private static final String OPENROUTER_API_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_voice);
        questionEditText = findViewById(R.id.edit_text_question);
        getAdviceBtn = findViewById(R.id.btn_get_advice);
        responseText = findViewById(R.id.voice_response);
        recordBtn = findViewById(R.id.btn_start_recording);

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

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    if (ContextCompat.checkSelfPermission(FeatureVoiceActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(FeatureVoiceActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
                    } else {
                        startRecording();
                    }
                } else {
                    stopRecording();
                }
            }
        });

        getAdviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = questionEditText.getText().toString().trim();
                // If user entered text, always analyze text (ignore audio)
                if (!question.isEmpty()) {
                    responseText.setText("Analyzing your input, please wait...");
                    new AnalyzeTextTask().execute(question);
                // If no text, but audio is present, analyze audio
                } else if (audioFilePath != null && !audioFilePath.isEmpty()) {
                    responseText.setText("Analyzing your voice input, please wait...");
                    new AnalyzeAudioTask().execute(audioFilePath);
                } else {
                    responseText.setText("Please record your voice or enter a question.");
                }
            }
        });
    }

    private void startRecording() {
        audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/voice_input.3gp";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audioFilePath);
        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            recordBtn.setText("Stop Recording");
            responseText.setText("Recording... Speak now.");
        } catch (IOException e) {
            e.printStackTrace();
            responseText.setText("Recording failed.");
        }
    }

    private void stopRecording() {
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
            recordBtn.setText("Start Recording");
            responseText.setText("Recording complete. Tap 'Get Advice' to analyze.");
        } catch (Exception e) {
            e.printStackTrace();
            responseText.setText("Error stopping recording.");
        }
    }

    // AsyncTask to send user input to OpenRouter API and get doctor-like suggestions
    private class AnalyzeTextTask extends android.os.AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String userInput = params[0];
                String prompt = "You are a world-class medical AI doctor. The patient says: '" + userInput + "'. Analyze the symptoms or question, provide a clear, patient-friendly explanation, possible causes, and actionable recommendations. Avoid medical jargon. If urgent, advise to see a doctor.";
                
                // Create OpenRouter API request
                org.json.JSONObject jsonBody = new org.json.JSONObject();
                jsonBody.put("model", "openai/gpt-4o");
                jsonBody.put("messages", new org.json.JSONArray()
                    .put(new org.json.JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
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
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) baos.write(buffer, 0, len);
                is.close();
                String response = baos.toString();
                android.util.Log.d("OpenRouter_Voice", response);
                
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
                return "Sorry, the analysis could not be completed due to a technical error. Please check your internet connection and API key, then try again.";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            responseText.setText("✅ Analysis Complete\n\n" + result);
        }
    }

    // Add this AsyncTask for audio analysis (stub for now)
    private class AnalyzeAudioTask extends android.os.AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // You can implement audio-to-text and send to OpenRouter here
            // For now, just return a placeholder
            return "Audio analysis is not yet implemented. Please use text input for now.";
        }
        @Override
        protected void onPostExecute(String result) {
            responseText.setText(result);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                // Show a dialog explaining why the permission is needed
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Microphone Permission Needed")
                        .setMessage("This feature requires access to your microphone to record your voice. Please allow microphone access in your device settings.")
                        .setPositiveButton("OK", null)
                        .show();
                responseText.setText("Microphone denied. Please allow microphone access in settings.");
            }
        }
    }
}
