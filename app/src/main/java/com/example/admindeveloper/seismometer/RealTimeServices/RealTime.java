package com.example.admindeveloper.seismometer.RealTimeServices;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.DisplayGraph;
import com.example.admindeveloper.seismometer.R;
import com.example.admindeveloper.seismometer.RecordSaveData;
import com.github.mikephil.charting.charts.LineChart;

public class RealTime extends Fragment {

    View myView;

    public LineChart lineChartX,lineChartY,lineChartZ;
    private SensorManager mSensorManager;
    private Thread thread;
    DisplayGraph dgx,dgy,dgz;
    RealTimeController rtc;
    RecordSaveData rsdata;
    BroadcastReceiver br;
    private void smooththread() {
        if (thread != null){
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
    private void displayRawDataGraph(float x , float y , float z) {
            rtc.updateXYZ(x,y,z);
            dgx.displayRawDataGraph(rtc.getX(),lineChartX);
            dgy.displayRawDataGraph(rtc.getY(),lineChartY);
            dgz.displayRawDataGraph(rtc.getZ(),lineChartZ);
    }
    public void showMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setCancelable(false);
        builder.setTitle("Real Time");
        builder.setMessage("Please put your device on a Flat Surface Area");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getContext(),"Thank you", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    //-----------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.realtimelayout,container,false);
        lineChartX = (LineChart) myView.findViewById(R.id.linechartX);
        lineChartY = (LineChart) myView.findViewById(R.id.linechartY);
        lineChartZ = (LineChart) myView.findViewById(R.id.linechartZ);
        rtc = new RealTimeController();
        rsdata = new RecordSaveData();
        dgx = new DisplayGraph();
        dgy = new DisplayGraph();
        dgz = new DisplayGraph();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        }
        //showMessage();
        smooththread();
        dgx.setup(lineChartX,"X");
        dgy.setup(lineChartY,"Y");
        dgz.setup(lineChartZ,"Z");
        return myView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1000:{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getActivity(),"Permission Granted", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getActivity(),"Permission not granted", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            }
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(br == null) {
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //String str = (String) intent.getExtras().get("Extra data name").toString();
                    //Toast.makeText(MainActivity.this,str,Toast.LENGTH_SHORT).show();
                    //text.setText("Value of Accelerometer: " + str);
                   //float x = Float.parseFloat(intent.getExtras().get("valueX").toString());
                   //float y = Float.parseFloat(intent.getExtras().get("valueY").toString());
                   //float z = Float.parseFloat(intent.getExtras().get("valueZ").toString());

                    displayRawDataGraph(Float.parseFloat(intent.getExtras().get("valueX").toString())
                            ,Float.parseFloat(intent.getExtras().get("valueY").toString())
                            ,Float.parseFloat(intent.getExtras().get("valueZ").toString()));
                }
            };
        }
        IntentFilter filt = new IntentFilter("FILTER"); // before
        getActivity().registerReceiver(br, filt);// before
        /* AFTER
         registerReceiver(br,new IntentFilter("location_update"));
         */
    }

    @Override
    public void onPause() {
        super.onPause();
        if(br != null) {
            getActivity().unregisterReceiver(br); // put unregister here in on pause so that it will unregister if
        }
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        thread.interrupt();
        super.onDestroyView();
    }
    //-----------------------------------------------------------------------------------------------------


}