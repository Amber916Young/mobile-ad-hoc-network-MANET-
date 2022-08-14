package com.yang.myapplication.Interface;

import android.bluetooth.BluetoothDevice;

public interface BlueToothInterface {

    void getBlueToothDevices(BluetoothDevice device);

    void getConnectedBlueToothDevices(BluetoothDevice device);

    void getDisConnectedBlueToothDevices(BluetoothDevice device);

    void searchFinish();

    void open();

    void disable();
}
