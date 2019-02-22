package com.example.admindeveloper.seismometer.RealTimeServices;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.DataAcquisition.Bluetooth;
import com.example.admindeveloper.seismometer.DataAcquisition.DataService;
import com.example.admindeveloper.seismometer.DisplayGraph;
import com.example.admindeveloper.seismometer.R;
import com.example.admindeveloper.seismometer.RecordSaveData;
import com.jjoe64.graphview.GraphView;

import java.util.Calendar;
import java.util.Date;

public class RealTime extends Fragment{

    View myView;
    RealTimeController rtc;
    RecordSaveData rsdata;
    BroadcastReceiver br,wifi_state;
    ImageView iv_wifi;
    DisplayGraph dataGraphController;
    GraphView dataGraph;
    TextView hourBox,minuteBox,statusBox;

    private void displayData(float x , float y , float z) {
        rtc.updateXYZ(x,y,z);
        dataGraphController.displayRawDataGraph(rtc.getX(),rtc.getY(),rtc.getZ());
        Date currentDate=Calendar.getInstance().getTime();
        hourBox.setText(currentDate.getHours()+" ");
        minuteBox.setText(currentDate.getMinutes()+" ");
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
        rtc = new RealTimeController();
        rsdata = new RecordSaveData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        }
        //showMessage();
        iv_wifi = myView.findViewById(R.id.iv_wifi_status);
        dataGraph=myView.findViewById(R.id.dataGraph);
        dataGraphController=new DisplayGraph(dataGraph,500);
        hourBox=myView.findViewById(R.id.hourBox);
        minuteBox=myView.findViewById(R.id.minuteBox);
        statusBox=myView.findViewById(R.id.statusBox);
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
        wifi_state = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    boolean connected = info.isConnected();
                    if (connected) {
                       // Toast.makeText(getActivity(),"wifi connected",Toast.LENGTH_SHORT).show();
                        iv_wifi.setImageResource(R.drawable.wifi_connected);
                    }else{
                        //Toast.makeText(getActivity(),"Not connected",Toast.LENGTH_SHORT).show();
                        iv_wifi.setImageResource(R.drawable.not_connected);
                    }
                }
            }
        };
        getActivity().registerReceiver(wifi_state, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        statusBox.setText("Service Stop");
        if(br == null) {
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(DataService.DATA)){
                        dataGraphController.updateDisplayGraph();
                        if(!DataService.isDataFromDevice())
                            statusBox.setText("Internal Mode");
                        else
                            statusBox.setText("Connected");
                        displayData(intent.getFloatExtra(DataService.GET_X,0),
                                intent.getFloatExtra(DataService.GET_Y,0),
                                intent.getFloatExtra(DataService.GET_Z,0));
                    }else if(intent.getAction().equals(DataService.DATASERVICE_STOP)){
                        statusBox.setText("Service Stop");
                    }else if(intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
                        if(!statusBox.getText().equals("Service Stop"))
                            statusBox.setText("Disconnected");
                    }
                }
            };
        }
        IntentFilter filt = new IntentFilter(DataService.DATA); // before
        filt.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filt.addAction(DataService.DATASERVICE_STOP);
        getActivity().registerReceiver(br, filt);// before
        /* AFTER
         registerReceiver(br,new IntentFilter("location_update"));
         */
    }

    @Override
    public void onPause() {
        super.onPause();
        if(br != null || wifi_state != null) {
            getActivity().unregisterReceiver(br); // put unregister here in on pause so that it will unregister if
            getActivity().unregisterReceiver(wifi_state);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    //-----------------------------------------------------------------------------------------------------


}