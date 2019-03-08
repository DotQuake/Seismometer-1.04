package com.example.admindeveloper.seismometer.DataAcquisition;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.Background;
import com.example.admindeveloper.seismometer.NavigationDrawer;

public class DataService extends Service implements SensorEventListener {

    public static final String BLUETOOTH_INIT_FAILED="DataAcquisition.Bluetooth_init_failed";
    public static final String EXTRA_REPORT="DataAcquisition.Extra_Report";
    public static final String DATA="Data";
    public static final String GET_X="Get_X";
    public static final String GET_Y="Get_Y";
    public static final String GET_Z="Get_Z";
    public static final String GET_COMPASS="Get_Compass";

    public  static final String DATASERVICE_STOP="DataAcquisition.Data_Service_Stop";
    public static final String START_SERVICE_DEVICE="DataAcquisition.Start_Service_Device";
    public static final String START_SERVICE_INTERNAL="DataAcquisition.Start_Service_Internal";

    public static boolean ServiceStarted=false;
    private static boolean dataFromDevice =false;
    private Intent i=new Intent();
    private static String deg = "0";
    private SensorManager mSensorManager;
    private DataStreamTask dataStream;


    public static String getDegree(){
        return DataService.deg;
    }
    public static boolean isDataFromDevice() {
        return dataFromDevice;
    }

