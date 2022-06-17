package com.yang.myapplication.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
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

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleScanPresenterImp;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanPresenter;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.scan.BleScanner;
import com.yang.myapplication.R;

import java.util.List;

public class BLEActivity extends AppCompatActivity {
    private boolean isAutoConnect = false;
    private static String TAG = "BLEActivity";

    private ListView list_paired_devices,list_available_devices;
    private ArrayAdapter<String> adapterPairedDevice,adapterAvailableDevice;
    private static Context context;
    private ProgressBar progress_scan_devices;
    private Button scan ,paired;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ble, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.jump:
                Intent intent = new Intent(context,DeviceListActivity.class);
                startActivity(intent);
                break;
            case R.id.menuScan:
                scanNow();
                break;
            case R.id.pariedMenu:
                loadPaired();
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleactivity);
        context = this;
        checkPermissions();
        isLocationEnabled();
        if (checkIfSupportBle()) {
            init();
            initBle();
            scanNow();
            loadPaired();
        }
    }

    public boolean checkIfSupportBle() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private void loadPaired() {
        List<BleDevice> pairedDevice =  BleManager.getInstance().getAllConnectedDevice();
        Log.e(TAG,"已连接"+pairedDevice.size());

        for(BleDevice device :pairedDevice ){
            adapterPairedDevice.add(device.getName()+"\n"+device.getMac());
            adapterPairedDevice.notifyDataSetChanged();
        }
    }

    private void init(){
        list_paired_devices = findViewById(R.id.list_paired_devices);
        list_available_devices = findViewById(R.id.list_available_devices);
        progress_scan_devices = findViewById(R.id.progress_scan_devices);
        scan = findViewById(R.id.scan);
        paired = findViewById(R.id.paired);
        adapterPairedDevice = new ArrayAdapter<String>(context,R.layout.device_list_item);
        adapterAvailableDevice = new ArrayAdapter<String>(context,R.layout.device_list_item);
        list_paired_devices.setAdapter(adapterPairedDevice);
        list_available_devices.setAdapter(adapterAvailableDevice);

        list_available_devices.setOnItemClickListener(mDeviceClickListener);
        list_paired_devices.setOnItemClickListener(mDeviceClickListener);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanNow();
            }
        });
        paired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadPaired();
            }
        });
    }

    private AdapterView.OnItemClickListener mDeviceClickListener  = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            BleManager.getInstance().cancelScan();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            Log.e(TAG,address);


        }
    };


    private void initBle(){

        BleManager.getInstance().init(getApplication());

        BleManager.getInstance()
                .enableLog(false)
                .setReConnectCount(1, 5000)
                .setSplitWriteNum(20)
                .setConnectOverTime(10000)
                .setOperateTimeout(5000);

        BleManager.getInstance().enableBluetooth();

//        配置扫描的参数
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
//                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
//                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
//                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(30000)              // 扫描超时时间，可选，默认10秒
                .build();


        BleManager.getInstance().initScanRule(scanRuleConfig);

    }

    private void scanNow() {

        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            //开始搜索
            public void onScanStarted(boolean success) {
                progress_scan_devices.setVisibility(View.VISIBLE);
                adapterAvailableDevice.clear();
                Toast.makeText(context, "Scan starts", Toast.LENGTH_SHORT).show();
            }
            //每搜到一条的回调
            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }
            //正在搜索
            @Override
            public void onScanning(BleDevice bleDevice) {
                String name = bleDevice.getName();
                String address = bleDevice.getMac();
                String  s= name+"--"+address+"--";
                Log.e(TAG, s);
                if(name==null) return;
                adapterAvailableDevice.add(bleDevice.getName()+"\n"+bleDevice.getMac());
                adapterAvailableDevice.notifyDataSetChanged();
            }

            //搜索结束返回集合中所有的搜索
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {

//                for (BleDevice device : scanResultList ){
//                    String name = device.getName();
//                    String key = device.getKey();
//                    String address = device.getMac();
//                    int rssi = Math.abs(device.getRssi());
//                    String  s= name+"--"+address+"--"+rssi+"--"+key;
//                    Log.e(TAG, s);
//                }
                Log.e(TAG, "查找到设备完成");
                progress_scan_devices.setVisibility(View.GONE);
                if(adapterAvailableDevice.getCount() == 0){
                    Toast.makeText(context, "No device found", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(context, "Click on the device to start to chat", Toast.LENGTH_SHORT).show();
                }


            }
        });
    }


    private void connectDevice(String mac) {
//        String mac = bleDevice.getMac();
        BleManager.getInstance().connect(mac, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                // 开始连接
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                // 连接失败
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                // 连接成功，BleDevice即为所连接的BLE设备
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                // 连接中断，isActiveDisConnected表示是否是主动调用了断开连接方法
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().cancelScan();
    }








    public void  isLocationEnabled() {
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            return locationManager.isLocationEnabled();
//        }
//        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//        if (gps || network) {
//            return true;
//        }
//        return false;

        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                //判断是否需要 向用户解释，为什么要申请该权限
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(context, "Please open GPS", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this ,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
                return;
            }else{

            }
        }

    }


    String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]);
            int l = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]);
            int m = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[2]);
            if (i != PackageManager.PERMISSION_GRANTED ||
                    l != PackageManager.PERMISSION_GRANTED ||
                    m != PackageManager.PERMISSION_GRANTED) {
                startRequestPermission();
            }
        }
    }
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(BLEActivity.this, permissions, 321);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //如果没有获取权限，那么可以提示用户去设置界面--->应用权限开启权限
                } else {
                    //获取权限成功提示，可以不要
                    Toast toast = Toast.makeText(this, "Permission obtained successfully", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        }
    }

}