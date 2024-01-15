package com.example.brainwave;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.service.autofill.Dataset;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.example.brainwave.TrainModel;

import org.datavec.api.transform.filter.InvalidNumColumns;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;
import org.w3c.dom.Text;

import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    // read and write permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermission(Activity activity) {
        // Get permission status
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission we request it
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private static String server_url = "https://server-production-6a93.up.railway.app";
//    private static String server_url = "http://192.168.1.8:8080";

    private static boolean isLoading = false;
    private static boolean isLoaded = false;
    private static boolean isSended = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_view);

        verifyStoragePermission(UpdateActivity.this);

        Button button_update = (Button) findViewById(R.id.btn_update);
        button_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isLoading) {
                    return;
                }
                isLoading = true;
                AsyncTaskRunner runner = new AsyncTaskLoadModel();
                runner.execute();
                ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
                bar.setVisibility(View.VISIBLE);
            }
        });

        Button button_upload = (Button) findViewById(R.id.btn_upload);
        button_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoading) {
                    return;
                }
                isLoading = true;
                AsyncTaskRunner runner = new AsyncTaskUploadModel();
                runner.execute();
                ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
                bar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class AsyncTaskRunner extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
            bar.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            //Hide the progress bar now that we are finished
            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
            bar.setVisibility(View.INVISIBLE);

        }

    }

    private class AsyncTaskLoadModel extends AsyncTaskRunner {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String content = "Loading model...";
            TextView textView = findViewById(R.id.textView);
            textView.setText(content);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                String apiUrl = server_url + "/api/getmodel";
                File pathFile = new File(getExternalFilesDir(TrainModel.modelDir), TrainModel.fileModelName);

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream outputStream = new FileOutputStream(pathFile);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();

                    String content = "Zip file downloaded successfully.";
                    System.out.println(content);
                    isLoaded = true;
                } else {
                    System.out.println("Failed to download zip file. Response code: " + responseCode);
                }

                connection.disconnect();
                TrainModel.model = ModelSerializer.restoreMultiLayerNetwork(pathFile, false);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (isLoaded == true) {
                String content = "Zip file downloaded successfully.";
                TextView textView = findViewById(R.id.textView);
                textView.setText(content);
            } else {
                String content = "Failed to download zip file.";
                TextView textView = findViewById(R.id.textView);
                textView.setText(content);
            }
            isLoading = false;
            isLoaded = false;
        }

    }

    private class AsyncTaskUploadModel extends AsyncTaskRunner {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextView textView = findViewById(R.id.textView);
            String content = " Sending model...";
            textView.setText(content);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            String apiUrl = server_url + "/api/uploadmodel";
            File pathFile = new File(getExternalFilesDir(TrainModel.modelDir), TrainModel.fileModelName);

            try {
                String boundary = "*****";
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + pathFile.getName() + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                FileInputStream fileInputStream = new FileInputStream(pathFile);
                int bytesRead;
                byte[] buffer = new byte[4096];
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                int responseCode = conn.getResponseCode();

                // Handle the response
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String content = "Send file successfully";
                    System.out.println(content);
                    isSended = true;
                } else {
                    System.out.println("Failed to send file. Response code: " + responseCode);
                }

                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (isSended == true) {
                String content = "Send file successfully";
                TextView textView = findViewById(R.id.textView);
                textView.setText(content);
            } else {
                String content = "Failed to send file!";
                TextView textView = findViewById(R.id.textView);
                textView.setText(content);
            }
            isLoading = false;
            isSended = false;
        }
    }
}