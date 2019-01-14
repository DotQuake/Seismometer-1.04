package com.example.admindeveloper.seismometer;


import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.RealTimeServices.RealTimeController;
import com.example.admindeveloper.seismometer.UploadServices.ZipManager;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Background extends Service implements SensorEventListener {

    /*private LocationManager locationManager;
    private LocationListener locationListener;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Intent i = new Intent("location_update");
                i.putExtra("coordinates",location.getLongitude()+" "+location.getLatitude());
                sendBroadcast(i);
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
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0,locationListener);
    }
*/
    private SensorManager mSensorManager;
    private SimpleDateFormat simpleDateFormat;
    Bundle extras;
    Intent i;
    RecordSaveData recordSaveData;
    RealTimeController realTimeController;
    Handler handler;
    ZipManager zipManager;
    String UPLOAD_URL;

    ArrayList<String> csvnames;
    FileObserver fileObservercsv;
    FileObserver fileObserverzip;
    boolean flag = false;

    String ipaddress;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ---------Initialization ------------------
        ipaddress = intent.getStringExtra("ipaddress");
        Toast.makeText(getApplication(), ipaddress, Toast.LENGTH_SHORT).show();
        csvnames = new ArrayList<>();
        zipManager = new ZipManager();
        extras = new Bundle();
        i = new Intent();
        recordSaveData = new RecordSaveData();
        realTimeController = new RealTimeController();
        handler = new Handler();
        Toast.makeText(getApplication(), "Services Enabled", Toast.LENGTH_SHORT).show();
        // ---------------------Register Listeners for Sensors( Accelerometer / Orientation) Temporarily
        mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION).get(0), SensorManager.SENSOR_DELAY_GAME);
        // ------------------- Set up for Delay / Start Up --------------------
        Calendar calendar = Calendar.getInstance();                     // getting instance
        simpleDateFormat = new SimpleDateFormat("ss");       // format hour
        Date date = calendar.getTime();                             // getting current time
        final int sec = (60 - Integer.parseInt(simpleDateFormat.format(date))) * 1000; // parsing string to int
        //---------------------Special Delay Call (Infinite Loop in an definite delay)--------------------
        final Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                // ------------- Set Up -----------
                Toast.makeText(getApplicationContext(), "Saving in Progress", Toast.LENGTH_SHORT).show();
                Date currentTime = Calendar.getInstance().getTime();
                String fileName = (currentTime.getYear() + 1900) + "-" + (currentTime.getMonth() + 1) + "-" + currentTime.getDate() + "-" + currentTime.getHours() +"-" + currentTime.getMinutes() + "-" + currentTime.getSeconds() + ".csv";

                // -------------- Save / Clear -------------
                String status = recordSaveData.saveEarthquakeData("0", fileName);                                // saving Data to a specific Location (Samples)
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                if(status.equals("Success") && !flag) {
                    recordSaveData.clearData();                                                                 // deleting recorded data
                    csvnames.add(fileName);
                    //------------------ Initialize Delay for the next Call -----------------
                    Date settime = Calendar.getInstance().getTime();
                    int secnew = (60 - settime.getSeconds()) * 1000; // seconds delay for minute
                    // ----------------- Recursive Call --------------------------
                    handler.postDelayed(this, secnew);
                }else if(status.equals("Error") && !flag) {
                    handler.postDelayed(this, 0);
                }else{

                }
            }
        };
        handler.postDelayed(runnable1, sec); // calling handler for infinite loop

       final String csvpath = android.os.Environment.getExternalStorageDirectory().toString() + "/Samples/";
       fileObservercsv = new FileObserver(csvpath,FileObserver.ALL_EVENTS) {
           @Override
           public void onEvent(int event, final String file) {
               if (event == FileObserver.CREATE ) {
                  // Log.d("MediaListenerService", "File created [" + csvpath + file + "]");
                   new Handler(Looper.getMainLooper()).post(new Runnable() {
                       @Override
                       public void run() {
                           Toast.makeText(getBaseContext(), file + " was saved!", Toast.LENGTH_SHORT).show();
                           zipManager.compressGzipFile("Samples/" + file, "Zip/" + file + ".gz");  // Compressing Data

                       }
                   });
               }
           }
       };
       fileObservercsv.startWatching();

        final String zippath = android.os.Environment.getExternalStorageDirectory().toString() + "/Zip/";
        fileObserverzip = new FileObserver(zippath,FileObserver.ALL_EVENTS) {
            @Override
            public void onEvent(int event, final String file) {
                if (event == FileObserver.CREATE ) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), file + " was compressed!", Toast.LENGTH_SHORT).show();
                            for(int ictr=0 ; ictr<csvnames.size() ; ictr++) {
                               uploadMultipart("/storage/emulated/0/Zip/" + csvnames.get(ictr) + ".gz", csvnames.get(ictr),ictr);
                             }

                        }
                    });
                }
            }
        };
        fileObserverzip.startWatching();



        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        flag = true;
        /*if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }*/

    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            realTimeController.updateXYZ(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            recordSaveData.recordData(realTimeController.getX(), realTimeController.getY(), realTimeController.getZ());
            i.putExtra("valueX", String.valueOf(realTimeController.getX()));
            i.putExtra("valueY", String.valueOf(realTimeController.getY()));
            i.putExtra("valueZ", String.valueOf(realTimeController.getZ()));
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            i.putExtra("compass", String.valueOf(sensorEvent.values[0]));
        }
        i.setAction("FILTER");
        sendBroadcast(i);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void uploadMultipart(String path, final String name, final int index) {
        //getting name for the image
        UPLOAD_URL = "http://"+ipaddress+"/data/api/uploaddata.php";
        Date currentTime = Calendar.getInstance().getTime();
        //String name=(currentTime.getYear()+1900)+"-"+(currentTime.getMonth()+1)+"-"+currentTime.getDate()+"-"+currentTime.getHours()+currentTime.getMinutes()+"-"+currentTime.getSeconds()+".csv";
        String[] separated = name.split("-");
        String location = "Lapulapu";
        String year = separated[0];
        String month = separated[1];
        String day = separated[2];
        String hour = separated[3];
        String minute = separated[4];

        //getting the actual path of the image
        //  String path = FilePath.getPath(getActivity(), filePath);

        if (path == null) {

            Toast.makeText(this, "NULL PATH", Toast.LENGTH_LONG).show();
        } else {
            //Uploading code

            try {
                final String uploadId = UUID.randomUUID().toString();


                //Creating a multi part request
                new MultipartUploadRequest(getApplicationContext(), uploadId, UPLOAD_URL)
                        .addFileToUpload(path, "gz") //Adding file
                        .addParameter("name", name) //Adding text parameter to the request
                        .addParameter("location", location)
                        .addParameter("month", month)
                        .addParameter("day", day)
                        .addParameter("year", year)
                        .addParameter("hour", hour)
                        .addParameter("minute", minute)
                        .setNotificationConfig(new UploadNotificationConfig())
                        .setMaxRetries(2)
                        .setDelegate(new UploadStatusDelegate() {
                            @Override
                            public void onProgress(Context context, UploadInfo uploadInfo) {
                                Toast.makeText(getApplicationContext(), "Uploading to Server", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
                                Toast.makeText(getApplicationContext(), "Server Connection Failed", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                Toast.makeText(getApplicationContext(), serverResponse.getBodyAsString(), Toast.LENGTH_SHORT).show();
                                if(serverResponse.getBodyAsString().equals("Successfully Uploaded yehey")) {
                                    File file1 = new File("/storage/emulated/0/Samples/", name);
                                    boolean deleted1 = file1.delete();
                                    File file2 = new File("/storage/emulated/0/Zip/", name + ".gz");
                                    boolean deleted2 = file2.delete();
                                    csvnames.remove(index);
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

