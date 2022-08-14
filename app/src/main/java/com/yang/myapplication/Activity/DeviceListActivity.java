package com.yang.myapplication.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yang.myapplication.R;
import com.yang.myapplication.Tools.BluetoothTools;
import com.yang.myapplication.Tools.NetworkTool;
import com.yang.myapplication.Tools.RouterTool;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.http.UrlDomain;
import com.yang.myapplication.service.MessageDB;

import net.vidageek.mirror.dsl.Mirror;

import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private ListView list_paired_devices,list_available_devices;
    private ArrayAdapter<String> adapterPairedDevice,adapterAvailableDevice;
    private Context context;
    private static BluetoothAdapter bluetoothAdapter;
    private ProgressBar progress_scan_devices;
    private static final String TAG = "DeviceListActivity";
    DeviceInfo deviceInfo = MessageDB.getDeviceInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context = this;
        init();
    }

    private void init(){
        list_paired_devices = findViewById(R.id.list_paired_devices);
        list_available_devices = findViewById(R.id.list_available_devices);
        progress_scan_devices = findViewById(R.id.progress_scan_devices);
        adapterPairedDevice = new ArrayAdapter<String>(context,R.layout.device_list_item);
        adapterAvailableDevice = new ArrayAdapter<String>(context,R.layout.device_list_item);
        list_paired_devices.setAdapter(adapterPairedDevice);
        list_available_devices.setAdapter(adapterAvailableDevice);
        list_available_devices.setOnItemClickListener(mDeviceClickListener);
        list_paired_devices.setOnItemClickListener(mDeviceClickListener);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        registerReceiver(receiver, makeFilters());
        deviceInfo = MessageDB.getDeviceInfo();
    }

    public static String EXTRA_DEVICE_ADDRESS = "deviceAddress";

    private AdapterView.OnItemClickListener mDeviceClickListener  = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            bluetoothAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = allDevices.get(address);

            if (deviceInfo == null) {
                String notification = "sorry, you haven't login";
                Toast.makeText(context, notification, Toast.LENGTH_SHORT).show();
                return;
            }
            if (device != null) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    };

    HashMap<String,BluetoothDevice> allDevices = new HashMap<>();

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
                progress_scan_devices.setVisibility(View.GONE);
                if(adapterAvailableDevice.getCount() == 0){
//                    Toast.makeText(context, "No device found", Toast.LENGTH_SHORT).show();
                }else {
//                    Toast.makeText(context, "Click on the device to start to chat", Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name != null) {
                    int rssi = Math.abs(intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI));
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(!allDevices.containsKey(device.getAddress())){
                        allDevices.put(device.getAddress(),device);
                        adapterAvailableDevice.add(device.getName()+"\n"+device.getAddress());

                    }
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
            }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                int status = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (status) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "蓝牙已关闭");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "蓝牙已打开");
                        bluetoothAdapter.startDiscovery();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "蓝牙关闭中...");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "蓝牙打开中...");
                        break;
                    default:
                        unregisterReceiver(this);
                        break;
                }
            }
        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.menuScan:
                scanDevices();
                break;
//            case R.id.menuLeave:
//                leaveCurrentMANET();
//                break;
            case R.id.menuBack:
                intent = new Intent(DeviceListActivity.this,BluetoothChat.class);
                startActivity(intent);
                break;

//            case R.id.menuCreate:
//                createAMANET();
//                break;


        }
        return true;
    }


    private void scanDevices(){
        progress_scan_devices.setVisibility(View.VISIBLE);
        adapterAvailableDevice.clear();
        adapterPairedDevice.clear();
        Toast.makeText(context, "Scan starts", Toast.LENGTH_SHORT).show();
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }



    private IntentFilter makeFilters(){
        IntentFilter deviceIntentFilter = new IntentFilter();
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        return deviceIntentFilter;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(receiver);
    }
}