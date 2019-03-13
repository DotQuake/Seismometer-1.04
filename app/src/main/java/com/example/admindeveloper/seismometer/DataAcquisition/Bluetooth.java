package com.example.admindeveloper.seismometer.DataAcquisition;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {
    public final static int RESULT_OK = 0;
    public final static int RESULT_BLUETOOTH_NOT_SUPPORTED = 1;
    public final static int RESULT_BLUETOOTH_CANNOT_ENABLE = 2;
    public final static int RESULT_RFCOMM_ESTABLISH = 3;
    public final static int RESULT_RFCOMM_CANNOT_ESTABLISH = 4;
    public final static int RESULT_SOCKET_CANNOT_CONNECT = 5;
    public final static int RESULT_SOCKET_CANNOT_CLOSE = 6;
    public final static int RESULT_CANNOT_CREATE_RFCOMM = 7;
    public final static int RESULT_CANNOT_GET_INPUT_STREAM = 8;
    public final static int RESULT_CANNOT_GET_OUTPUT_STREAM = 9;
    public final static int RESULT_BLUETOOTH_TURNING_ON = 10;
    public final static int RESULT_QUERY_FAILED = 11;

    private static BluetoothDevice bluetoothDevice;
    private static BluetoothSocket bluetoothSocket;
    private static BluetoothAdapter mBluetoothAdapter;
    private static String deviceName = "HC-06";
    private static String deviceAdress = "";
    private static String deviceUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static Boolean scanDeviceNameOnly = true;
    private static InputStream mInputStream;
    private static OutputStream mOutputStream;
    private static boolean startDiscoveryProcess = false;

    private static Thread loopDiscovery = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                mBluetoothAdapter.startDiscovery();
                while (Bluetooth.startDiscoveryProcess) {
                    if (!mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.startDiscovery();
                    }
                }
            } catch (Exception e) {
                Log.d("loopDiscovery Thread", "Thread Stopped!");
            }
        }
    });

    public static String getDeviceName() {
        return deviceName;
    }

    public static String getDeviceAdress() {
        if (deviceAdress != null)
            return deviceAdress;
        else
            return null;
    }

    public static void setDeviceUUID(String deviceUUID) {
        Bluetooth.deviceUUID = deviceUUID;
    }

    public static boolean registerAsBluetoothDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice.getName().equals(Bluetooth.deviceName)) {
            Bluetooth.bluetoothDevice = bluetoothDevice;
            return true;
        } else
            return false;
        /*if(scanDeviceNameOnly){
            if(bluetoothDevice.getName().equals(Bluetooth.deviceName)) {
                Bluetooth.bluetoothDevice=bluetoothDevice;
                return true;
            }
            else
                return false;
        }else{
            try {
                if (bluetoothDevice.getName().equals(Bluetooth.deviceName) && bluetoothDevice.getAddress().equals(Bluetooth.deviceAdress)) {
                    Bluetooth.bluetoothDevice = bluetoothDevice;
                    return true;
                }
                else
                    return false;
            }catch (Exception e){return false;}
        }*/
    }

    public static BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public static void setDeviceName(String deviceName) {
        Bluetooth.deviceName = deviceName;
    }

    public static void setDeviceAdress(String deviceAdress, Boolean scanDeviceNameOnly) {
        Bluetooth.deviceAdress = deviceAdress;
        Bluetooth.scanDeviceNameOnly = scanDeviceNameOnly;
    }

    public static void disableBluetooth() {
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.disable();
    }

    public static int initializeBluetooth() {
        Bluetooth.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Bluetooth.mBluetoothAdapter == null) {
            return RESULT_BLUETOOTH_NOT_SUPPORTED;
        } else {
            if (!Bluetooth.mBluetoothAdapter.isEnabled()) {
                if (Bluetooth.mBluetoothAdapter.enable()) {
                    return RESULT_BLUETOOTH_TURNING_ON;
                } else {
                    return RESULT_BLUETOOTH_CANNOT_ENABLE;
                }
            } else {
                if (Bluetooth.queryDevice())
                    return RESULT_OK;
                else
                    return RESULT_QUERY_FAILED;
            }
        }
    }

    public static boolean queryDevice() {
        Set<BluetoothDevice> pairedDevices = Bluetooth.mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-06")) {
                    if (!scanDeviceNameOnly && device.getAddress().equals(Bluetooth.deviceAdress)) {
                        Bluetooth.bluetoothDevice = device;
                        return true;
                    } else if (scanDeviceNameOnly) {
                        Bluetooth.bluetoothDevice = device;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void startDiscovery() {
        Bluetooth.startDiscoveryProcess = true;
        loopDiscovery.start();
    }

    public static void stopDiscovery() {
        Bluetooth.startDiscoveryProcess = false;
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
    }

    public static int startRFCOMMEstablish() {
        if (bluetoothDevice != null) {
            Bluetooth.mInputStream = null;
            Bluetooth.mOutputStream = null;
            Bluetooth.bluetoothSocket = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                Bluetooth.bluetoothSocket = Bluetooth.bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                Log.d("RFCOMM Function Error", e.getMessage());
                return Bluetooth.RESULT_CANNOT_CREATE_RFCOMM;
            }
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                bluetoothSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.d("RFCOMM Function Error", connectException.getMessage());
                    bluetoothSocket.close();
                    return Bluetooth.RESULT_SOCKET_CANNOT_CONNECT;
                } catch (IOException closeException) {
                    Log.d("RFCOMM Function Error", closeException.getMessage());
                    return Bluetooth.RESULT_SOCKET_CANNOT_CLOSE;
                }
            }
            try {
                mInputStream = bluetoothSocket.getInputStream();
            } catch (IOException e) {
                Log.d("RFCOMM Function Error", e.getMessage());
                return Bluetooth.RESULT_CANNOT_GET_INPUT_STREAM;
            }
            try {
                mOutputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.d("RFCOMM Function Error", e.getMessage());
                return Bluetooth.RESULT_CANNOT_GET_OUTPUT_STREAM;
            }
            return Bluetooth.RESULT_RFCOMM_ESTABLISH;
        }
        return Bluetooth.RESULT_RFCOMM_CANNOT_ESTABLISH;
    }

    public static void closeSocket() {
        if(bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.close();
            }catch (Exception e){}
        }
    }
    public static boolean sendData(char data){
        if(Bluetooth.mOutputStream!=null){
            try {
                Bluetooth.mOutputStream.write(data);
                return true;
            } catch (IOException e) {
                Log.e("BLError",e.getMessage());
            }
        }
        return false;
    }

    public static InputStream getmInputStream() {
        return mInputStream;
    }
}
