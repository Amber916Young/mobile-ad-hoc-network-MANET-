package com.yang.myapplication.Tools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothTools {
    private Context context;
    public static BluetoothAdapter bluetoothAdapter;

    public BluetoothTools(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //获取本地蓝牙实例
    }

    static public boolean pair(BluetoothDevice device){
        return createBond(device);
    }

    static public boolean unpair(BluetoothDevice device) {
        int state = device.getBondState();
        if (state == BluetoothDevice.BOND_BONDING) {
            cancelBondProcess(device);
        }
        if (state != BluetoothDevice.BOND_NONE) {
            final boolean successful = removeBond(device);
            return successful;
        }
        return false;
    }

    static public void setPin(BluetoothDevice device, String pin){
        try {
            device.setPin(pin.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }



    static private boolean createBond(BluetoothDevice device){
        return device.createBond();
    }

    static public  boolean removeBond(BluetoothDevice device){
        boolean result = false;
        try {
            Method method = null;
            method = device.getClass().getDeclaredMethod("removeBond");
            method.setAccessible(true);
            result = (boolean) method.invoke(device);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return result;
    }

    static private void cancelBondProcess(BluetoothDevice device){
        try {
            Method method = null;
            method = device.getClass().getDeclaredMethod("cancelBondProcess");
            method.setAccessible(true);
            method.invoke(device);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }



    /**
     * 判断蓝牙是否开启
     */
    public boolean isBluetoothEnable() {
        return bluetoothAdapter.isEnabled();
    }
    public void bluetoothEnable() {
        bluetoothAdapter.enable();
        openBluetooth();
    }


    /**
     * enable Bluetooth 3000s
     */
    public void openBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
        context.startActivity(intent);
    }

    /**
     * 关闭蓝牙
     */
    public void disableBluetooth() {
        bluetoothAdapter.disable();
    }

    /**
     * 查询已配对设备
     */
    public List<BluetoothDevice> getDevicesList() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        return new ArrayList<>(pairedDevices);
    }

    /**
     * 扫描附件设备需要定位权限
     */
    public void startDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    public static BluetoothDevice getBluetoothDevice(String mac) {
        return bluetoothAdapter.getRemoteDevice(mac);
    }


    public void close(){
        bluetoothAdapter.cancelDiscovery();
    }


    public String UserNameBluetooth(){
        return bluetoothAdapter.getName();
    }

}