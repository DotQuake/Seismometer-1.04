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
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.RealTimeServices.RealTimeController;
import com.example.admindeveloper.seismometer.UploadServices.ZipManager;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Background extends Service implements SensorEventListener{

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
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        zipManager = new ZipManager();
        extras = new Bundle();
        i = new Intent();
        recordSaveData = new RecordSaveData();
        realTimeController = new RealTimeController();
        handler = new Handler();
        Toast.makeText(getApplication(),"Services Enabled", Toast.LENGTH_SHORT).show();
        mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        mSensorManager.registerListener(this,mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION).get(0), SensorManager.SENSOR_DELAY_GAME);

        Calendar calendar = Calendar.getInstance();                     // getting instance
        simpleDateFormat = new SimpleDateFormat("ss");       // format hour
        Date date = calendar.getTime();                             // getting current time
        final int sec = (60-Integer.parseInt(simpleDateFormat.format(date)))*1000; // parsing string to int

        final Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"Recording in Progress",Toast.LENGTH_SHORT).show();
                String fileName = recordSaveData.saveEarthquakeData("0");                            // saving Data to a specific Location (Samples)
                recordSaveData.clearData();                                                                 // deleting recorded data
                Toast.makeText(getApplicationContext(),"Data Recorded",Toast.LENGTH_SHORT).show();
                zipManager.compressGzipFile("Samples/"+fileName,"Zip/"+fileName+".gz");  // Compressing Data
                Toast.makeText(getApplicationContext(),"Data Compressed",Toast.LENGTH_SHORT).show();
                uploadMultipart("/storage/emulated/0/Zip/"+fileName+".gz");                             // uploading data to server
                handler.postDelayed(this, 60000);
            }
        };
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(runnable1, 60000);
            }
        };
        handler.postDelayed(runnable, sec);



        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        /*if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }*/

        //player.stop();
    }

    public void talk(String code , String data) {
        Intent i = new Intent();
        i.putExtra(code,data);
        i.setAction("FILTER");
        sendBroadcast(i);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //talk("valueX",String.valueOf(sensorEvent.values[0]));
            //talk("valueY",String.valueOf(sensorEvent.values[1]));
            //talk("valueZ",String.valueOf(sensorEvent.values[2]));

           // i.putExtra("valueX",String.valueOf(sensorEvent.values[0])+"/"+String.valueOf(sensorEvent.values[1])+"/"+String.valueOf(sensorEvent.values[1]));
            realTimeController.updateXYZ(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]);
            recordSaveData.recordData(realTimeController.getX(),realTimeController.getY(),realTimeController.getZ());
            i.putExtra("valueX",String.valueOf(realTimeController.getX()));
            i.putExtra("valueY",String.valueOf(realTimeController.getY()));
            i.putExtra("valueZ",String.valueOf(realTimeController.getZ()));
            i.setAction("FILTER");


        }else if(sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION){
            i.putExtra("compass",String.valueOf(sensorEvent.values[0]));
        }
        sendBroadcast(i);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void uploadMultipart(String path) {
        //getting name for the image
        UPLOAD_URL =  "http://192.168.254.10/data/api/uploaddata.php";
        Date currentTime = Calendar.getInstance().getTime();
        String name=(currentTime.getYear()+1900)+"-"+(currentTime.getMonth()+1)+"-"+currentTime.getDate()+"-"+currentTime.getHours()+currentTime.getMinutes()+"-"+currentTime.getSeconds()+".csv";

        String location = "Lapulapu";
        String month = ""+(currentTime.getMonth()+1);
        String day = ""+currentTime.getDate();
        String year = ""+(currentTime.getYear()+1900);
        String hour = ""+currentTime.getHours();
        String minute = ""+currentTime.getMinutes();

        //getting the actual path of the image
      //  String path = FilePath.getPath(getActivity(), filePath);

        if (path == null) {

            //Toast.makeText(this, "Please move your .csv file to internal storage and retry", Toast.LENGTH_LONG).show();
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

