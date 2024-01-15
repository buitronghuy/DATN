package com.example.brainwave;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
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

public class ModelsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
    };

    public void verifyStoragePermission(Activity activity) {
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

    private static boolean isTraining = false;

    private static final int NO_ERROR = 0;
    private static final int DATASET_ERROR = 1;
    private static final int MODEL_ERROR = 2;
    private static final int UNKNOWN_ERROR = 3;
    private static int trainingError = NO_ERROR;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.models_view);

        verifyStoragePermission(ModelsActivity.this);

        TrainModel.id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Button button_train = (Button) findViewById(R.id.btn_train_model);
        button_train.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTraining) {
                    return;
                }

                isTraining = true;
                trainingError = UNKNOWN_ERROR;
                ModelsActivity.AsyncTaskRunner runner = new ModelsActivity.AsyncTaskTrainModel();
                runner.execute();
                ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar_models);
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

            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar_models);
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
            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar_models);
            bar.setVisibility(View.INVISIBLE);

        }

    }

    private class AsyncTaskTrainModel extends ModelsActivity.AsyncTaskRunner {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextView textView = findViewById(R.id.textView_models);
            String content = "Your devide is training...";
            textView.setText(content);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // run training process
            try {
                // load model first
                File pathFile = new File(getExternalFilesDir(TrainModel.modelDir), TrainModel.fileModelName);

                TrainModel.model = ModelSerializer.restoreMultiLayerNetwork(
                        pathFile, false);
                if (TrainModel.getlocateToSaveDataSet(ModelsActivity.this).length() == 0){
                    // nothing to train
                    trainingError = DATASET_ERROR;
                    return 0;
                }
                MultiLayerNetwork trainedmodel = TrainModel.TrainingModel(TrainModel.getlocateToSaveDataSet(ModelsActivity.this));

                ModelSerializer.writeModel(trainedmodel, pathFile, false);
                TrainModel.model = trainedmodel;
                trainingError = NO_ERROR;
            } catch (FileNotFoundException e) {
                trainingError = MODEL_ERROR;
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            //Update the UI with output
            TextView textView = findViewById(R.id.textView_models);
            if (trainingError == DATASET_ERROR) {
                textView.setText("Không tồn tại dữ liệu để huấn luyện!");
            } else if (trainingError == MODEL_ERROR) {
                textView.setText("Không tồn tại mô hình ban đầu!");
            } else if (trainingError == NO_ERROR) {
                textView.setText("Hoàn thành huấn luyện mô hình!");
            } else {
                textView.setText("Quá trình huấn luyện thất bại!");
            }
            isTraining = false;
        }
    }
}