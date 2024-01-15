package com.example.brainwave;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MonitoringActivity extends AppCompatActivity{

    private static final String TAG = FileDemoActivity.class.getSimpleName();

    private Button btn_start = null;
    private Button btn_stop = null;
    private TextView tv_status_label;
    private TextView tv_start_time;

    private int badPacketCount = 0;

    private TgStreamReader tgStreamReader;
    private BluetoothAdapter mBluetoothAdapter;

    private String filename = "_EEGData.txt";

    private String foldername = "Focuson";
    File myExternalFile;
    FileOutputStream fileOutputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 99);
        }
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.monitoring_view);

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

        // check if external storage is available and not read only
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            btn_start.setEnabled(false);
        }
//        else {
////            myFolder = Environment.getExternalStoragePublicDirectory(foldername);
////            myExternalFile = new File(myFolder, filename);
////            if (!myFolder.exists()) {
////                myFolder.mkdirs();
////            }
//            myExternalFile = new File(getExternalFilesDir(foldername), filename);
//        }
        // Example of constructor public TgStreamReader(BluetoothAdapter ba, TgStreamHandler tgStreamHandler)
        tgStreamReader = new TgStreamReader(mBluetoothAdapter,callback);
        // (2) Demo of setGetDataTimeOutTime, the default time is 5s, please call it before connect() of connectAndStart()
        tgStreamReader.setGetDataTimeOutTime(6);
        // (3) Demo of startLog, you will get more sdk log by logcat if you call this function
        tgStreamReader.startLog();
    }

    private void initView() {
        tv_status_label = (TextView) findViewById(R.id.tv_status_label);
        tv_start_time = (TextView) findViewById(R.id.tv_start_time);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_stop.setEnabled(false);

        btn_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                myExternalFile = new File(getExternalFilesDir(foldername), getCurrentTime().substring(0,19) + filename);
                badPacketCount = 0;

//                // (3) How to destroy a TgStreamReader object
//                if(tgStreamReader != null){
//                    tgStreamReader.stop();
//                    tgStreamReader.close();
//                    tgStreamReader = null;
//                }
//                InputStream is = getApplicationContext().getResources().openRawResource(R.raw.tgam_capture);
//                // Example of TgStreamReader(InputStream is, TgStreamHandler tgStreamHandler)
//                tgStreamReader = new TgStreamReader(is, callback);
//
//                // (1) Example of setReadFileBlockSize(int), the default block size is 8, call it before connectAndStart() or connect()
//                tgStreamReader.setReadFileBlockSize(16);
//                // (2) Example of setReadFileDelay(int), the default delay time is 2ms, call it before connectAndStart() or connect()
//                tgStreamReader.setReadFileDelay(2);
//
//                tgStreamReader.connectAndStart();

                if(tgStreamReader != null && tgStreamReader.isBTConnected()){

                    // Prepare for connecting
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }
                // (4) Demo of  using connect() and start() to replace connectAndStart(),
                // please call start() when the state is changed to STATE_CONNECTED
                tgStreamReader.connect();
//				tgStreamReader.connectAndStart();

                tv_status_label.setText("Đang thực hiện...");
                tv_start_time.setText("Thời điểm bắt đầu: " + getCurrentTime().substring(10,19));
                openFile(myExternalFile);
                btn_start.setEnabled(false);
                btn_stop.setEnabled(true);
            }
        });

        btn_stop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                stop();
                tv_status_label.setText("");
                tv_start_time.setText("");
                showToast("File saved!", Toast.LENGTH_SHORT);
                tv_status_label.setText("Địa chỉ tệp:");
                tv_start_time.setText(myExternalFile.getAbsolutePath());
            }
        });

