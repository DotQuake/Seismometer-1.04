package com.example.admindeveloper.seismometer.DataAcquisition;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.example.admindeveloper.seismometer.RealTimeServices.RealTimeController;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DataStreamTask extends AsyncTask<Void,Void,Void> {

    private Context applicationContext;
    private Intent i=new Intent();
    private boolean calibrationHasFinish=false;
    private static int calibrateX=0,calibrateY=0,calibrateZ=0;
    private int counter=0;
    private final int maxCalibrationSamples=100;

    public DataStreamTask(Context applicationContext) {
        this.applicationContext=applicationContext;
        calibrateX=calibrateY=calibrateZ=0;
    }

    private Short byteToShort(byte[] value)
    {
        ByteBuffer wrapper=ByteBuffer.wrap(value);
        return wrapper.getShort();
    }
    @Override
    protected Void doInBackground(Void... voids) {
        float x,y,z;
        byte[] mmBuffer=new byte[6];
        while(!isCancelled()) {
            try {
                while (Bluetooth.getmInputStream().available() >= 6 && !isCancelled()) {
                    Bluetooth.getmInputStream().read(mmBuffer);
                    byte[] valueX = {mmBuffer[1], mmBuffer[0]};
                    byte[] valueY = {mmBuffer[3], mmBuffer[2]};
                    byte[] valueZ = {mmBuffer[5], mmBuffer[4]};

                    if(calibrationHasFinish) {
                        x = byteToShort(valueX)-calibrateX;
                        y = byteToShort(valueY)-calibrateY;
                        z = byteToShort(valueZ)-calibrateZ;
                        i.putExtra(DataService.GET_X, x);
                        i.putExtra(DataService.GET_Y, y);
                        i.putExtra(DataService.GET_Z, z);
                        i.putExtra(DataService.GET_COMPASS, DataService.getDegree());
                        i.setAction(DataService.DATA);
                        applicationContext.sendBroadcast(i);
                    }else{
                        if(counter<maxCalibrationSamples) {
                            calibrateX += byteToShort(valueX);
                            calibrateY += byteToShort(valueY);
                            calibrateZ += byteToShort(valueZ);
                            counter++;
                        }else{
                            calibrateX/=maxCalibrationSamples;
                            calibrateY/=maxCalibrationSamples;
                            calibrateZ/=maxCalibrationSamples;
                            calibrationHasFinish=true;
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
        return null;
    }
}
