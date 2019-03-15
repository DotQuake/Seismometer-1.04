package com.example.admindeveloper.seismometer;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.DataAcquisition.Bluetooth;
import com.example.admindeveloper.seismometer.DataAcquisition.DataService;
import com.example.admindeveloper.seismometer.DataAcquisition.DataStreamTask;
import com.example.admindeveloper.seismometer.RealTimeServices.RealTimeController;
import com.example.admindeveloper.seismometer.UploadServices.ZipManager;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadServiceBroadcastReceiver;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class Background extends Service {


    public static final String SERVICE_READY="Background.ServiceReady";

    RealTimeController realTimeController;
    Handler handler;
    ZipManager zipManager;
    String UPLOAD_URL;

    ArrayList<String> csvnames, deletenames;
    FileObserver fileObservercsv;
    FileObserver fileObserverzip;

    int sec;

    boolean compressionflag = false;

    long StartTime;
    String time;

    String fileName;

    String ipaddress;

    String longitude;
    String latitutde;
    String compass;
    String location;

    private LocationManager locationManager;
    private LocationListener locationListener;

    Runnable runnable;
    long resettime=0;
    int ctr=0;

    Boolean mystart = true;
    Boolean myexit = false;

    FileOutputStream fos;
    BufferedWriter bw;

    String upload_name;
    int upload_index;
    boolean successful;

    public static boolean ServiceStarted=false;

    private final BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(DataService.DATA)){
                saveData(intent.getFloatExtra(DataService.GET_X,0),
                        intent.getFloatExtra(DataService.GET_Y,0),
                        intent.getFloatExtra(DataService.GET_Z,0),
                        intent.getStringExtra(DataService.GET_COMPASS));
            }
        }
    };

    private void saveData(float x,float y,float z,String compass)
    {
        this.compass=compass;
        realTimeController.updateXYZ(x,y,z);
        if(Background.ServiceStarted) {
            try {
                if (mystart) {
                    if(DataService.DeviceConnected&&NavigationDrawer.isGainChange()) {
                        if (!Bluetooth.sendData((char) (NavigationDrawer.getSelectedIndex() + 48)))
                            Toast.makeText(getApplicationContext(), "Error Updating Gain", Toast.LENGTH_SHORT).show();
                        else {
                            DataStreamTask.calibrate();
                            NavigationDrawer.gainChangeRequestIsDone();
                            Toast.makeText(getApplicationContext(),"Gain Updated Successfully",Toast.LENGTH_SHORT).show();
                        }
                    }
                    ctr = 0;
                    File myDir = new File("storage/emulated/0/Samples");
                    if (!myDir.exists()) {
                        myDir.mkdirs();
                    }
                    File file = new File(myDir, fileName);
                    file.createNewFile();
                    fos = new FileOutputStream(file);
                    bw = new BufferedWriter(new OutputStreamWriter(fos));
                    bw.write("ARRIVALS,,,,\r\n");
                    bw.write("#sitename,,,,\r\n");
                    bw.write("#onset,,,,\r\n");
                    bw.write("#first motion,,,,\r\n");
                    bw.write("#phase,,,,\r\n");
                    bw.write("#year month day,,,,\r\n");
                    bw.write("#hour minute,,,,\r\n");
                    bw.write("#second,,,,\r\n");
                    bw.write("#uncertainty in seconds,longitude :, " + longitude + ",,\r\n");
                    bw.write("#peak amplitude,latitude :, " + latitutde + ",,\r\n");
                    bw.write("#frequency at P phase,compass :, " + compass + ",,\r\n");
                    bw.write(",,,,\r\n");
                    bw.write("TIME SERIES,,,,\r\n");
                    bw.write("LLPS,LLPS,LLPS,#sitename,\r\n");
                    bw.write("EHE _,EHN _,EHZ _,#component,\r\n");
                    bw.write(0 + "," + 0 + "," + 0 + ",#authority,\r\n");
                    String[] separated = fileName.split("[-|.]");
                    String year = separated[0];
                    String month = separated[1];
                    String day = separated[2];
                    String hour = separated[3];
                    String minute = separated[4];
                    String second = separated[5];
                    String hold = year;
                    hold = Integer.parseInt(month) <= 9 ? hold + "0" + Integer.parseInt(month) : hold + Integer.parseInt(month);
                    hold = Integer.parseInt(day) <= 9 ? hold + "0" + Integer.parseInt(day) : "" + hold + Integer.parseInt(day);
                    bw.write(hold + "," + hold + "," + hold + ",#year month day,\r\n");
                    hold = hour;
                    hold = Integer.parseInt(minute) <= 9 ? hold + "0" + Integer.parseInt(minute) : "" + hold + Integer.parseInt(minute);
                    if(hold.length()<4)
                        hold="0"+hold;
                    bw.write(hold + "," + hold + "," + hold + ",#hour minute,\r\n");
                    bw.write(second + "," + second + "," + second + ",#second,\r\n");
                    if (DataService.isDataFromDevice()) {
                        bw.write(860 + "," + 860 + "," + 860 + ",#samples per second,\r\n");
                    }else{
                        bw.write(50 + "," + 50 + "," + 50 + ",#samples per second,\r\n");
                    }
                    bw.write("0,0,0,#sync,\r\n");
                    bw.write(",,,#sync source,\r\n");
                    if (DataService.isDataFromDevice()) {
                        bw.write("count,count,count,#unit,\r\n");
                        bw.write("External,External,External,#data source,\r\n");
                        String conversionValue=String.valueOf(NavigationDrawer.getCurrentSelectedGain());
                        bw.write(conversionValue+" uV,"+conversionValue+" uV,"+conversionValue+" uV,"+"#conversion value,\r\n");
                    }
                    else {
                        bw.write("g,g,g,#unit,\r\n");
                        bw.write("Internal,Internal,Internal,#data source.\r\n");
                    }
                    bw.write("--------,--------,--------,,\r\n");
                    mystart = false;
                }
                bw.write(realTimeController.getX() + "," + realTimeController.getY() + "," + realTimeController.getZ() + ",,\r\n");
                if (myexit) {
                    ctr = 0;
                    compressionflag = true;
                    bw.write("       ,       ,       ,,\r\n" +
                            "       ,       ,       ,,\r\n" +
                            "END,END,END,,\r\n");
                    bw.flush();
                    bw.close();
                    fos.flush();
                    fos.close();
                    myexit = false;
                    mystart = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                myexit = false;
                mystart = true;
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longitude = String.valueOf(location.getLongitude());
                latitutde = String.valueOf(location.getLatitude());
                //Toast.makeText(getApplicationContext(),"Location Updated",Toast.LENGTH_SHORT).show();
                if(!Background.ServiceStarted)
                {
                    Background.ServiceStarted=true;
                    Intent serviceReadyIntent=new Intent();
                    serviceReadyIntent.setAction(Background.SERVICE_READY);
                    sendBroadcast(serviceReadyIntent);
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000,0,locationListener);

        IntentFilter intentFilter=new IntentFilter(DataService.DATA);
        registerReceiver(mBroadcastReceiver,intentFilter);

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        //region ---------Initialization ------------------
        Log.e("BLE","START");
        StartTime = SystemClock.uptimeMillis();
        ipaddress = intent.getStringExtra("ipaddress");
        location = intent.getStringExtra("location");
        csvnames = new ArrayList<>();
        deletenames = new ArrayList<>();
        zipManager = new ZipManager();
        realTimeController = new RealTimeController();
        handler = new Handler();
        //endregion
        //Toast.makeText(getApplication(), "Services Enabled", Toast.LENGTH_SHORT).show();
        //region ------------------- Set up for Delay / Start Up --------------------
        Calendar settime1 = Calendar.getInstance();
        sec = (60 - settime1.get(Calendar.SECOND)) * 1000;
        resettime = SystemClock.uptimeMillis();
        //endregion

        //region ---------------------(HANDLER) Special Delay Call (Infinite Loop in an definite delay)--------------------
        Calendar setnamedate1 = Calendar.getInstance();
        fileName = setnamedate1.get(Calendar.YEAR) + "-" + (setnamedate1.get(Calendar.MONTH)+1)  + "-" + setnamedate1.get(Calendar.DATE)  + "-" + setnamedate1.get(Calendar.HOUR_OF_DAY)  + "-" + setnamedate1.get(Calendar.MINUTE)  + "-" + setnamedate1.get(Calendar.SECOND)  + ".csv";
        csvnames.add(fileName);
        runnable = new Runnable() {
            @Override
            public void run() {
                myexit = true;
                resettime = SystemClock.uptimeMillis();
                //somechanges---

                Calendar setnamedate = Calendar.getInstance();
                fileName = setnamedate.get(Calendar.YEAR) + "-" + (setnamedate.get(Calendar.MONTH)+1)  + "-" + setnamedate.get(Calendar.DATE)  + "-" + setnamedate.get(Calendar.HOUR_OF_DAY)  + "-" + setnamedate.get(Calendar.MINUTE)  + "-" + setnamedate.get(Calendar.SECOND)  + ".csv";
                csvnames.add(fileName);
                //------------------ Initialize Delay for the next Call -----------------
                Calendar settime = Calendar.getInstance();
                sec = (60 - settime.get(Calendar.SECOND)) * 1000; // seconds delay for minute
                // ----------------- Recursive Call --------------------------
                handler.postDelayed(this, sec);
            }
        };
        handler.postDelayed(runnable, sec); // calling handler for infinite loop
        //endregion

        //region --------- FileObserver for Compression and Upload -------
        final String csvpath = android.os.Environment.getExternalStorageDirectory().toString() + "/Samples/";
        fileObservercsv = new FileObserver(csvpath,FileObserver.ALL_EVENTS) {
            @Override
            public void onEvent(int event, final String file) {
                if (event == FileObserver.CLOSE_WRITE && compressionflag) {
                    // Log.d("MediaListenerService", "File created [" + csvpath + file + "]");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // Toast.makeText(getBaseContext(), file + " was saved!", Toast.LENGTH_SHORT).show();
                            zipManager.compressGzipFile("Samples/" + file,  file + ".gz");  // Compressing Data
                            compressionflag = false;
                            if(successful){
                                for (int i = 0; i < deletenames.size(); i++) {
                                    try {
                                        csvnames.remove(deletenames.get(i));
                                        File file1 = new File("/storage/emulated/0/Samples/", deletenames.get(i));
                                        boolean deleted1 = file1.delete();
                                        File file2 = new File("/storage/emulated/0/Zip/", deletenames.get(i) + ".gz");
                                        boolean deleted2 = file2.delete();
                                        Toast.makeText(getApplicationContext(), deletenames.get(i)+" deleted", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                deletenames.clear();
                                successful = false;
                            }
                            for(int ictr=0 ; ictr<csvnames.size()-1 ; ictr++) {
                                upload_name = csvnames.get(ictr);
                                upload_index = ictr;
                                uploadMultipart("/storage/emulated/0/Zip/" + csvnames.get(ictr) + ".gz");
                            }
                        }
                    });
                }
            }
        };
        fileObservercsv.startWatching();
        //endregion
/*
        //region  -------- FileObserver for Sending Data to Database -------------
        final String zippath = android.os.Environment.getExternalStorageDirectory().toString() + "/Zip/";
        fileObserverzip = new FileObserver(zippath,FileObserver.ALL_EVENTS) {
            @Override
            public void onEvent(int event, final String file) {
                if (event == FileObserver.CREATE) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("ZIP",file);
                            //Toast.makeText(getBaseContext(), file, Toast.LENGTH_SHORT).show();
                            if(successful){
                                for (int i = 0; i < deletenames.size(); i++) {
                                    try {
                                        csvnames.remove(deletenames.get(i));
                                        File file1 = new File("/storage/emulated/0/Samples/", deletenames.get(i));
                                        boolean deleted1 = file1.delete();
                                        File file2 = new File("/storage/emulated/0/Zip/", deletenames.get(i) + ".gz");
                                        boolean deleted2 = file2.delete();
                                        Toast.makeText(getApplicationContext(), deletenames.get(i)+" deleted", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                deletenames.clear();
                                successful = false;
                            }
                            for(int ictr=0 ; ictr<csvnames.size()-1 ; ictr++) {
                                upload_name = csvnames.get(ictr);
                                upload_index = ictr;
                                uploadMultipart("/storage/emulated/0/Zip/" + csvnames.get(ictr) + ".gz");
                            }
                        }
                    });
                }
            }
        };
        fileObserverzip.startWatching();
        //endregion
        */


        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        unregisterReceiver(mBroadcastReceiver);
        if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
        Background.ServiceStarted=false;
        Toast.makeText(this,"Service Stopped",Toast.LENGTH_SHORT).show();
    }



    public void uploadMultipart(String path) {
        //getting name for the string path
        UPLOAD_URL = "http://"+ipaddress+"/data/api/uploaddata.php";
        String[] separated = upload_name.split("[-|.]");
        String year = separated[0];
        String month = separated[1];
        String day = separated[2];
        String hour = separated[3];
        String minute = separated[4];

        //getting the actual path of the image
        if (path == null) {

            Toast.makeText(this, "NULL PATH", Toast.LENGTH_LONG).show();
        } else {
            //Uploading code

            try {
                final String uploadId = UUID.randomUUID().toString();


                //Creating a multi part request
                new MultipartUploadRequest(getApplicationContext(), uploadId, UPLOAD_URL)
                        .addFileToUpload(path, "gz") //Adding file
                        .addParameter("name", upload_name) //Adding text parameter to the request
                        .addParameter("location", location)
                        .addParameter("month", month)
                        .addParameter("day", day)
                        .addParameter("year", year)
                        .addParameter("hour", hour)
                        .addParameter("minute", minute)
                        .setNotificationConfig(new UploadNotificationConfig())
                        .setMaxRetries(1)
                        .setDelegate(new UploadStatusDelegate() {
                            @Override
                            public void onProgress(Context context, UploadInfo uploadInfo) {
                               // Toast.makeText(getApplicationContext(), "Uploading to Server", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
                                Toast.makeText(getApplicationContext(), "Server Connection Failed", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                Toast.makeText(getApplicationContext(), serverResponse.getBodyAsString(), Toast.LENGTH_SHORT).show();
                                String[] s = serverResponse.getBodyAsString().split(",");
                                if(s[0].equals("Successfully Uploaded")) {
                                    deletenames.add(s[1]);
                                    successful = true;
                                }
                            }
                            @Override
                            public void onCancelled(Context context, UploadInfo uploadInfo) {
                                Toast.makeText(getApplicationContext(), "Uploading Cancelled", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .startUpload(); //Starting the upload

            } catch (Exception exc) {
                Toast.makeText(getApplicationContext(), exc.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}

