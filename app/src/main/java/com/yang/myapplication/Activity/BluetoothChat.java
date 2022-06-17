package com.yang.myapplication.Activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.yang.myapplication.entity.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.yang.myapplication.Adapter.MessageAdapter;
import com.yang.myapplication.Adapter.NeighborAdapter;
import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.LoginTool;
import com.yang.myapplication.Tools.NetworkTool;
import com.yang.myapplication.Tools.PermissionCheck;
import com.yang.myapplication.Tools.ProtocolModel;
import com.yang.myapplication.Tools.RandomID;
import com.yang.myapplication.Tools.RouterTool;
import com.yang.myapplication.Tools.TimeParse;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import rxhttp.RxHttp;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class BluetoothChat extends AppCompatActivity implements RecycleViewInterface {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private  final int LOCATION_PERMISSION_REQUEST = 101;
    private  final int SELECT_DEVICE = 102;
    public static final int MESSAGE_STATE_CHANGE = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    private String connectedDevice;
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private ChatUtils chatUtils = null;


    private ListView listMainChat;
    private EditText edCreateMessage;

    private Button button_send ,button_cancel,button_transfer,button_net_send;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (message.arg1) {
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            MessageDB.checkUnSendMessage(localName,localMacAddress);
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            if(localName.equals(ownerName)){
//                                if(!tagWitch.contains(connectedDevice)){
//                                    tagWitch.add(connectedDevice);
//                                    sendBroadcastMessage();
//                                }
                                cloudMessageResendOwn();
                                if(label.equals("")){
                                    handleUnreadMessage();
                                }
                            }
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) message.obj;
                    String inputBuffer = new String(readBuf, 0, message.arg1);
                    Log.d(TAG,"MESSAGE_READ:"+inputBuffer);
                    if(inputBuffer.contains("FLAG") ) {
                        handlerFALGMsg(inputBuffer);
                        break;
                    } else if (inputBuffer.contains("#")) {
                        Log.d(TAG,"creating neighbor table");
                        receiveBroadcastMessage(inputBuffer);
                        break;
                    }else if (inputBuffer.contains("@")) {
                        handleMultihop(inputBuffer);
                        break;
                    } else if (inputBuffer.contains("&"))  {
                        label = "";
                        setState("Connected: " + connectedDevice);
                        isNoneOrShowBtn(1);
                        // 直接连接
                        if (inputBuffer.contains("END")) {
                            String format = MessageDB.FormatMessage(1, inputBuffer, "1");
                            chatUtils.write(format.getBytes());
                            break;
                        }
                        if (MessageDB.storeMessageEachTime(inputBuffer, 1)) {
                            reLoadMessageFromDB();
                        }
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) message.obj;
                    String writeMessage = new String(writeBuf);
//                   Log.d(TAG,"MESSAGE_WRITE:"+writeMessage);
                    if(writeMessage.contains("&") && writeMessage.contains("FLAG")){
                        break;
                    } else if (writeMessage.contains("#") ) {
                        break;
                    } else if (writeMessage.contains("@")) {
                        String SOURCENAME = transferMessage.split("@")[3];
                        String ACK = transferMessage.split("@")[8];
                        if (SOURCENAME.equals(localName) && ACK.equals("0")) {
                            if (MessageDB.storeMessageTheFirstHop(transferMessage, 0)) {
                                reLoadMessageFromDB();
                            }
                        }
                        break;
                    } else if (writeMessage.contains("&")) {
                        //TODO  P2P
                        isNoneOrShowBtn(1);
                        label = "";
                        setState("Connected: " + connectedDevice);
                        if (writeMessage.contains("END")) {
                            if (MessageDB.storeMessageEachTime(writeMessage, 0)) {
                                reLoadMessageFromDB();
                            }
                        }else {
                            // receive from target
                            if (MessageDB.storeMessageEachTime(writeMessage, 1)) {
                                reLoadMessageFromDB();
                            }
                        }
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    if (label.equals(""))
                        Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    if (label.equals(""))
                        Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        }
    });



    //because of some reason, the feedback only resend the last one
    //however the source can write each line.
    private void resendFeedback(){
        if(feedbackList.size()==0) return;
        Timer resendTimer = new Timer();
        resendTimer.schedule(new TimerTask() {
            public void run() {
                for(int i = 0 ; i < feedbackList.size();i++){
                    if(chatUtils.getState() == ChatUtils.STATE_CONNECTED){
                        System.out.println("resendFeedback  -- "+feedbackList.get(i));
                        try {
                            chatUtils.write(feedbackList.get(i).getBytes());
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                feedbackList.clear();
                resendTimer.cancel();
            }
        }, 10*1000,  3*1000);
    }
    private List<String> feedbackList = new ArrayList<>();
    private synchronized void handlerFALGMsg(String inputBuffer){
        String[] str = inputBuffer.split("&");
        if(str[2].equals("FLAG=DELETE")|| str[2].contains("DEL") || str[2].equals("FLAG=UPDATE") || str[2].contains("UPD")) {
            String type = "UPDATE";
            if(str[2].equals("FLAG=DELETE") || str[2].contains("DEL")){
                type = "DELETE";
            }
            System.out.println("handlerFALGMsg--->"+inputBuffer);
            MessageDB.TestMethos(str[0],str[1],type);
            return;
        }
        String uuid = str[7];
        String end = str[6];
        String ORGINAL = str[8];
        String MESSAGE = str[0];
        String SOURCENAME = str[1];
        String SOURCMAC = str[8];
        String TAREGETNAME = str[3];
        String TARGETMAC = str[4];
        String START = str[5];
        String flag = str[9];
        if (str[2].equals(localMacAddress) && (inputBuffer.contains("FLAG=UPD")|| inputBuffer.contains("FLAG=DEL"))) {
            // update or delete cloud
            String type = "UPDATE";
            if(inputBuffer.contains("FLAG=DELETE")){
                type = "DELETE";
            }
            MessageDB.TestMethos(uuid,end,type);
            return;
        }

        List<MessageInfo> isExist = LitePal.where("uuid = ?", uuid).find(MessageInfo.class);
        String time = TimeParse.stampToDate(TimeParse.getTimestamp());
        MessageInfo newRecord = new MessageInfo(uuid, MESSAGE, START, time, 1, TAREGETNAME, TARGETMAC, SOURCENAME, SOURCMAC, 1);
        // update end or create
        if (flag.equals("FLAG=SO")) {
            newRecord = new MessageInfo(uuid, MESSAGE, START, end, 1, TAREGETNAME, TARGETMAC, SOURCENAME, SOURCMAC, 1);
            if (isExist.size() == 0) {
                newRecord.save();
            }else {
                MessageInfo oldRecord = new MessageInfo();
                oldRecord.setReadDate(end);
                oldRecord.setIsUpload(1);
                oldRecord.setIsRead(1);
                oldRecord.updateAll("uuid = ?", uuid);
            }
            reLoadMessageFromDB();
            String tmp = "ID&END&FLAG";
            String feedback = tmp.replaceAll("ID", uuid).replaceAll("END", end).replaceAll("FLAG", "FLAG=DELETE");
            feedbackList.add(feedback);
            resendFeedback();
            return;
        } else if (flag.equals("FLAG=TA")) {
            if (isExist.size() == 0) {
                newRecord.save();
            } else {
                if (!isExist.get(0).getReadDate().equals("END")) {
                    time = isExist.get(0).getReadDate();
                } else {
                    MessageInfo oldRecord = new MessageInfo();
                    oldRecord.setReadDate(time);
                    oldRecord.setIsUpload(1);
                    oldRecord.setIsRead(1);
                    oldRecord.updateAll("uuid = ?", uuid);
                }
            }
            reLoadMessageFromDB();
            String tmp = ProtocolModel.feedback;
            String feedback = tmp.replaceAll("ID", uuid).replaceAll("END", time).replaceAll("FLAG", "FLAG=UPDATE");
            feedbackList.add(feedback);
            resendFeedback();
            return;
        }
    }

    private void  handleUnreadMessage(){
        MessageDB.CheckMSGFromCloud(localName,connectedDevice,"Sourceunread",chatUtils,localMacAddress);
        MessageDB.CheckMSGFromCloud(localName,connectedDevice,"Targetunread",chatUtils,localMacAddress);
    }

    private String label="";
    private void setState(CharSequence subTitle){

        if(label.equals("")){
            Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
        }else {
            if(subTitle.equals("Not Connected"))
                Objects.requireNonNull(getSupportActionBar()).setSubtitle(label);
            else
                Objects.requireNonNull(getSupportActionBar()).setSubtitle(label+"-"+subTitle);
        }
    }


    //当存在连接的时候重发信息，是owner发消息，而不是
    private void cloudMessageResendOwn(){
        List<MessageInfo> isReadlist =  MessageDB.queryAllUnreadMsgDB("0",2);
        System.err.println("isReadlist====="+isReadlist.size());
        if (isReadlist.size()==0){
            return;
        }
        for(MessageInfo info : isReadlist) {
            String ID = info.getUuid();
            String MESSAGE = info.getContent();
            String SOURCENAME = info.getSourceName();
            String SOURCMAC = info.getSourceMAC();
            String TAREGETNAME = info.getTargetName();
            String TARGETMAC = info.getTargetMAC();
            String START = info.getSendDate();
            String END = info.getReadDate();
            String tmp = ProtocolModel.directMsg;
            tmp = tmp.replaceAll("MESSAGE", MESSAGE).replaceAll("SOURCENAME", SOURCENAME)
                    .replaceAll("SOURCMAC", SOURCMAC).replaceAll("TAREGETNAME", TAREGETNAME).replaceAll("TARGETMAC", TARGETMAC)
                    .replaceAll("START", START).replaceAll("ID", ID);

            //自己发给别人的
            if (info.getTargetName().equals(connectedDevice) && info.getSourceName().equals(localName)) {
                tmp = tmp.replaceAll("END", "END").replaceAll("ACK", "0");
                chatUtils.write(tmp.getBytes());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            //自己的消息 返回ACK
            if (info.getTargetName().equals(localName) && info.getSourceName().equals(connectedDevice)) {
                END = TimeParse.stampToDate(TimeParse.getTimestamp());
                tmp = tmp.replaceAll("END", END).replaceAll("ACK", "1");
                chatUtils.write(tmp.getBytes());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
        }


    }

    private static RecyclerView neighborView;
    private static List<NeighborInfo> allDevice = new ArrayList<>();
    public static void loadTestRouterTool(){

    }

    public void updateView() {
        allDevice.clear();
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        if(list.size()>0){
            allDevice.addAll(list);
            NeighborAdapter adapter = new NeighborAdapter(context, R.layout.neighbor_table, list,this);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(BluetoothChat.this);
            neighborView.setLayoutManager(linearLayoutManager);
            neighborView.setItemAnimator(new DefaultItemAnimator());
            neighborView.setAdapter(adapter);
        }
    }


    int count  = 0;
    private void ConnectNextDevice(String LASTNAME,int times,String msg){
        count = 0;
        Timer timer = new Timer();
        Timer timer2 = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                if (chatUtils.getState() == ChatUtils.STATE_CONNECTED){
                    timer.cancel();
                    timer2.cancel();
                    chatUtils.write(msg.getBytes());
                }
                if(count==times){
                    timer.cancel();
                    timer2.cancel();
                }
            }
        }, 0,  300);

        for (BluetoothDevice device : pairedDevicesSet) {
            if (!device.getName().equals(LASTNAME)) {
                timer2.schedule(new TimerTask() {
                    public void run() {
                        chatUtils.connect(device, label);
                        count++;
                    }
                }, 0,  2000);
            }
        }
    }



//    private void handleMultihopCloudMessage(String inputBuffer){
//        String[] strings = inputBuffer.split("@");
//        String model = ProtocolModel.cloudMultihop;
//        label = "multi_hop";
//        Response isFlag = MessageDB.updatecloudMessage(inputBuffer,localName);
//        if(isFlag.getCode()== -1){
//            return;
//        }
//        if(isFlag.getCode()== 0){
//            if (chatUtils!=null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
//                chatUtils.stop();
//            }
//            reLoadMessageFromDB();
//            return;
//        }
//        if(isFlag.getCode()== 1){
//            String LASTNAME = strings[4];
//            model = String.valueOf(isFlag.getData());
//            if (chatUtils!=null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
//                chatUtils.stop();
//            }
//            ConnectNextDevice(LASTNAME,3,model);
//            return;
//        }
//    }

    private void handleMultihop(String inputBuffer) {
        String[] strings = inputBuffer.split("@");
        String model = ProtocolModel.Multihop;
        label = "multi_hop";
        String LASTNAME = strings[4];

        Response isFlag = MessageDB.storeMessageMultiHop(inputBuffer,localName);
        if(isFlag.getCode() == -1){
            return;
        }
        if(isFlag.getCode() == 1){
            //transfering
            Toast.makeText(context, "transferring: the "+Integer.parseInt(strings[5]) +" hop", Toast.LENGTH_SHORT).show();
            if (chatUtils!=null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
                chatUtils.stop();
            }
            ConnectNextDevice(LASTNAME,5, String.valueOf(isFlag.getData()));
        }
        if(isFlag.getCode() == 2){
            reLoadMessageFromDB();

            //return 消息
            String[] tmp = String.valueOf(isFlag.getData()).split("@");
            //destination
            String MESSAGE = tmp[0];
            String NEIGHBORMAC = tmp[1];
            String NEIGHBORNAME = tmp[2];
            String SOURCENAME = tmp[3];
            String start = tmp[6];
            String end = tmp[7];
            String ID = tmp[9];
            String ORGINAL = tmp[10];
            String SOURCEMAC = tmp[11];

            //send ack
            model = model.replaceAll("ID",ID)
                    .replaceAll("ACK","1").replaceAll("SOURCENAME",NEIGHBORNAME)
                    .replaceAll("SOURCEMAC",NEIGHBORMAC).replaceAll("NEIGHBORNAME",SOURCENAME)
                    .replaceAll("NEIGHBORMAC",SOURCEMAC).replaceAll("END",end)
                    .replaceAll("START",start).replaceAll("HOP",ORGINAL)
                    .replaceAll("ORGINAL",ORGINAL).replaceAll("LASTNAME",localName)
                    .replaceAll("MESSAGE",MESSAGE);
            transferMessage = model;
            chatUtils.write(model.getBytes());
        }
        if(isFlag.getCode() == 3){
            if (chatUtils!=null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
                chatUtils.stop();
            }
            ConnectNextDevice(LASTNAME,5,String.valueOf(isFlag.getData()));
        }
        if(isFlag.getCode() == 4){
            String[] tmp = String.valueOf(isFlag.getData()).split("@");
            String ID = tmp[9];
            String MESSAGE = tmp[0];
            String start = tmp[6];
            String end = tmp[7];
            String NEIGHBORMAC = tmp[1];
            String NEIGHBORNAME = tmp[2];
            String SOURCENAME = tmp[3];
            String SOURCEMAC = tmp[11];
            if (chatUtils!=null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
                chatUtils.stop();
            }
            //多跳的ACK也就是原目标设备收到目的的ACK，更新到数据库中。
            MessageDB.checkUnSendMessageMulti(ID,end);
            MessageInfo message = new MessageInfo(ID, MESSAGE, start, end, 1, SOURCENAME, SOURCEMAC,NEIGHBORNAME ,NEIGHBORMAC);
            if(MessageDB.storeMessage(ID, message)){
                reLoadMessageFromDB();
            }
        }

    }

    private void receiveBroadcastMessage(String inputBuffer){
        if(!inputBuffer.contains("#")) return;
        String[] strings = inputBuffer.split("#");
        for(String info :strings ){
            String[] neighbor = info.split(",");
            String nei_name = neighbor[0];
            String nei_mac = neighbor[1];
            int hop = Integer.parseInt(neighbor[2]);
            String name = neighbor[3];
            if(!localMacAddress.equals(nei_mac) && !localName.equals(nei_name)){
                NeighborInfo neighborInfo = new NeighborInfo(nei_mac,nei_name,hop,new Date(),"");
                List<NeighborInfo> exists = LitePal.where("neighborMac = ?", nei_mac).find(NeighborInfo.class);
                if(exists.size() >= 1){
                    LitePal.deleteAll(NeighborInfo.class, "neighborMac = ?" , nei_mac);
                }
                neighborInfo.save();
            }
            String out = "nei_name  :"+nei_name+" nei_mac:  "+nei_mac+"hop:  "+hop+" localName: "+localName;
            Log.e("--------",out);
        }
        updateView();
    }
    private  String localName = null;
//    private Set<String> tagWitch = new HashSet<>();

    private void sendBroadcastMessage(){
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        if(list.size()==1) return;
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for(NeighborInfo info : list){
            String message =JsonUtils.entittyToString(info,localName );
            sb.append(message);
            if(index<list.size()) sb.append("#");
            index++;
        }
        if(!sb.toString().isEmpty()){
            chatUtils.write(sb.toString().getBytes());
        }
    }


    private void multiConnection(){
        for(BluetoothDevice info : pairedDevicesSet){
            if (chatUtils.getState() == chatUtils.STATE_NONE) {
                if(!info.getName().equals(localName)){
                    chatUtils.connect(info,label);
                }
            }
        }
    }
    //{des mac, des name, source name, message,start,end}
    private static  String transferMessage =  ProtocolModel.Multihop;;

    private void isNoneOrShowBtn(int type){
        if(type==1){
            button_transfer.setVisibility(View.GONE);
            button_send.setVisibility(View.VISIBLE);
            button_net_send.setVisibility(View.GONE);
        }else if(type==2){
            button_send.setVisibility(View.GONE);
            button_transfer.setVisibility(View.VISIBLE);
            button_net_send.setVisibility(View.GONE);
        }else if(type==3){
            button_send.setVisibility(View.GONE);
            button_transfer.setVisibility(View.GONE);
            button_net_send.setVisibility(View.VISIBLE);
        }
    }
    private BluetoothDevice directCurrentDevice = null;

    @Override
    public void onItemClickPair(int position) {
        label = "";
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        if(list.size()==0) {
            Toast.makeText(context, "Please finish the scanning", Toast.LENGTH_SHORT).show();
            return;
        }
        if(position >= pairedDevicesSet.size()){
            label = "multihop";
            NeighborInfo tmp = list.get(position);
            String model = ProtocolModel.Multihop;
            model = model.replaceAll("ORGINAL",String.valueOf(tmp.getHop())).replaceAll("HOP",String.valueOf(tmp.getHop()))
                    .replaceAll("NEIGHBORMAC",tmp.getNeighborMac()).replaceAll("NEIGHBORNAME",tmp.getNeighborName())
                    .replaceAll("SOURCENAME",localName).replaceAll("SOURCEMAC",localMacAddress);
            transferMessage = model;
            chatUtils.setState(ChatUtils.STATE_NONE);
            isNoneOrShowBtn(2);
            multiConnection();
        }else {
            isNoneOrShowBtn(1);
            if (chatUtils != null) {
                chatUtils.stop();
            }
            directCurrentDevice = pairedDevicesSet.get(position);
            chatUtils.connect(pairedDevicesSet.get(position),"directly");
        }
    }



    @Override
    public void onItemClick(int position) {
        try {
            if(NetworkTool.ping("google.com",10)){
                NeighborInfo current = allDevice.get(position);
                String api = APIUrl.APIlogin;
                JSONObject jsonObject = new JSONObject();
                String targetName = current.getNeighborName();
                String targetMAC= current.getNeighborMac();
                try {
                    jsonObject.put("targetName", targetName);
                    jsonObject.put("targetMAC", targetMAC);
                    jsonObject.put("sourceName", localName);
                    jsonObject.put("sourceMAC", localMacAddress);
                    jsonObject.put("sendDate", new Date());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RxHttp.postJson(api).addAll(jsonObject.toString())
                        .asClass(Response.class).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(data -> {
                            if(data.getCode()==0){
                                setState("Connected: " + targetName);
                                isNoneOrShowBtn(3);
                                Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_LONG).show();
                            }else {
                                Toast.makeText(getApplicationContext(),"unable to connect", Toast.LENGTH_LONG).show();
                            }
                        }, throwable -> {
                            //请求失败
                            Log.d(TAG,throwable.toString());
                            Toast.makeText(getApplicationContext(), "network refused!", Toast.LENGTH_LONG).show();
                        });



            }else {
                Toast.makeText(context, "sorry, the network is unavailable", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static String ownerName = "Nexus 5";
    private static DeviceInfo deviceInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);
        initDB();
        context = this;
        checkPermissions();
        initBluetooth();
        if(chatUtils==null){
            chatUtils = new ChatUtils(context,handler);
        }
        init();
        devicelogin();


        scanDevices();
    }

    //自动登录 。 测试阶段 主要是确定在同一个MANET
    private void devicelogin() {
        Timer resendTimer = new Timer();
        resendTimer.schedule(new TimerTask() {
            public void run() {
                LoginTool.funclogin(localName);
                List<DeviceInfo>  list =  LitePal.findAll(DeviceInfo.class);
                if(list.size()==1) {
                    deviceInfo = list.get(0);
                    Thread.currentThread();
                    resendTimer.cancel();

                }
            }
        }, 0,  5*1000);
    }

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

//    private ArrayAdapter<String> adapterMainChat ;
    private MessageAdapter adapterMainChat ;

    private ProgressBar progress_scan_devices;



    //reload message from DB
    private void reLoadMessageFromDB(){
        List<MessageInfo> list = MessageDB.queryAllFromDB(localName);
        adapterMainChat = new MessageAdapter(context, R.layout.message,list );
        listMainChat.setAdapter(adapterMainChat);
    }


    private void init(){
        listMainChat = findViewById(R.id.list_conversation);
        edCreateMessage = findViewById(R.id.ed_enter_message);
        button_send = findViewById(R.id.button_send);
        button_net_send= findViewById(R.id.button_net_send);
        progress_scan_devices = findViewById(R.id.progress_scan_devices);
        button_cancel= findViewById(R.id.button_cancel);
        button_transfer= findViewById(R.id.button_transfer);
        neighborView = findViewById(R.id.neighborView);
        edCreateMessage.clearFocus();
        registerReceiver(receiver, makeFilters());
        LitePal.deleteAll(MessageInfo.class);
        reLoadMessageFromDB();
        localMacAddress =  NetworkTool.getMacAddr();
        localName = bluetoothAdapter.getName();
        button_transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edCreateMessage.getText().toString().trim();
                if(transferMessage.contains("MESSAGE")){
                    String uuid =String.valueOf(RandomID.genIDWorker());
                    String time = TimeParse.stampToDate(TimeParse.getTimestamp());
                    transferMessage =  transferMessage.replaceAll("START",time).replaceAll("ID",uuid).replaceAll("ACK","0").replaceAll("MESSAGE",message);
                }else {
                }
                transferMessage = transferMessage.replaceAll("LASTNAME",localName);
                edCreateMessage.setText("");
                chatUtils.write(transferMessage.getBytes());
            }
        });


        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(directCurrentDevice == null && chatUtils.getState() != ChatUtils.STATE_CONNECTED) return;
                if(directCurrentDevice == null){
                    for(int i =0;i<pairedDevicesSet.size();i++){
                        if(pairedDevicesSet.get(i).getName().equals(connectedDevice) ){
                            directCurrentDevice = pairedDevicesSet.get(i);
                        }
                    }
                }
                String message = edCreateMessage.getText().toString().trim();
                String tmp = ProtocolModel.directMsg;
                if (!message.isEmpty()) {
                    edCreateMessage.setText("");
                    label = "";
                    String time = TimeParse.stampToDate(TimeParse.getTimestamp());
                    String TAREGETNAME = directCurrentDevice.getName();
                    String TARGETMAC = directCurrentDevice.getAddress();
                    tmp = tmp.replaceAll("START",time).replaceAll("TAREGETNAME",TAREGETNAME).replaceAll("TARGETMAC",TARGETMAC).replaceAll("ID", String.valueOf(RandomID.genIDWorker())).replaceAll("MESSAGE",message).replaceAll("SOURCENAME",localName).replaceAll("SOURCMAC",localMacAddress).replaceAll("ACK","0");
                    chatUtils.write(tmp.getBytes());
                }
            }
        });
        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chatUtils !=null){
                    chatUtils.stop();
                }
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();
        updateView();
        scanDevices();
        if (chatUtils != null) {
            if (chatUtils.getState() == chatUtils.STATE_NONE) {
                chatUtils.start();
            }
        }
    }
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(context, "No Bluetooth found", Toast.LENGTH_SHORT).show();
        }
        enable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    private final UUID[] UUIDList = new UUID[]{UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"),UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")};


    private final static String TAG = "---->";
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
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                        chatUtils.connect(device,"");
                        return;

                    }
                }
            });

    private void scanDevices(){
        progress_scan_devices.setVisibility(View.VISIBLE);
//        reLoadMessageFromDB();
        listMainChat.setAdapter(adapterMainChat);
        pairedDevicesSet.clear();
        allDevices.clear();
        LitePal.deleteAll(NeighborInfo.class);
        registerReceiver(receiver, makeFilters());
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.menuScan:
                scanDevices();
                break;
            case R.id.menu_search:
//                intent = new Intent(context,DeviceListActivity.class);
//                startActivityIntent.launch(intent);

                intent = new Intent(context,DeviceMainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("username",localName);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.discoverable:
                Toast.makeText(context, "Clicked enable", Toast.LENGTH_SHORT).show();
                enable();
                updateView();
                break;
            case R.id.historyMsg:
                intent = new Intent(context,MessageListActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }
    private void enable(){
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils != null){
            chatUtils.stop();
        }
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(receiver);
    }


    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(getApplicationContext(), PermissionCheck.permissions[0]);
            int l = ContextCompat.checkSelfPermission(getApplicationContext(), PermissionCheck.permissions[1]);
            int m = ContextCompat.checkSelfPermission(getApplicationContext(), PermissionCheck.permissions[2]);
            if (i != PackageManager.PERMISSION_GRANTED ||
                    l != PackageManager.PERMISSION_GRANTED ||
                    m != PackageManager.PERMISSION_GRANTED) {
                startRequestPermission();
            }
        }
    }
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(BluetoothChat.this,  PermissionCheck.permissions, 321);
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

    private IntentFilter makeFilters(){
        IntentFilter deviceIntentFilter = new IntentFilter();
        deviceIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        deviceIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        return deviceIntentFilter;
    }

    HashMap<String,BluetoothDevice> allDevices = new HashMap<>();
    List<BluetoothDevice> pairedDevicesSet = new ArrayList<>();


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e(TAG, "Find device completely");
                progress_scan_devices.setVisibility(View.GONE);
                bluetoothAdapter.cancelDiscovery();
                unregisterReceiver(receiver);


                updateSql();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name != null) {
                    int rssi = Math.abs(intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI));
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(!allDevices.containsKey(device.getAddress())){
                        allDevices.put(device.getAddress(),device);
                    }
                    String s = "";
                    int BondState = device.getBondState();
                    switch (BondState){
                        case BluetoothDevice.BOND_BONDED:
                            s = "rssi："+ rssi +"---name：" + device.getName() + "\n" + "MAC：" + device.getAddress() + "\n" + "state：paired" + "\n";
                            break;
                        default:
                            s = "rssi："+ rssi +"---name：" + device.getName() + "\n" + "MAC：" + device.getAddress() + "\n" + "state：unknown" + "\n";
                            break;
                    }
                    Log.e(TAG,s);
                }
            }
        }

        private void updateSql() {
            LitePal.deleteAll(NeighborInfo.class);
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if(pairedDevices != null && pairedDevices.size()>0){
                for (BluetoothDevice device : pairedDevices){
                    pairedDevicesSet.add(device);
                }
            }
            List<NeighborInfo> neighborInfoList = new ArrayList<>();
            for(BluetoothDevice device : pairedDevicesSet) {
                String address = device.getAddress();
                if (allDevices.containsKey(address)) {
                    NeighborInfo neighbor = new NeighborInfo(address, device.getName(),1,new Date(),"");
                    neighborInfoList.add(neighbor);
                }
            }
            if(deviceInfo!=null){
                if(neighborInfoList.size() != 0){
                    RouterTool.setDeviceInfo(deviceInfo);
                    RouterTool.uploadRouterNeighbor(neighborInfoList,localName,localMacAddress );
                }
            }



            // owner

//
//            for(NeighborInfo info : neighborInfoList){
//                info.save();
//            }
//            updateView();
        }

    };





}