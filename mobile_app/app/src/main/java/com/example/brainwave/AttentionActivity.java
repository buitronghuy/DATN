package com.example.brainwave;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AttentionActivity extends AppCompatActivity {

    private static final String TAG = AttentionActivity.class.getSimpleName();
    private TgStreamReader tgStreamReader;

    private BluetoothAdapter mBluetoothAdapter;

    private TextView tv_attention_value;
    private TextView tv_attention_notification;
    private int badPacketCount = 0;
    private int numbeOfSamples = 0;
    private static final int MAX_SAMPLES = 5;
    private static final int THRESHOLD = 80;

    private Button btn_start = null;
    private Button btn_stop = null;

    private static boolean isPoorSignal = false;
    public static Intent intent;

    private static boolean isProcessing = false;

    public EEGPower[] dataCollected = new EEGPower[MAX_SAMPLES];
    public EEGPower[] dataForInfer = new EEGPower[MAX_SAMPLES];

    private static final int NUMBER_OF_FEATURES = 80;

    private static final int [] sampleShape = {1, NUMBER_OF_FEATURES};

    private static int currentStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 99);
        }
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.attention_view);

        intent = new Intent(AttentionActivity.this, AlertService.class);

        initView();

        try {
            // (1) Make sure that the device supports Bluetooth and Bluetooth is on
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
        tv_attention_value = (TextView) findViewById(R.id.tv_attention_value);
        tv_attention_notification = (TextView) findViewById(R.id.tv_attention_notification);

        btn_start = (Button) findViewById(R.id.btn_attention_start);
        btn_stop = (Button) findViewById(R.id.btn_attention_stop);

        btn_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (isProcessing) {
                    return;
                }
                showToast("Connecting...", Toast.LENGTH_SHORT);
                numbeOfSamples = 0;
                isProcessing = true;
                tv_attention_notification.setText("Đang giám sát...");

                badPacketCount = 0;

                // load model
                try {
                    if (TrainModel.model == null) {
                        File pathFile = new File(getExternalFilesDir(TrainModel.modelDir), TrainModel.fileModelName);
                        TrainModel.model = ModelSerializer.restoreMultiLayerNetwork(pathFile, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                if(tgStreamReader != null && tgStreamReader.isBTConnected()){

                    // Prepare for connecting
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }

                tgStreamReader.connect();
//				tgStreamReader.connectAndStart();

            }

        });

        btn_stop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                stop();
            }

        });
    }


    public void stop() {
        if(tgStreamReader != null){
            tgStreamReader.stop();
            tgStreamReader.close();
        }
        tv_attention_value.setText("--");
        numbeOfSamples = 0;
        isProcessing = false;
        tv_attention_notification.setText("Nhấn Start để bắt đầu");
        stopAlertService();
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
        tv_attention_notification.setText("Nhấn Start để bắt đầu");
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

    private boolean isPressing = false;
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
                            dataForInfer = dataCollected.clone();
                            AsyncTaskInfer runner = new AttentionActivity.AsyncTaskInfer();
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


    public void showToast(final String msg,final int timeStyle){
        AttentionActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }

    private class AsyncTaskInfer extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // run training process here
            EEGPower[] EEGdata = dataForInfer.clone();
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

            INDArray sample_to_infer = Nd4j.create(ArrayUtil.flattenDoubleArray(sample), sampleShape);
            INDArray predicted = TrainModel.model.output(sample_to_infer, false);
            INDArray index = predicted.argMax();
            int[] pl = index.toIntVector();
            currentStatus = pl[0];
            if(pl[0] == 0) {
                alertService();
            }

            return null;
        }

        //This block executes in UI when background thread finishes
        //This is where we update the UI with our classification results
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            String predicted_label = "Bạn đang " + LocalDataSet.statues[currentStatus].toLowerCase() + ".";
            tv_attention_value.setText(predicted_label);
        }
    }

    public void alertService() {
        Bundle b = new Bundle();
        b.putBoolean("Status", true);
        intent.putExtra("Alert", b);
        startService(intent);
    }

    public void stopAlertService() {
        Bundle b = new Bundle();
        b.putBoolean("Status", false);
        intent.putExtra("Alert", b);
        startService(intent);
    }

    private void setFailState(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = findViewById(R.id.tv_attention_notification);
                textView.setText("Mất kết nối!");
                if(tgStreamReader != null){
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }
            }
        });
    }
}