    public static void startServiceFromRemoteDevice(String deviceName, Context applicationContext)
    {
        Intent intent = new Intent(applicationContext,DataService.class);
        intent.setAction(DataService.START_SERVICE_DEVICE);
        if(deviceName!=null){
            intent.putExtra("Device Name","HC-06");
        }else{
            intent.putExtra("Device Name",deviceName);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent);
           // applicationContext.startService(intent);
        } else {
            applicationContext.startService(intent);
        }
    }
    public static void startServiceFromInternal(Context applicationContext)
    {
        Intent intent = new Intent(applicationContext,DataService.class);
        intent.setAction(DataService.START_SERVICE_INTERNAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent);
            //applicationContext.startService(intent);
        } else {
            applicationContext.startService(intent);
        }
    }

    private final static Handler myHandler=new Handler();
    private Runnable reconnectRunnable=new Runnable() {
        @Override
        public void run() {
            if(!DataService.DeviceConnected) {
                RFCOMMEstablish();
                myHandler.postDelayed(reconnectRunnable, 5000);
            }else{
                myHandler.removeCallbacks(reconnectRunnable);
                DataService.ReconnectHasStarted=false;
            }
        }
    };

    private static  boolean ReconnectHasStarted=false;
    public static boolean DeviceConnected=false;
    private static boolean DataStreamHasStarted=false;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action!=null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:

                        int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                        switch (mode) {
                            case BluetoothAdapter.STATE_ON: {
                                if (!Bluetooth.queryDevice()) {
                                    Bluetooth.startDiscovery();
                                } else {
                                    RFCOMMEstablish();
                                }
                                break;
                            }
                        }
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        // Discovery has found a device. Get the BluetoothDevice
                        // object and its info from the Intent.
                        try {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (Bluetooth.registerAsBluetoothDevice(device)) {
                                Bluetooth.stopDiscovery();
                                RFCOMMEstablish();
                            }
                        } catch (Exception e){Log.e("DiscoveryException",e.getMessage());}
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        DataService.DeviceConnected = true;
                        DataStreamHasStarted=true;
                        DataStreamTask.calibrate();
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        DataService.DeviceConnected = false;
                        DataStreamHasStarted=false;
                        if(dataStream.getStatus()== AsyncTask.Status.RUNNING)
                            dataStream.cancel(true);
                        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                        StartReconnect();
                        break;
                }
            }
        }
    };

    public DataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals(DataService.START_SERVICE_DEVICE)){
            DataService.dataFromDevice =true;
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            registerReceiver(mBroadcastReceiver, filter);

            mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
            mSensorManager.registerListener(this, mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION).get(0), SensorManager.SENSOR_DELAY_GAME);
            try{
                Bluetooth.setDeviceName(intent.getExtras().getString("Device Name"));
            }catch (Exception e){}
            int result=Bluetooth.initializeBluetooth();
            switch(result){
                case Bluetooth.RESULT_OK:{
                    RFCOMMEstablish();
                    break;
                }
                case Bluetooth.RESULT_QUERY_FAILED:{
                    Toast.makeText(getApplicationContext(),"Start Discovery, Please make sure the device is on",Toast.LENGTH_SHORT).show();
                    Bluetooth.startDiscovery();
                    break;
                }
                case Bluetooth.RESULT_BLUETOOTH_TURNING_ON:break;
                default:{
                    sendBroadcast(new Intent(DataService.BLUETOOTH_INIT_FAILED).putExtra(DataService.EXTRA_REPORT,result));
                    Toast.makeText(getApplicationContext(),"DataService: Bluetooth Initialize Failed",Toast.LENGTH_SHORT).show();
                }
            }
        }else if(intent.getAction().equals(DataService.START_SERVICE_INTERNAL)){
            DataService.dataFromDevice =false;
            mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
            mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION).get(0), SensorManager.SENSOR_DELAY_GAME);
        }
        DataService.ServiceStarted=true;
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(getApplicationContext(),"DataService Started",Toast.LENGTH_SHORT).show();
        dataStream=new DataStreamTask(getApplicationContext());
    }

    private void StartReconnect(){
        if(!DataService.ReconnectHasStarted) {
            DataService.ReconnectHasStarted=true;
            myHandler.postDelayed(reconnectRunnable, 2000);
        }
    }

    public void StartDataStream(){
        if(!DataService.DataStreamHasStarted) {
            if(Bluetooth.sendData('2'))
                DataService.DataStreamHasStarted=true;
            else
                RFCOMMEstablish();

        }
        if(dataStream.getStatus()== AsyncTask.Status.FINISHED)
            dataStream=new DataStreamTask(getApplicationContext());
        dataStream.execute();
    }

    private void RFCOMMEstablish(){
        switch(Bluetooth.startRFCOMMEstablish()){
            case Bluetooth.RESULT_RFCOMM_ESTABLISH:StartDataStream();break;
            default:{
                Toast.makeText(getApplicationContext(),"DataService: RF Communication Establish Failed",Toast.LENGTH_SHORT).show();
                StartReconnect();
            }
        }
    }

    @Override
    public void onDestroy() {
        if(DataStreamHasStarted) {
            if(!Bluetooth.sendData('2')){
                Toast.makeText(this, "Please Restart The Device", Toast.LENGTH_SHORT).show();
            }
        }
        DataService.DataStreamHasStarted=false;
        if(dataStream.getStatus()== AsyncTask.Status.RUNNING)
            dataStream.cancel(true);
        DataStreamTask.calibrate();
        myHandler.removeCallbacks(reconnectRunnable);
        if(DataService.isDataFromDevice())
            unregisterReceiver(mBroadcastReceiver);
        mSensorManager.unregisterListener(this);
        Bluetooth.disableBluetooth();
        DataService.DeviceConnected=false;
        DataService.ReconnectHasStarted=false;
        Toast.makeText(getApplicationContext(),"DataService Stopped",Toast.LENGTH_SHORT).show();
        sendBroadcast(new Intent(DataService.DATASERVICE_STOP));
        DataService.ServiceStarted=false;
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            i.putExtra(DataService.GET_X,event.values[0]);
            i.putExtra(DataService.GET_Y,event.values[1]);
            i.putExtra(DataService.GET_Z,event.values[2]);
            i.putExtra(DataService.GET_COMPASS,deg);
            i.setAction(DataService.DATA);
            sendBroadcast(i);
        }
        else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            int deg =(int)Math.floor(event.values[0]);
            this.deg=(deg+90)>360?String.valueOf(deg-270):String.valueOf(deg+90);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
