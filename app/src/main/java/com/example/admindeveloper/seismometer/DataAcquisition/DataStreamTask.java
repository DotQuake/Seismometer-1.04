package com.example.admindeveloper.seismometer.DataAcquisition;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DataStreamTask extends AsyncTask<Void,Void,Void> {

    private static int sampleSkippedCounter=0,calibrationCounter=0;
    private static boolean calibrationHasFinish=false;
    private static int calibrateX=0,calibrateY=0,calibrateZ=0;

    private Context applicationContext;
    private Intent i=new Intent();
    private final int maxCalibrationSamples=100;
    private final int maxSampleSkipped=860;

    public static void calibrate(){
        sampleSkippedCounter=calibrationCounter=0;
        calibrateX=calibrateY=calibrateZ=0;
        calibrationHasFinish=false;
    }

    public DataStreamTask(Context applicationContext) {
        this.applicationContext=applicationContext;
        calibrateX=calibrateY=calibrateZ=0;
    }

    private short byteToShort(byte[] value)
    {
        ByteBuffer wrapper=ByteBuffer.wrap(value);
        return wrapper.getShort();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        float x,y,z;
        byte[] mmBuffer=new byte[8];
        while(!isCancelled()) {
            try {
                while (Bluetooth.getmInputStream().available() >= 8 && !isCancelled()) {
                    Bluetooth.getmInputStream().read(mmBuffer);
                    if(mmBuffer[0]==0x00&&mmBuffer[1]==0x00) {
                        byte[] valueX = {mmBuffer[3], mmBuffer[2]};
                        byte[] valueY = {mmBuffer[5], mmBuffer[4]};
                        byte[] valueZ = {mmBuffer[7], mmBuffer[6]};

                        if (calibrationHasFinish) {
                            x = byteToShort(valueX);
                            y = byteToShort(valueY);
                            z = byteToShort(valueZ);
                            if(x>=0)
                                x--;
                            if(y>=0)
                                y--;
                            if(z>=0)
                                z--;
                            x=x-calibrateX;
                            y=y-calibrateY;
                            z=z-calibrateZ;
                            i.putExtra(DataService.GET_X, x);
                            i.putExtra(DataService.GET_Y, y);
                            i.putExtra(DataService.GET_Z, z);
                            i.putExtra(DataService.GET_COMPASS, DataService.getDegree());
                            i.setAction(DataService.DATA);
                            applicationContext.sendBroadcast(i);
                        } else {
                            if(sampleSkippedCounter<maxSampleSkipped){
                                sampleSkippedCounter++;
                            }else {
                                if (calibrationCounter < maxCalibrationSamples) {
                                    calibrateX+=byteToShort(valueX);
                                    calibrateY+=byteToShort(valueY);
                                    calibrateZ+=byteToShort(valueZ);
                                    calibrationCounter++;
                                } else {
                                    calibrateX/=maxCalibrationSamples;
                                    calibrateY/=maxCalibrationSamples;
                                    calibrateZ/=maxCalibrationSamples;
                                    calibrationHasFinish = true;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
        return null;
    }
}
