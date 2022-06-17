package com.yang.myapplication.Tools;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class ScanRecordTool  {
    private static final String TAG = "ScanRecordTool";
    private BluetoothAdapter bluetoothAdapter;

    public void init(){
//        registerReceiver(receiver, makeFilters());
    }


    private IntentFilter makeFilters(){
        IntentFilter deviceIntentFilter = new IntentFilter();
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        return deviceIntentFilter;
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e(TAG, "开始搜索");

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e(TAG, "查找到设备完成");


            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name != null) {
                    int rssi = Math.abs(intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI));
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String s = "";
                    int BondState = device.getBondState();
                    switch (BondState){
                        case BluetoothDevice.BOND_BONDED:
                            s = "信号："+ rssi +"---设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：已配对" + "\n";
                            break;
                        default:
                            s = "信号："+ rssi +"---设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未知" + "\n";


                            break;
                    }
                    Log.e(TAG,s);
                }
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,BluetoothAdapter.ERROR);
                if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE) {
                    Log.e(TAG,"设备不可搜索但是可以连接");
                } else  if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Log.e(TAG,"设备可见监听");
                }else  if (scanMode == BluetoothAdapter.SCAN_MODE_NONE) {
                    Log.e(TAG,"设备不可搜索不可连接");
                }  else {
                    Log.e(TAG,"ERROR");
                }

            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (remoteDevice == null) {
                    Log.e(TAG,"没有绑定设备");
                    return;
                }
                int status = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);
                if (status == BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG,"绑定设备完成: " + remoteDevice.getName());
                } else if (status == BluetoothDevice.BOND_BONDING) {
                    Log.e(TAG,"绑定设备中: " + remoteDevice.getName());
                } else if (status == BluetoothDevice.BOND_NONE) {
                    Log.e(TAG,"取消绑定: ");
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.e(TAG,"配对时，发起连接");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.e(TAG,"配对结束时,断开连接");
            }
        }
    };



}
