package com.yang.myapplication.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yang.myapplication.Interface.BlueToothInterface;

public class BluetoothStateBroadcastReceive extends BroadcastReceiver {

    private BlueToothInterface blueToothInterface;

    public BluetoothStateBroadcastReceive(BlueToothInterface blueToothInterface) {
        this.blueToothInterface = blueToothInterface;
    }
    final private String TAG = "#scan#";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                Log.i(TAG, "scanable device" + device.getName() +"--" + device.getAddress());
                blueToothInterface.getBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.i(TAG, "Find device completely");
                blueToothInterface.searchFinish();
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.i(TAG, device.getName() + "connected");
                blueToothInterface.getConnectedBlueToothDevices(device);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.i(TAG, device.getName() + "disconnect");
                blueToothInterface.getDisConnectedBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(TAG, "disable BLE");
                        blueToothInterface.disable();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG, "enable BLE");
                        blueToothInterface.open();
                        break;
                }
                break;
        }
    }
}
