package com.example.brainwave;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class LabeledActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
    };

    public void verifyPermission(Activity activity) {
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 99);
        }
    }

    private static final String TAG = LabeledActivity.class.getSimpleName();
    private TgStreamReader tgStreamReader;

    private BluetoothAdapter mBluetoothAdapter;

    private TextView tv_notification = null;
    private int badPacketCount = 0;

    private Button btn_start = null;
    private Button btn_stop = null;

    File myExternalFile;
    FileOutputStream fileOutputStream = null;

    private static String currentStatus = "Buồn ngủ";
    private static boolean isPoorSignal = false;

    private int numbeOfSamples = 0;
    private static final int MAX_SAMPLES = 5;
    private static final int NUMBER_OF_FEATURES = 80;

    private static boolean isProcessing = false;

    public EEGPower[] dataCollected = new EEGPower[MAX_SAMPLES];
    public EEGPower[] dataForLabel = new EEGPower[MAX_SAMPLES];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.labeled_view);

        verifyPermission(LabeledActivity.this);

        initView();

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Toast.makeText(
                        this,
                        "Please enable your Bluetooth and re-run this program !",
                        Toast.LENGTH_LONG).show();
                finish();
//				return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
            return;
        }

        tgStreamReader = new TgStreamReader(mBluetoothAdapter,callback);
        tgStreamReader.setGetDataTimeOutTime(6);
        tgStreamReader.startLog();

    }

    private void initView() {
        tv_notification = (TextView) findViewById(R.id.tv_labeled_notification);
        tv_notification.setText("");

        btn_start = (Button) findViewById(R.id.btn_labeled_start);
        btn_stop = (Button) findViewById(R.id.btn_labeled_stop);
        // init spinner and set listener
        Spinner status = findViewById(R.id.label_spinner);
        status.setOnItemSelectedListener(this);

        btn_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (isProcessing) {
                    return;
                }
                numbeOfSamples = 0;
                isProcessing = true;
                tv_notification.setText("Đang ghi lại trạng thái trực tiếp...");
                showToast("Connecting...", Toast.LENGTH_SHORT);
                badPacketCount = 0;

                openFile(TrainModel.getlocateToSaveDataSet(LabeledActivity.this));


                if(tgStreamReader != null && tgStreamReader.isBTConnected()){
                    // Prepare for connecting
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }
                tgStreamReader.connect();
            }
        });

        btn_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                tv_notification.setText("Đã hoàn thành!");
                stop();
            }
        });
    }


    public void stop() {
        if(tgStreamReader != null){
            tgStreamReader.stop();
            tgStreamReader.close();
        }
        numbeOfSamples = 0;
        isProcessing = false;
        tv_notification.setText("Nhấn Start để bắt đầu");
        closeFile(TrainModel.getlocateToSaveDataSet(LabeledActivity.this));
    }

    @Override
    protected void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        tv_notification.setText("Nhấn Start để bắt đầu");
        super.onResume();
    }

    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    tgStreamReader.start();
                    showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    tgStreamReader.startRecordRawData();

                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    tgStreamReader.stopRecordRawData();

                    showToast("Get data time out!", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_STOPPED:
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    break;
                case ConnectionStates.STATE_ERROR:
                    break;
                case ConnectionStates.STATE_FAILED:
                    setFailState();
                    showToast("Connection failed!\nPlease check your bluetooth device", Toast.LENGTH_SHORT);
                    break;
            }
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);
        }

        @Override
        public void onRecordFail(int flag) {
            // handle the record error message
            Log.e(TAG,"onRecordFail: " +flag);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // handle the bad packets.
            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // handle the received data
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = datatype;
            msg.arg1 = data;
            msg.obj = obj;
            LinkDetectedHandler.sendMessage(msg);

            //Log.i(TAG,"onDataReceived");
        }

    };

    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;

    int raw;
    private Handler LinkDetectedHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MindDataType.CODE_RAW:
                    break;
                case MindDataType.CODE_MEDITATION:
                    Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);
                    break;
                case MindDataType.CODE_ATTENTION:
                    Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
                    break;
                case MindDataType.CODE_EEGPOWER:
                    if (isPoorSignal == true) {
                        isPoorSignal = false;
                        break;
                    }
                    EEGPower power = (EEGPower)msg.obj;
                    if(power.isValidate()){
                        if(numbeOfSamples >= MAX_SAMPLES) {
                            numbeOfSamples = 0;
                            dataForLabel = dataCollected.clone();
                            LabeledActivity.AsyncTaskLabel runner = new LabeledActivity.AsyncTaskLabel();
                            runner.execute();
                        }
                        dataCollected[numbeOfSamples] = power;
                        numbeOfSamples++;
                    }
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    int poorSignal = msg.arg1;
                    Log.d(TAG, "poorSignal:" + poorSignal);
                    if (poorSignal > 0) {
                        isPoorSignal = true;
                    }
                    break;
                case MSG_UPDATE_BAD_PACKET:
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    // Spinner related codes
    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        currentStatus = parent.getItemAtPosition(pos).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    public void showToast(final String msg,final int timeStyle){
        LabeledActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }

    private void openFile(File myFile) {
        try {
            fileOutputStream = new FileOutputStream(myFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeFile(File myFile) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AsyncTaskLabel extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            EEGPower[] EEGdata = dataForLabel.clone();
            double [] sample = new double[NUMBER_OF_FEATURES];
            for (int i = 0; i<MAX_SAMPLES; i++) {
                sample[i*16] = EEGdata[i].delta;
                sample[i*16+1] = EEGdata[i].theta;
                sample[i*16+2] = EEGdata[i].lowAlpha;
                sample[i*16+3] = EEGdata[i].highAlpha;
                sample[i*16+4] = EEGdata[i].lowBeta;
                sample[i*16+5] = EEGdata[i].highBeta;

                sample[i*16+6] = (double) EEGdata[i].delta/EEGdata[i].theta;
                sample[i*16+7] = (double)EEGdata[i].delta/EEGdata[i].lowAlpha;
                sample[i*16+8] = (double)EEGdata[i].delta/EEGdata[i].highAlpha;
                sample[i*16+9] = (double)EEGdata[i].delta/EEGdata[i].lowBeta;
                sample[i*16+10] = (double)EEGdata[i].delta/EEGdata[i].highBeta;

                sample[i*16+11] = (double)EEGdata[i].theta/EEGdata[i].lowAlpha;
                sample[i*16+12] = (double)EEGdata[i].theta/EEGdata[i].highAlpha;
                sample[i*16+13] = (double)EEGdata[i].theta/EEGdata[i].lowBeta;
                sample[i*16+14] = (double)EEGdata[i].theta/EEGdata[i].highBeta;

                sample[i*16+15] = (double)(EEGdata[i].delta + EEGdata[i].theta) / (EEGdata[i].lowAlpha + EEGdata[i].highAlpha + EEGdata[i].lowBeta +EEGdata[i].highBeta);
            }
            writeEEGDataToFile(sample);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    public void writeEEGDataToFile(double [] sample) {

        int index = LocalDataSet.label2index(currentStatus);
        if (index == -1) {
            // unexpected label
            return;
        }
        String data = String.format("%d",index);
        for (int i = 0; i<NUMBER_OF_FEATURES; i++) {
            data = data + "," + String.format(Locale.US,"%.2f", sample[i]);
        }
        data = data + "\n";
        try {
            fileOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setFailState(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = findViewById(R.id.tv_labeled_notification);
                textView.setText("Mất kết nối!");
                if(tgStreamReader != null){
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }
            }
        });
    }

}