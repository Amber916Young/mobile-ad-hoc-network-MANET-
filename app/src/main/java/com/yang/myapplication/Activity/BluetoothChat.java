package com.yang.myapplication.Activity;


import static android.content.ContentValues.TAG;
import static com.yang.myapplication.entity.MessageInfo.DATA_AUDIO;
import static com.yang.myapplication.entity.MessageInfo.DATA_IMAGE;
import static com.yang.myapplication.entity.MessageInfo.DATA_TEXT;
import static com.yang.myapplication.entity.NeighborInfo.offLine;
import static com.yang.myapplication.entity.NeighborInfo.onLine;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.yang.myapplication.Adapter.NeighborAdapter;
import com.yang.myapplication.Interface.BlueToothInterface;
import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;
import com.yang.myapplication.Tools.BluetoothTools;
import com.yang.myapplication.Tools.HandlerTool;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.LoginTool;
import com.yang.myapplication.Tools.NetworkTool;
import com.yang.myapplication.Tools.RouterTool;
import com.yang.myapplication.Tools.XPermissionUtil;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;
import com.yang.myapplication.service.BluetoothStateBroadcastReceive;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import rxhttp.RxHttp;


public class BluetoothChat extends AppCompatActivity implements RecycleViewInterface {
    private Context context;
    private BluetoothTools bluetoothUtil;
    private BluetoothStateBroadcastReceive broadcastReceive;
    public static GroupChat groupChatManager = null;
    HashMap<String, BluetoothDevice> allDevices = new HashMap<>();
    private String label = "";
    private void setState(CharSequence subTitle) {
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }

    private static RecyclerView neighborView;
    private static List<NeighborInfo> allDevice = new ArrayList<>();

    public void updateView() {
        Set<String> set = new HashSet<>();
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        LitePal.deleteAll(NeighborInfo.class);

        HashMap<String,Integer> map = new HashMap<>();
        List<ChatUtils> chatSockets = GroupChat.chatSockets;
        for(ChatUtils utils : chatSockets){
            map.put(utils.getName(),utils.getState());
        }
        for (NeighborInfo duplicate : list) {
            if (!set.contains(duplicate.getNeighborName())) {
                set.add(duplicate.getNeighborName());
                NeighborInfo neighbor = new NeighborInfo();
                neighbor.setHop(duplicate.getHop());
                neighbor.setPath(duplicate.getPath());
                neighbor.setNeighborName(duplicate.getNeighborName());
                neighbor.setNeighborMac(duplicate.getNeighborMac());
                String name1 = duplicate.getNeighborName();
                String name2 = localName;
                if(duplicate.getHop() != 1){
                    String[] routers = RouterTool.routerList(duplicate.getPath());
                    String first = routers[1].trim();
                    if(map.containsKey(first)){
                        int status = map.get(first);
                        if(status == 1 || status == 2){
                            neighbor.setConnection_status(offLine);
                        }else {
                            neighbor.setConnection_status(onLine);
                        }
                    }else {
                        // need to connect again
                    }

                }else {
                    if(map.containsKey(duplicate.getNeighborName())){
                        int status = map.get(duplicate.getNeighborName());
                        if(status == 1 || status == 2){
                            neighbor.setConnection_status(offLine);
                        }else {
                            neighbor.setConnection_status(onLine);
                        }
                    }
                }




                // query latest message from message DB
                MessageInfo info = LitePal.where("sourceName = ? and targetName = ? or sourceName = ? and targetName = ? ",name1,name2,name2,name1).order("sendDate").findLast(MessageInfo.class);
                if(info != null){
                    int dataType = info.getDataType();
                    String lastMessage = "";
                    switch (dataType){
                        case DATA_TEXT:
                            lastMessage = info.getMessage().length() > 20 ? info.getMessage()+"..." : info.getMessage();
                            break;
                        case DATA_IMAGE:
                            lastMessage = "[image message]";
                            break;
                        case DATA_AUDIO:
                            lastMessage = "[audio message]";
                            break;
                    }
                    String lastTime = info.getSendDate().substring(11);
                    neighbor.setLastMessage(lastMessage);
                    neighbor.setLastTime(lastTime);
                }
                neighbor.save();
            }
        }
        list = LitePal.findAll(NeighborInfo.class);
        if (list.size() > 0) {
            allDevice.addAll(list);
            NeighborAdapter adapter = new NeighborAdapter(context, R.layout.neighbor_table, list, this);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(BluetoothChat.this);
            neighborView.setLayoutManager(linearLayoutManager);
            neighborView.setItemAnimator(new DefaultItemAnimator());
            neighborView.setAdapter(adapter);
        }
    }


