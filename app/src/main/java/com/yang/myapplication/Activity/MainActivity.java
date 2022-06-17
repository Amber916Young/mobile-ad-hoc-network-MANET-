package com.yang.myapplication.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yang.myapplication.Adapter.DeviceAdapter;
import com.yang.myapplication.Adapter.PairedDeviceAdapter;
import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity  extends AppCompatActivity implements RecycleViewInterface {
    private static final String TAG = "MainActivity";
    private Button discover,Scan,listen,send;
    RecyclerView listView ,listViewPaired;
    EditText writeMsg;
    TextView status,msg_box;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    DeviceAdapter adapter ;
    List<String> unPaireddeviceList = new ArrayList<>();
    List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();
    List<BluetoothDevice> btArray = new ArrayList<>();

    public static final int STATE_LISTENING = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTION_FAILED= 4;
    public static final int STATE_MESSAGE_RECEIVED= 5;



    public static final int BOND_NONE = 10;
    public static final int BOND_BONDING = 11;
    public static final int BOND_BONDED = 12;

    int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final String APP_NAME = "BTChat";
    public static final UUID MY_UUID = UUID.randomUUID();

    SendReceive sendReceive;

    String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };

    public ArrayList<String> requestList = new ArrayList<>();
    private static final int REQ_PERMISSION_CODE = 1;

    public void getPermision(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestList.add(Manifest.permission.BLUETOOTH_SCAN);
            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            requestList.add(Manifest.permission.BLUETOOTH);
        }
        if(requestList.size() != 0){
            ActivityCompat.requestPermissions(this, requestList.toArray(new String[0]), REQ_PERMISSION_CODE);
        }
    }

    //点击按钮，访问如下方法
    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]);
            int l = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]);
            int m = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[2]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED ||
                    l != PackageManager.PERMISSION_GRANTED ||
                    m != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                startRequestPermission();
            }
        }
    }
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, 321);
    }


    /**
     * 用户权限 申请 的回调方法
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //如果没有获取权限，那么可以提示用户去设置界面--->应用权限开启权限
                } else {
                    //获取权限成功提示，可以不要
                    Toast toast = Toast.makeText(this, "获取权限成功", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action, menu);
        return true;
    }
    private void pairedDevices() {
        btArray = new ArrayList<>();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        System.err.println("bt===>" + bondedDevices.size());
        if (bondedDevices.size() > 0) {
            btArray.addAll(bondedDevices);
            PairedDeviceAdapter pairedadapter = new PairedDeviceAdapter(getApplicationContext(), R.layout.main_device_layout, btArray, this);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
            listViewPaired.setLayoutManager(linearLayoutManager);
            listViewPaired.setItemAnimator(new DefaultItemAnimator());
            listViewPaired.setAdapter(pairedadapter);
        }
    }
    private void scanDevices(){
        unPaireddeviceList = new ArrayList<>();
        bluetoothDeviceList = new ArrayList<>();
        registerReceiver(receiver, makeFilters());
        bluetoothAdapter.startDiscovery();
    }
    //TODO
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long id = item.getItemId();
        if (id ==R.id.menuLogin) {

        } if (id ==R.id.menuLogout ) {

        } if (id ==R.id.menuScan ) {
            scanDevices();
        } if (id ==R.id.menuDiscover ) {
            pairedDevices();
        } if (id ==R.id.menuMembership ) {

        } if (id ==R.id.menuAllMANET ) {

        } if (id ==R.id.menuGlobalIP ) {

        }

        return true;

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        listView = findViewById(R.id.listView);
        listViewPaired = findViewById(R.id.listViewPaired);
        listen = findViewById(R.id.listen);
        msg_box = findViewById(R.id.msg_box);
        send = findViewById(R.id.send);
        writeMsg = findViewById(R.id.writeMsg);
        checkPermissions();


        implementListeners();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth module not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        registerReceiver(receiver, makeFilters());

        loadScanDeices();
        pairedDevices();
    }

    private void loadScanDeices() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        listView.setLayoutManager(linearLayoutManager);
        listView.setItemAnimator(new DefaultItemAnimator());
        adapter = new DeviceAdapter(getApplicationContext(), R.layout.paired_device_layout,bluetoothDeviceList,this);
        listView.setAdapter(adapter);
    }

    //蓝牙监听需要添加的Action
    private IntentFilter makeFilters(){
        // 注册广播
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



    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;
        public ServerClass(){
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket socket = null;
            while (socket==null){
                Message message = Message.obtain();
                try {
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if (socket != null){
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);
                    //TODO write some code for send / received
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }

            }
        }
    }

    private class ClientClass extends Thread{
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device){
            this.device = device;
            try {
                this.socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run(){
            Message message = Message.obtain();

            try {
                socket.connect();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        public SendReceive(BluetoothSocket socket){
            this.bluetoothSocket =socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
            this.inputStream = tmpIn;
            this.outputStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while (true){
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection fail");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    status.setText("message received");
                    //TODO
                    byte[] readBuff = (byte[]) message.obj;
                    String tepMsg = new String(readBuff,0,message.arg1);
                    msg_box.setText(tepMsg);
                    break;
            }
            return true;
        }
    });

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e(TAG, "开始搜索");

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothAdapter.cancelDiscovery();
//                unregisterReceiver(receiver);
                Log.e(TAG, "查找到设备完成");

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name != null) {
                    int rssi = Math.abs(intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI));
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String s = "";
                    int BondState = device.getBondState();
                    switch (BondState){
                        case BOND_BONDED:
                            s = "信号："+ rssi +"---设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：已配对" + "\n";
                            break;
                        case BOND_BONDING:
                            s = "信号："+ rssi +"---设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：正在配对" + "\n";
                            break;
                        case BOND_NONE:
                            s = "信号："+ rssi +"---设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未配对" + "\n";
                            break;
                        default:
                            s = "信号："+ rssi +"---设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未知" + "\n";
                            break;
                    }
                    Log.e(TAG,s);

                    if(!unPaireddeviceList.contains(device.getName()) && BondState!= BOND_BONDED){
                        bluetoothDeviceList.add(device);
                        unPaireddeviceList.add(device.getName());
                        adapter.notifyDataSetChanged();
                    }
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    private void implementListeners() {

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string = writeMsg.getText().toString();
                sendReceive.write(string.getBytes());
            }
        });
    }


    @Override
    public void onItemClick(int position) {
        ClientClass clientClass = new ClientClass(btArray.get(position));
        clientClass.start();
        status.setText("Connecting");
    }

    @Override
    public void onItemClickPair(int position) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothDeviceList.get(position);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            device.createBond();
        }
    }
}