package com.alejandro_castilla.cloudfitforwear.activities;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.activities.adapters.PracticeActivityGridPagerAdapter;
import com.alejandro_castilla.cloudfitforwear.data.HeartRate;
import com.alejandro_castilla.cloudfitforwear.data.WearableTraining;
import com.alejandro_castilla.cloudfitforwear.services.bluetooth.BluetoothService;
import com.alejandro_castilla.cloudfitforwear.services.zephyrsensor.ZephyrService;
import com.alejandro_castilla.cloudfitforwear.utilities.StaticVariables;

import java.util.ArrayList;

public class TrainingActivity extends WearableActivity implements View.OnClickListener,
        SensorEventListener {

    private final String TAG = TrainingActivity.class.getSimpleName();

    /*Fields for the views used on the layout*/

    private Chronometer chronometer;
    private ImageView resumeActionImgView, pauseActionImgView, exitActionImgView;
    private TextView heartRateTextView, resumeActionTextView, pauseActionTextView;
    private GridViewPager gridViewPager;

    /* Bluetooth fields */

    private BluetoothDevice targetDevice;

    /* Preferences fields */

    private SharedPreferences sharedPref;
    private boolean zephyrEnabled;

    /* Status fields */

    private boolean sessionPaused = false;

    /* Chronometer fields */

    private long timeWhenPaused;
    private boolean chronoAllowedToStart = true;

    /* Internal sensors fields */

    private SensorManager sensorManager;
    private Sensor heartRateInternalSensor;

    /* Data fields */

    private WearableTraining training;
    private ArrayList<HeartRate> heartRateList;

    /* Fields to connect to services */

    private BluetoothService bluetoothService;
    private boolean bluetoothServiceBinded = false;
    private ZephyrService zephyrService;
    private boolean zephyrServiceBinded = false;

    private ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.BluetoothServiceBinder bluetoothServiceBinder =
                    (BluetoothService.BluetoothServiceBinder) service;
            bluetoothService = bluetoothServiceBinder.getService();
            bluetoothService.findBluetoothDevice("C8:3E:99:0D:DD:43");
            //TODO This mac address should be synced from the phone (and stored on SharedPreferences)
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection zephyrServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ZephyrService.ZephyrServiceBinder zephyrServiceBinder =
                    (ZephyrService.ZephyrServiceBinder) service;
            zephyrService = zephyrServiceBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /*Messenger fields*/

    private final Handler messageHandler = new Handler() {

        long timeMark;
        int heartRateInt;

        @Override
        public void handleMessage(Message msg) {
            timeMark = SystemClock.elapsedRealtime() - chronometer.getBase();
            switch (msg.what) {
                case StaticVariables.DEVICE_FOUND:
                    Log.d(TAG, "Device received on " + TAG);
                    targetDevice = (BluetoothDevice) msg.obj;
                    zephyrService.connectToZephyr(targetDevice);
                    break;
                case StaticVariables.ZEPHYR_HEART_RATE:
                    startChronometer(chronoAllowedToStart, SystemClock.elapsedRealtime());
                    chronoAllowedToStart = false;
                    pauseActionImgView.setOnClickListener(TrainingActivity.this);

                    if (!sessionPaused) {
                        String heartRateString = msg.getData().getString("heartratestring");
                        timeMark = SystemClock.elapsedRealtime() - chronometer.getBase();
                        heartRateInt = Integer.parseInt(heartRateString);
                        saveHeartRate(timeMark, heartRateInt);
                        heartRateTextView.setText(heartRateString);
                    }
                    break;
                case StaticVariables.DEVICE_NOT_FOUND:
                    Intent intent = new Intent (TrainingActivity.this,
                            ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                            ConfirmationActivity.FAILURE_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                            "No se ha podido conectar con el sensor Zephyr");
                    startActivity(intent);
                    finish();
            }
            super.handleMessage(msg);
        }
    };

    private Messenger practiceActivityMessenger = new Messenger(messageHandler);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                chronometer = (Chronometer) findViewById(R.id.practiceChronometer);
                heartRateTextView = (TextView) findViewById(R.id.heartRateText);
                resumeActionImgView = (ImageView) findViewById(R.id.practiceResumeActionImg);
                pauseActionImgView = (ImageView) findViewById(R.id.practicePauseActionImg);
                pauseActionTextView = (TextView) findViewById(R.id.practicePauseActionText);
                resumeActionTextView = (TextView) findViewById(R.id.practiceResumeActionText);
                exitActionImgView = (ImageView) findViewById(R.id.practiceExitActionImg);

                resumeActionImgView.setOnClickListener(TrainingActivity.this);
                exitActionImgView.setOnClickListener(TrainingActivity.this);

                gridViewPager = (GridViewPager) stub.findViewById(R.id.practicePager);
                gridViewPager.setAdapter(new PracticeActivityGridPagerAdapter());
                gridViewPager.setOffscreenPageCount(2);
                DotsPageIndicator dotsPageIndicator = (DotsPageIndicator)
                        findViewById(R.id.practicePageIndicator);
                dotsPageIndicator.setPager(gridViewPager);

                //TODO Read WearableTraining and set parameters
                checkSharedPreferences();
                heartRateList = new ArrayList<HeartRate>();

                if (zephyrEnabled) {
                    // Start bluetooth and Zephyr sensor services
                    Intent bluetoothServiceIntent = new Intent(TrainingActivity.this,
                            BluetoothService.class);
                    bluetoothServiceIntent.putExtra("messenger", practiceActivityMessenger);
                    bindService(bluetoothServiceIntent, bluetoothServiceConnection,
                            Context.BIND_AUTO_CREATE);
                    bluetoothServiceBinded = true;

                    Intent zephyrServiceIntent = new Intent(TrainingActivity.this,
                            ZephyrService.class);
                    zephyrServiceIntent.putExtra("messenger", practiceActivityMessenger);
                    bindService(zephyrServiceIntent, zephyrServiceConnection,
                            Context.BIND_AUTO_CREATE);
                    zephyrServiceBinded = true;
                } else {
                    //Initialize internal heart rate sensor (if it's available)
                    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    heartRateInternalSensor = sensorManager
                            .getDefaultSensor(Sensor.TYPE_HEART_RATE);
                    if (heartRateInternalSensor == null) {
                        Intent intent = new Intent (TrainingActivity.this,
                                ConfirmationActivity.class);
                        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                ConfirmationActivity.FAILURE_ANIMATION);
                        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                                "Este dispositivo no tiene pulsómetro");
                        startActivity(intent);
                        finish();
                    }
                    sensorManager.registerListener(TrainingActivity.this, heartRateInternalSensor,
                            SensorManager.SENSOR_DELAY_UI);
                }

            }
        });

        setAmbientEnabled();
    }

    @Override
    public void onClick(View v) {
        int resId = v.getId();
        long timeElapsed;
        switch (resId) {
            case R.id.practicePauseActionImg:
                timeWhenPaused = SystemClock.elapsedRealtime() - chronometer.getBase();
                chronometer.stop();
                sessionPaused = true;
                pauseActionImgView.setVisibility(View.GONE);
                resumeActionImgView.setVisibility(View.VISIBLE);
                pauseActionTextView.setVisibility(View.GONE);
                resumeActionTextView.setVisibility(View.VISIBLE);
                break;
            case R.id.practiceResumeActionImg:
                startChronometer(true, SystemClock.elapsedRealtime() - timeWhenPaused);
                sessionPaused = false;
                pauseActionImgView.setVisibility(View.VISIBLE);
                resumeActionImgView.setVisibility(View.GONE);
                pauseActionTextView.setVisibility(View.VISIBLE);
                resumeActionTextView.setVisibility(View.GONE);
                break;
            case R.id.practiceExitActionImg:
                if (sessionPaused) {
                    timeElapsed = timeWhenPaused;
                } else {
                    timeElapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
                }

                //TODO Save training data
//                TrainingJSONParser parser = new TrainingJSONParser(trainingData);
//                String json = parser.writeToJSON();
//                sharedPrefEditor.putString(KEY_PREF_SESSIONS_JSON, sessionsJSONString + json);
//                sharedPrefEditor.commit();
//                Log.d(TAG, json);
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (bluetoothServiceBinded) {
            unbindService(bluetoothServiceConnection);
            bluetoothServiceBinded = false;
        }

        if (zephyrServiceBinded) {
            unbindService(zephyrServiceConnection);
            zephyrServiceBinded = false;
        }

        if (!zephyrEnabled) {
            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }

    private void checkSharedPreferences() {
        //Check if the user wants to use Zephyr Sensor
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        zephyrEnabled = sharedPref.getBoolean(StaticVariables.KEY_PREF_ZEPHYR_ENABLED, false);
    }

    private void startChronometer(boolean allowed, long baseTime) {
        if (allowed) {
            chronometer.setBase(baseTime);
            chronometer.start();
        }
    }

    private void saveHeartRate(long timeMark, int heartRate) {
        HeartRate hr = new HeartRate(timeMark, heartRate);
        heartRateList.add(hr);
    }

//    private void saveCurrentDate() {
//        Calendar calendar = Calendar.getInstance();
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMMM-yyyy kk:mm:ss");
//        String date = sdf.format(calendar.getTime());
//        trainingData.setDate(date);
//    }

    /* Methods for internal heart rate sensor */

    @Override
    public void onSensorChanged(SensorEvent event) {

        startChronometer(chronoAllowedToStart, SystemClock.elapsedRealtime());
        chronoAllowedToStart = false;
        pauseActionImgView.setOnClickListener(TrainingActivity.this);

        if (!sessionPaused) {
            float heartRateFloat = event.values[0];
            int heartRateInt = Math.round(heartRateFloat);
            long timeMark = SystemClock.elapsedRealtime() - chronometer.getBase();

            saveHeartRate(timeMark, heartRateInt);

            heartRateTextView.setText(Integer.toString(heartRateInt));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Nothing to do here.
    }
}