    private String localName = null;

    NeighborInfo currentConnectDevice = null;
    static String nextrouters = null;
    static String SER_KEY = "CONNECTINFO";

    @Override
    public void onItemClickPair(int position) {
        label = "";
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        if (list.size() == 0) {
            Toast.makeText(context, "Please finish the scanning", Toast.LENGTH_SHORT).show();
            return;
        }
        currentConnectDevice = list.get(position);
        System.out.println("跳转"+currentConnectDevice.getNeighborName());
        Intent mIntent=new Intent(BluetoothChat.this,ChatMainActivity.class);
        Bundle mBundle=new Bundle();
        mBundle.putSerializable(SER_KEY,currentConnectDevice);
        mIntent.putExtras(mBundle);
        startActivity(mIntent);
        nextrouters = null;
        if (currentConnectDevice.getHop() == 1) {
            nextrouters = currentConnectDevice.getNeighborName();
        }else {
            String[] routers = RouterTool.routerList(currentConnectDevice.getPath());
            for(int i=0; i<routers.length;i++){
                String path = routers[i].trim();
                if(path.equals(localName)){
                    nextrouters = routers[i+1].trim();
                    break;
                }
            }
        }

    }

    @Override
    public void onItemClick(int position) {
    }

    private static DeviceInfo deviceInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);
        initDB();
        context = this;
        checkPermissions();
        if(bluetoothUtil == null){
            bluetoothUtil = new BluetoothTools(context);
        }
        bluetoothUtil.bluetoothEnable();

        init();
        cloudNotification();
        localMacAddress = NetworkTool.getMacAddr();
        localName = bluetoothUtil.UserNameBluetooth();
        LoginTool.funclogin(localName);
        if (bluetoothUtil.isBluetoothEnable()) {
            bluetoothUtil.startDiscovery();
        }

        registerBluetoothReceiver();
    }


    private void cloudNotification() {
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            public void run() {
//                isupdateMessage = true;
//            }
//        }, 1000,   60*1000);
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            public void run() {
                scanDevices();
            }
        }, 120000, 600 * 1000);

        Timer timerViewupdate = new Timer();
        timerViewupdate.schedule(new TimerTask() {
            public void run() {
                if (isupdateNeiView) {
                    Message msg = new Message();
                    msg.arg1 = 2;
                    handlerView.sendMessage(msg);
                    isupdateNeiView = false;
                }
                if (isupdateMessage) {
                    Message msg = new Message();
                    msg.arg1 = 3;
                    handlerView.sendMessage(msg);
                    isupdateMessage = false;
                }

            }
        }, 0, 300);
    }

    //处理界面
    private Handler handlerView = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.arg1 == 1) {
                allDevices.clear();
                pairedDevicesSet.clear();
                progress_scan_devices.setVisibility(View.VISIBLE);
                Log.e(TAG, "start scan");
            } else if (msg.arg1 == 2) {
                progress_scan_devices.setVisibility(View.GONE);
                updateView();
            } else if (msg.arg1 == 3) {
                MessageDB.checkUnSendOrUnreadMessage(localName, localMacAddress);
            } else if (msg.arg1 == 4) {
                List<DeviceInfo> list = LitePal.findAll(DeviceInfo.class);
                if (list.size() == 1) {
                    deviceInfo = list.get(0);
                }
            }
        }
    };

    public static boolean isupdateNeiView = false;
    public static boolean isupdateMessage = false;


    private void initDB() {
        LitePal.initialize(this);
        SQLiteDatabase db = LitePal.getDatabase();
        LitePal.registerDatabaseListener(new DatabaseListener() {
            @Override
            public void onCreate() {
            }
            @Override
            public void onUpgrade(int oldVersion, int newVersion) {
            }
        });
    }

    private String localMacAddress = "";
    private ProgressBar progress_scan_devices;


    private void init() {
        progress_scan_devices = findViewById(R.id.progress_scan_devices);
        neighborView = findViewById(R.id.neighborView);
        LitePal.deleteAll(MessageInfo.class);
    }





    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
        if(bluetoothUtil == null){
            bluetoothUtil = new BluetoothTools(context);
        }

        if(localName==null){
            localName = bluetoothUtil.UserNameBluetooth();
        }
        if(localMacAddress==null){
            localMacAddress = NetworkTool.getMacAddr();
        }
        groupChatManager = HandlerTool.getChatManager();
        if (groupChatManager == null) {
            List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
            if (list.size() == 0) {
                pairedDevicesSet = bluetoothUtil.getDevicesList();
                for (BluetoothDevice device : pairedDevicesSet) {
                    String address = device.getAddress();
                    List<String> path = new ArrayList<>();
                    path.add(localName);
                    path.add(device.getName());
                    NeighborInfo neighbor = new NeighborInfo(address, device.getName(), 1, new Date(), path.toString());
                    LitePal.deleteAll(NeighborInfo.class, "neighborMac = ? and neighborName = ?", address, device.getName());
                    neighbor.save();
                }
                list = LitePal.findAll(NeighborInfo.class);
            }
//            HandlerTool.init(list);

            HandlerTool.init(new ArrayList<>());
            HandlerTool.setLocalName(localName);
            groupChatManager = HandlerTool.getChatManager();
            HandlerTool.setChatManager(groupChatManager);
            HandlerTool.setLocalMacAddress(localMacAddress);
            HandlerTool.setWhichPage("MAIN");
            HandlerTool.setContext(context);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }


    private final static String TAG = "日志";
    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bundle extras = data.getExtras();
                        if (extras == null) {
                            return;
                        }
                        String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        BluetoothDevice device = bluetoothUtil.getBluetoothDevice(address);
                        BluetoothTools.pair(device);