//        btnSave.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                myData = "";
////                try {
////                    FileOutputStream fos = new FileOutputStream(myExternalFile);
////                    fos.write(myInputText.getText().toString().getBytes());
////                    fos.close();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//                writeData(myExternalFile, myInputText.getText().toString());
//                myInputText.setText("");
//                responseText.setText("Dữ liệu đã được lưu vào bộ nhớ ngoài");
//            }
//        });
//        readFromExternalStorage = (Button) findViewById(R.id.btnDisplay);
//        readFromExternalStorage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                myData = "";
//                try {
//                    FileInputStream fis = new FileInputStream(myExternalFile);
//                    DataInputStream in = new DataInputStream(fis);
//                    BufferedReader br = new BufferedReader(
//                            new InputStreamReader(in));
//                    String strLine;
//                    while ((strLine = br.readLine()) != null) {
//                        myData = myData + strLine;
//                    }
//                    in.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                myInputText.setText(myData);
//                responseText.setText("Được lấy ra từ bộ nhớ ngoài");
//            }
//        });
    }

    public void stop() {
        if(tgStreamReader != null){
            tgStreamReader.stop();
            tgStreamReader.close();
        }
        closeFile(myExternalFile);
        btn_start.setEnabled(true);
        btn_stop.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        if(tgStreamReader != null){
            tgStreamReader.close();
            tgStreamReader = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        stop();
    }

    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
//                case ConnectionStates.STATE_CONNECTED:
//                    //sensor.start();
//                    showToast("Connected", Toast.LENGTH_SHORT);
//                    break;
//                case ConnectionStates.STATE_WORKING:
//
//                    break;
//                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
//                    //  get data time out
//                    break;
//                case ConnectionStates.STATE_COMPLETE:
//                    //read file complete
//                    showToast("STATE_COMPLETE",Toast.LENGTH_SHORT);
//                    break;
//                case ConnectionStates.STATE_STOPPED:
//                    break;
//                case ConnectionStates.STATE_DISCONNECTED:
//                    break;
//                case ConnectionStates.STATE_ERROR:
//                    break;
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    tgStreamReader.start();
                    showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    // Do something when working

                    //(9) demo of recording raw data , stop() will call stopRecordRawData,
                    //or you can add a button to control it.
                    //You can change the save path by calling setRecordStreamFilePath(String filePath) before startRecordRawData
                    tgStreamReader.startRecordRawData();

                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    // Do something when getting data timeout

                    //(9) demo of recording raw data, exception handling
                    tgStreamReader.stopRecordRawData();

                    showToast("Get data time out!", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_STOPPED:
                    // Do something when stopped
                    // We have to call tgStreamReader.stop() and tgStreamReader.close() much more than
                    // tgStreamReader.connectAndstart(), because we have to prepare for that.

                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    // Do something when disconnected
                    break;
                case ConnectionStates.STATE_ERROR:
                    // Do something when you get error message
                    break;
                case ConnectionStates.STATE_FAILED:
                    showToast("Connection failed!\nPlease check your bluetooth device", Toast.LENGTH_SHORT);
                    // Do something when you get failed message
                    // It always happens when open the BluetoothSocket error or timeout
                    // Maybe the device is not working normal.
                    // Maybe you have to try again
                    break;
            }
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onRecordFail(int a) {
            // TODO Auto-generated method stub
            Log.e(TAG,"onRecordFail: " +a);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // TODO Auto-generated method stub

            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // TODO Auto-generated method stub
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
//                    updateWaveView(msg.arg1);
                    break;
                case MindDataType.CODE_MEDITATION:
                    Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);
                    writeDataToFile(getCurrentTime(), "CODE_MEDITATION", msg.arg1);
//                    tv_meditation.setText("" +msg.arg1 );
                    break;
                case MindDataType.CODE_ATTENTION:
                    Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
                    writeDataToFile(getCurrentTime(), "CODE_ATTENTION", msg.arg1);
//                    tv_attention.setText("" +msg.arg1 );
                    break;
                case MindDataType.CODE_EEGPOWER:
                    EEGPower power = (EEGPower)msg.obj;
                    if(power.isValidate()){
                        writeEEGDataToFile(getCurrentTime(), power);
                    }
                    break;
                case MindDataType.CODE_POOR_SIGNAL://
                    int poorSignal = msg.arg1;
                    Log.d(TAG, "poorSignal:" + poorSignal);
                    writeDataToFile(getCurrentTime(), "poorSignal", msg.arg1);
//                    tv_ps.setText(""+msg.arg1);

                    break;
                case MSG_UPDATE_BAD_PACKET:
                    writeDataToFile(getCurrentTime(), "badpacket", msg.arg1);
//                    tv_badpacket.setText("" + msg.arg1);

                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };


    public void showToast(final String msg,final int timeStyle){
        MonitoringActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }

//    private void writeData(File myFile, String data) {
//        try {
//            if (!myFile.exists()) {
//                myFile.createNewFile();
//            } else {
//                FileOutputStream fileOutputStream = null;
//                try {
//                    fileOutputStream = new FileOutputStream(myFile, false);
//                    fileOutputStream.write(data.getBytes());
//                    Toast.makeText(this, "Done" + myFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    if (fileOutputStream != null) {
//                        try {
//                            fileOutputStream.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void openFile(File myFile) {
        try {
            if (!myFile.exists()) {
                myFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(myFile, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeFile(File myFile) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
//                Toast.makeText(this, "Done" + myFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Kiểm tra xe bộ nhớ ngoài SDCard có readonly không vì nếu là readonly thì
     * không thể tạo file trên đó được
     */
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    /*
     * Kiểmtra xem device có bộ nhớ ngoài không
     */
    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public void writeDataToFile(String currentTime, String label, int value) {
        String data = currentTime + ',' + label + ',' + value + "\n";
        try {
            fileOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            onStart();
        }
    }

    public void writeEEGDataToFile(String currentTime, EEGPower power) {
        String data = currentTime + ',' + "delta" + ',' + power.delta + "\n"
                + currentTime + ',' + "theta" + ',' + power.theta + "\n"
                + currentTime + ',' + "lowalpha" + ',' + power.lowAlpha + "\n"
                + currentTime + ',' + "highalpha" + ',' + power.highAlpha + "\n"
                + currentTime + ',' + "lowbeta" + ',' + power.lowBeta + "\n"
                + currentTime + ',' + "highbeta" + ',' + power.highBeta + "\n"
                + currentTime + ',' + "lowgamma" + ',' + power.lowGamma + "\n"
                + currentTime + ',' + "middlegamma" + ',' + power.middleGamma + "\n";
        try {
            fileOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String strDate = sdf.format(c.getTime());
        return strDate;
    }
}