package com.example.admindeveloper.seismometer.RealTimeServices;

import com.example.admindeveloper.seismometer.DataAcquisition.DataService;

public class RealTimeController {

    private static Integer calibrateX=0,calibrateY=0,calibrateZ=0;
    private float[] gravity = {0,0,0};
    private float[] linear_acceleration = {0,0,0};
    private float x,y,z;

    public void updateXYZ(float x , float y ,float z){
        if(!DataService.isDataFromDevice()) {
            final float alpha = 0.8f;
            gravity[0] = alpha * gravity[0] + (1 - alpha) * x;
            linear_acceleration[0] = x - gravity[0];
            this.x = linear_acceleration[0];

            gravity[1] = alpha * gravity[1] + (1 - alpha) * y;
            linear_acceleration[1] = y - gravity[1];
            this.y = linear_acceleration[1];

            gravity[2] = alpha * gravity[2] + (1 - alpha) * z;
            linear_acceleration[2] = z - gravity[2];
            this.z = linear_acceleration[2];
        }else{
            this.x=x-RealTimeController.calibrateX;
            this.y=y-RealTimeController.calibrateY;
            this.z=z-RealTimeController.calibrateZ;
        }
    }

    public static void setCalibrationValue(Integer x,Integer y,Integer z){
        RealTimeController.calibrateX=x;
        RealTimeController.calibrateY=y;
        RealTimeController.calibrateZ=z;
    }

    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public float getZ() {
        return z;
    }
}