//                        chatUtils.connect(device, label);
                        return;

                    }
                }
            });



    private void scanDevices() {
        Message msg = new Message();
        msg.arg1 = 1;
        handlerView.sendMessage(msg);
        registerBluetoothReceiver();
        bluetoothUtil.startDiscovery();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menuScan:
                scanDevices();
                break;
            case R.id.menu_search:
                intent = new Intent(context, DeviceListActivity.class);
                startActivityIntent.launch(intent);
                break;
            case R.id.discoverable:
                bluetoothUtil.bluetoothEnable();
                Toast.makeText(context, "Clicked enable", Toast.LENGTH_SHORT).show();
                viewDeviceInfo();
                break;
            case R.id.historyMsg:
                intent = new Intent(context, MessageListActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void viewDeviceInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothChat.this);
        DeviceInfo info = LitePal.findFirst(DeviceInfo.class);
        if(info==null) return;
        String title = "device " + info.getUsername() + "'s basic information";
        builder.setTitle(title);
        String body = "uuid: " + info.getUuid() + "\n"
                + "MANET UUID: " + info.getManet_UUID() + "\n"
                + "MAC: " + info.getMac() + "\n"
                + "Login date: " + info.getLoginDate() + "\n"
                + "Role: " + info.getRole() + "\n";
        builder.setMessage(body);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
        isupdateMessage = false;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothUtil.close();
        if (groupChatManager != null) {
            groupChatManager.stop();
        }
        unregisterBluetoothReceiver();
        bluetoothUtil.close();
        isupdateMessage = false;
    }


    private void checkPermissions() {
        XPermissionUtil.requestPermissions(this, 1,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                new XPermissionUtil.OnPermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        Toast toast = Toast.makeText(context, "Permission obtained successfully", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                    @Override
                    public void onPermissionDenied() {
                        Toast toast = Toast.makeText(context, "Please go to the setting interface manually to give relevant permissions", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        XPermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private BlueToothInterface blueToothInterface = new BlueToothInterface() {
        @Override
        public void getBlueToothDevices(BluetoothDevice device) {
            String name = device.getName();
            if (name != null) {
                int BondState = device.getBondState();
                scanRangeAlldevices.add(device.getName());
                if(BondState == BluetoothDevice.BOND_BONDED){
                    if (!allDevices.containsKey(device.getAddress())) {
                        allDevices.put(device.getAddress(), device);
                        pairedDevicesSet.add(device);
                    }
                }
                printLog(device);
            }
        }

        @Override
        public void getConnectedBlueToothDevices(BluetoothDevice device) {
//            Snackbar.make(getView(), "连接" + device.getName() + "成功", Snackbar.LENGTH_LONG).show();
        }

        @Override
        public void getDisConnectedBlueToothDevices(BluetoothDevice device) {
            Log.i(TAG, "Disconnect");
        }

        @Override
        public void searchFinish() {
            Log.e(TAG, "Find device completely");
            progress_scan_devices.setVisibility(View.GONE);
            bluetoothUtil.close();
            unregisterBluetoothReceiver();
            getPairedDevices();
        }

        @Override
        public void open() {
        }

        @Override
        public void disable() {
        }
    };

    private void registerBluetoothReceiver() {
        Log.i(TAG, "Bluetooth broadcast monitoring start");
        if (broadcastReceive == null) {
            broadcastReceive = new BluetoothStateBroadcastReceive(blueToothInterface);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        context.registerReceiver(broadcastReceive, intentFilter);
    }

    private void unregisterBluetoothReceiver() {
        Log.i(TAG, "Bluetooth broadcast monitoring off");
        if (broadcastReceive != null) {
            context.unregisterReceiver(broadcastReceive);
            broadcastReceive = null;
        }
    }


    private void noNetworkOnlyGetOneHopNode(List<NeighborInfo> neighborInfoList) {
        LitePal.deleteAll(NeighborInfo.class);
        pairedDevicesSet.clear();
        pairedDevicesSet = bluetoothUtil.getDevicesList();

        for (NeighborInfo device : neighborInfoList){
            String address = device.getNeighborMac();
            List<String> path = new ArrayList<>();
            path.add(localName);
            path.add(device.getNeighborName());
            NeighborInfo neighbor = new NeighborInfo(address, device.getNeighborName(), 1, new Date(), path.toString());
            LitePal.deleteAll(NeighborInfo.class, "neighborMac = ? and neighborName = ?",address,device.getNeighborName());
            neighbor.save();
        }

        if(neighborInfoList.size() !=0){
            HandlerTool.init(neighborInfoList);
        }
        HandlerTool.setLocalName(localName);
        HandlerTool.setLocalMacAddress(localMacAddress);

//        for (BluetoothDevice device : pairedDevicesSet) {
//            String address = device.getAddress();
//            List<String> path = new ArrayList<>();
//            path.add(localName);
//            path.add(device.getName());
//            NeighborInfo neighbor = new NeighborInfo(address, device.getName(), 1, new Date(), path.toString());
//            LitePal.deleteAll(NeighborInfo.class, "neighborMac = ? and neighborName = ?",address,device.getName());
//            neighbor.save();
//        }
        isupdateNeiView = true;
    }

    List<BluetoothDevice> pairedDevicesSet = new ArrayList<>();
    HashSet<String> scanRangeAlldevices = new HashSet<>();

    private void printLog(BluetoothDevice device) {
        String s = "";
        int BondState = device.getBondState();
        switch (BondState) {
            case BluetoothDevice.BOND_BONDED:
                s = "---name：" + device.getName() + "\n" + "MAC：" + device.getAddress() + "\n" + "state：paired" + "\n";
                break;
            default:
                s = "---name：" + device.getName() + "\n" + "MAC：" + device.getAddress() + "\n" + "state：unknown" + "\n";
                break;
        }

        Log.e(TAG, s);
    }



    private void getPairedDevices() {
        List<NeighborInfo> neighborInfoList = new ArrayList<>();
        for (BluetoothDevice device : pairedDevicesSet) {
            String address = device.getAddress();
            List<String> path = new ArrayList<>();
            path.add(localName);
            path.add(device.getName());
            NeighborInfo neighbor = new NeighborInfo(address, device.getName(), 1, new Date(), path.toString());
            neighborInfoList.add(neighbor);
        }
        if(deviceInfo == null){
            deviceInfo = LitePal.findFirst(DeviceInfo.class);
        }
        if (deviceInfo != null) {
            List<DeviceInfo> infos = LitePal.findAll(DeviceInfo.class);
            RouterTool.setDeviceInfo(infos.get(0));
            deviceInfo = infos.get(0);
            String mid = deviceInfo.getManet_UUID();
            if (neighborInfoList.size() != 0) {
                try {
                    String api = APIUrl.uploadRouterNeighbor;
                    JSONObject jsonObject = new JSONObject();
                    JSONArray jsonArray = new JSONArray(Collections.singletonList(neighborInfoList));
                    jsonObject.put("MANET_UUID", mid);
                    jsonObject.put("sourceName", localName);
                    jsonObject.put("sourceMAC", localMacAddress);
                    jsonObject.put("neighbors", jsonArray);
                    RxHttp.postJson(api).addAll(jsonObject.toString())
                            .asClass(Response.class)
                            .subscribe(data -> {
                                if (data.getCode() == 0) {
                                    String r1 = data.getData().toString();
                                    HashMap<String, Object> map = JsonUtils.jsonToPojo(r1, HashMap.class);
                                    String MANET_UUID = map.get("MANET_UUID") == null ? "none" : map.get("MANET_UUID").toString();
                                    String userInfo = map.get("userInfo") == null ? "member" : map.get("userInfo").toString();
                                    if (!MANET_UUID.equals("none") || !MANET_UUID.equals("")) {
                                        String uuid = deviceInfo.getUuid();
                                        DeviceInfo info = new DeviceInfo();
                                        info.setManet_UUID(MANET_UUID);
                                        info.setRole(userInfo);
                                        info.updateAll("uuid = ?", uuid);
                                    }
                                    String router = map.get("router").toString();
                                    map = JsonUtils.jsonToPojo(router, HashMap.class);
                                    String member = map.get("member").toString();
                                    List<HashMap> memberlist = JsonUtils.jsonToList(member, HashMap.class);
                                    HashMap<String, String> allnodes = new HashMap<>();
                                    for (HashMap tp : memberlist) {
                                        String tname = tp.get("sourceName").toString();
                                        String tMAC = tp.get("sourceMAC") == null ? "none" : tp.get("sourceMAC").toString();
                                        allnodes.put(tname, tMAC);
                                    }
                                    router = map.get("router").toString();
                                    Log.e(TAG, router);
                                    List<HashMap> routerlist = JsonUtils.jsonToList(router, HashMap.class);
                                    LitePal.deleteAll(NeighborInfo.class);
                                    for (HashMap tp : routerlist) {
                                        int hop = Integer.parseInt(tp.get("hop").toString());
                                        String tname = tp.get("dest").toString();
                                        String path = tp.get("path").toString();
                                        String tMAC = allnodes.get(tname);
                                        NeighborInfo neighbor = new NeighborInfo(tMAC, tname, hop, new Date(), path);
                                        neighbor.save();
                                    }
                                } else if (data.getCode() == 2) {
                                    String MANET_UUID = data.getData() == null ? "none" : data.getData().toString();
                                    if (!MANET_UUID.equals("none") || !MANET_UUID.equals("")) {
                                        String uuid = deviceInfo.getUuid();
                                        DeviceInfo info = new DeviceInfo();
                                        info.setManet_UUID(MANET_UUID);
                                        info.updateAll("uuid = ?", uuid);
                                    }

                                }
                                List<NeighborInfo> list = LitePal.where("hop = ?","1").find(NeighborInfo.class);
                                if(list.size() !=0){
                                    HandlerTool.init(list);
                                }
                                HandlerTool.setLocalName(localName);
                                HandlerTool.setLocalMacAddress(localMacAddress);
                                new Timer().schedule(new TimerTask() {
                                    public void run() {
                                        isupdateNeiView = true;
                                    }
                                }, 2000);
                            }, throwable -> {
                                Log.d(TAG, throwable.toString());
                                noNetworkOnlyGetOneHopNode(neighborInfoList);
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                    noNetworkOnlyGetOneHopNode(neighborInfoList);
                }
            }
        } else {
            noNetworkOnlyGetOneHopNode(neighborInfoList);
        }



    }
}