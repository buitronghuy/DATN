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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import java.io.InputStream;

public class FocusingActivity extends AppCompatActivity {

    private static final String TAG = FocusingActivity.class.getSimpleName();
    private TgStreamReader tgStreamReader;

    private BluetoothAdapter mBluetoothAdapter;

    public MediaPlayer player;

    private TextView tv_attention = null;
    private int value_attention = 0;
    private int badPacketCount = 0;
    private int distractionCount = 0;
    private static final int MAX_DISTRACTION = 10;
    private static final int THRESHOLD = 80;

    private int musicCount = 0;
    private static final int MAX_MUSIC_COUNT = 5;

    private Button btn_start = null;
    private Button btn_stop = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 99);
        }
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.focusing_view);

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

        // Example of constructor public TgStreamReader(BluetoothAdapter ba, TgStreamHandler tgStreamHandler)
        tgStreamReader = new TgStreamReader(mBluetoothAdapter,callback);
        // (2) Demo of setGetDataTimeOutTime, the default time is 5s, please call it before connect() of connectAndStart()
        tgStreamReader.setGetDataTimeOutTime(6);
        // (3) Demo of startLog, you will get more sdk log by logcat if you call this function
        tgStreamReader.startLog();

    }

    private void initView() {
        tv_attention = (TextView) findViewById(R.id.tv_attention);

        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);

        btn_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                badPacketCount = 0;
                if(tgStreamReader != null && tgStreamReader.isBTConnected()){

                    // Prepare for connecting
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }

                // (4) Demo of  using connect() and start() to replace connectAndStart(),
                // please call start() when the state is changed to STATE_CONNECTED
                tgStreamReader.connect();
//				tgStreamReader.connectAndStart();

                btn_start.setEnabled(false);
                tv_attention.setText("Connecting...");

                // (3) How to destroy a TgStreamReader object
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
            }
//            @Override
//            public void onClick(View arg0) {
//                badPacketCount = 0;
//
//                // (5) demo of isBTConnected
//                if(tgStreamReader != null && tgStreamReader.isBTConnected()){
//
//                    // Prepare for connecting
//                    tgStreamReader.stop();
//                    tgStreamReader.close();
//                }
//
//                // (4) Demo of  using connect() and start() to replace connectAndStart(),
//                // please call start() when the state is changed to STATE_CONNECTED
//                tgStreamReader.connect();
////				tgStreamReader.connectAndStart();
//            }

        });

        btn_stop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                tv_attention.setText("--");
                stop();
//                tgStreamReader.stop();
//                tgStreamReader.close();
            }

        });

//        btn_play.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                play(v);
//            }
//        });
//
//        btn_pause.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                pause(v);
//            }
//        });
//
//        btn_stop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stopPlayer();
//            }
//        });
    }

    public void playHorn(){
        stopPlayer();
//        if(player == null) {
            player = MediaPlayer.create(this, R.raw.warning_horn);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                    playMusic();
                }
            });
//        }
        player.start();
    }

    public void playMusic(){
        if(player == null) {
            player = MediaPlayer.create(this, R.raw.betawaves14hz);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (musicCount < MAX_MUSIC_COUNT) {
                        musicCount++;
                        playMusic();
                    } else {
                        musicCount = 0;
                        stopPlayer();
                    }
                }
            });
        }
        player.start();
    }

    public void pause(){
        if (player != null) {
            player.pause();
        }
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }


    public void stop() {
        if(tgStreamReader != null){
            tgStreamReader.stop();
            tgStreamReader.close();
        }
        btn_start.setEnabled(true);
        btn_stop.setEnabled(false);
        stopPlayer();
    }

    @Override
    protected void onDestroy() {
        //(6) use close() to release resource
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
        stop();
        super.onStop();
    }

    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
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
            }
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);
        }

        @Override
        public void onRecordFail(int flag) {
            // You can handle the record error message here
            Log.e(TAG,"onRecordFail: " +flag);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // You can handle the bad packets here.
            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // You can handle the received data here
            // You can feed the raw data to algo sdk here if necessary.

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
            // (8) demo of MindDataType
            if (msg.what == MindDataType.CODE_ATTENTION) {
                Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
                tv_attention.setText("" +msg.arg1 );
                checkAttention(msg.arg1);
                if(msg.arg1 > 0 && msg.arg1 <= THRESHOLD) {

                }
            }
//            if (msg.what == MindDataType.CODE_MEDITATION) {
//                Log.d(TAG, "CODE_MEDITATION " + msg.arg1);
//                tv_attention.setText("" +msg.arg1 );
//                if(msg.arg1 < 70 && flag == false) {
////                    play();
//                    flag = true;
//                } else if (msg.arg1 > 85 && flag == true) {
////                    pause();
//                    flag = false;
//                }
//            }
//            switch (msg.what) {
//                case MindDataType.CODE_ATTENTION:
//                    Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
//                    tv_attention.setText("" +msg.arg1 );
//                    break;
//                default:
//                    break;
//            }
            super.handleMessage(msg);
        }
    };


    public void showToast(final String msg,final int timeStyle){
        FocusingActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }

    public void checkAttention(int value_attention) {
        if (value_attention <= 0) {
            distractionCount = 0;
        } else if (value_attention > THRESHOLD) {
            distractionCount = 0;
        } else if (value_attention <= THRESHOLD && distractionCount < MAX_DISTRACTION) {
            distractionCount++;
        } else {
            distractionCount = 0;
//            stopPlayer();
            playHorn();
        }
    }


}