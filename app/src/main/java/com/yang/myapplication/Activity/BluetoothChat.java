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
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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

import com.alibaba.fastjson.JSONArray;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;


public class BluetoothChat extends AppCompatActivity implements RecycleViewInterface {
    private Context context;
    private BluetoothAdapter bluetoothAdapter = null;
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
                            setState("UN");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("UN");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("ING");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState( connectedDevice);
//                            setState("Connected: " + connectedDevice);
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
                    } else if (inputBuffer.contains("@")) {
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
                    } else if (writeMessage.contains("@")) {
                        /* *
                         * 0 ID
                         * 1 MESSAGE
                         * 2 START
                         * 3 END
                         * 4 DESNAME
                         * 5 DESMAC
                         * 6 SOURCENAME
                         * 7 SOURCEMAC
                         * 8 Router [A,B,C,D]
                         * 9 UPLOAD
                         * */
                        String[] tmp = writeMessage.split("@");
                        if(tmp.length > 4){
                            String SOURCENAME = tmp[6];
                            if (SOURCENAME.equals(localName)) {
                                if (MessageDB.storeMessage(writeMessage)) {
                                    reLoadMessageFromDB();
                                }
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

    private String label="";
    private void setState(CharSequence subTitle){
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);

//        if(label.equals("")){
//            Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
//        }else {
//            if(subTitle.equals("Not Connected"))
//                Objects.requireNonNull(getSupportActionBar()).setSubtitle(label);
//            else
//                Objects.requireNonNull(getSupportActionBar()).setSubtitle(label+"-"+subTitle);
//        }
    }




    private static RecyclerView neighborView;
    private static List<NeighborInfo> allDevice = new ArrayList<>();


    public void updateView() {
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
    private void sendFeedback(String inputBuffer, String  upload, String END ){
        String[] strings = inputBuffer.split("@");
        String feedback_model = ProtocolModel.Multi_hop_feedback;
        String ID = strings[0];
        String MESSAGE = strings[1];
        String START = strings[2];
        String DESNAME = strings[4];
        String DESMAC = strings[5];
        String SOURCENAME = strings[6];
        String SOURCEMAC = strings[7];
        String ROUTER = strings[8];
        String[] routers = RouterTool.routerList(ROUTER);
        MessageInfo message = new MessageInfo(ID, MESSAGE, START, END, 1, DESNAME, DESMAC, SOURCENAME, SOURCEMAC);
        if (MessageDB.storeMessage(ID, message)) {
            reLoadMessageFromDB();
        }
        feedback_model = feedback_model.replaceAll("UPLOAD", upload).replaceAll("ID", ID).replaceAll("END", END).replaceAll("ROUTER", ROUTER);
        if (chatUtils != null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
            chatUtils.write(feedback_model.getBytes());
        } else {
            multiConnection(routers[routers.length - 2], feedback_model);
        }
    }

    private void sendNextDevice(String inputBuffer,String[] routers,int i , String upload){
        String model =  ProtocolModel.Multi_hop;
        String[] strings = inputBuffer.split("@");
        String ID = strings[0];
        String MESSAGE = strings[1];
        String START = strings[2];
        String END = "END";
        String DESNAME = strings[4];
        String DESMAC = strings[5];
        String SOURCENAME = strings[6];
        String SOURCEMAC = strings[7];
        String ROUTER = strings[8];
        model =  model.replaceAll("ID", ID)
                .replaceAll("MESSAGE", MESSAGE)
                .replaceAll("START", START)
                .replaceAll("END", END)
                .replaceAll("DESNAME", DESNAME)
                .replaceAll("DESMAC", DESMAC)
                .replaceAll("SOURCENAME", SOURCENAME)
                .replaceAll("SOURCEMAC", SOURCEMAC).replaceAll("ROUTER", ROUTER)
                .replaceAll("UPLOAD", upload);
        if (chatUtils != null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
            chatUtils.stop();
        }
        multiConnection(routers[i+1], model);
    }
    private void sendNextDeviceFeedback(String inputBuffer, String nextRouter){
        String[] strings = inputBuffer.split("@");
        String ID = strings[0];
        String END = strings[1];
        String ROUTER = strings[2];
        String model =  ProtocolModel.Multi_hop_feedback;
        model =  model.replaceAll("ID", ID)
                .replaceAll("ROUTER", ROUTER)
                .replaceAll("UPLOAD", "1")
                .replaceAll("END", END);
        multiConnection(nextRouter, model);
    }

    private void arriveMessage(String ID, String END){
        MessageDB.checkUnSendMessageMulti(ID, END);
        if (MessageDB.storeMessage(ID, END)) {
            reLoadMessageFromDB();
        }
    }
    private void constructNeighbor(String ROUTER) {
        String[] routers = RouterTool.routerList(ROUTER);
        int index = -1;
        for (int i = 0; i < routers.length; i++) {
            String cur = routers[i].trim().replaceAll(" ", "");
            if (cur.equals(localName)) {
                index = i;
                break;
            }
        }
        for (int i = 0; i < index; i++) {
            List<String> routing = new ArrayList<>();
            for (int j = i; j <= index; j++) {
                String tmp = routers[j].trim().replaceAll(" ", "");
                routing.add(tmp);
            }
            //one hop
            if (routing.size() > 2) {
                String name = routing.get(0);
                List<NeighborInfo> exists = LitePal.where("neighborName = ?", name).find(NeighborInfo.class);
                if(exists.size() == 0){
                    NeighborInfo neighbor = new NeighborInfo("", name, routing.size()-1, new Date(), routing.toString());
                    neighbor.save();
                }else if(exists.size() == 1){
                    NeighborInfo neighbor = new NeighborInfo();
                    neighbor.setHop(routing.size()-1);
                    neighbor.setPath( routing.toString());
                    neighbor.updateAll("neighborName = ?", name);
                }
            }
        }
        for (int i = routers.length ; i >= index; i--) {
            List<String> routing = new ArrayList<>();
            for (int j = index; j < i; j++) {
                String tmp = routers[j].trim().replaceAll(" ", "");
                routing.add(tmp);
            }
            //one hop
            if (routing.size() > 1) {
                String name = routing.get(routing.size() - 1);
                List<NeighborInfo> exists = LitePal.where("neighborName = ?", name).find(NeighborInfo.class);
                if(exists.size() == 0){
                    NeighborInfo neighbor = new NeighborInfo("", name, routing.size()-1, new Date(), routing.toString());
                    neighbor.save();
                }else if(exists.size() == 1){
                    NeighborInfo neighbor = new NeighborInfo();
                    neighbor.setHop(routing.size()-1);
                    neighbor.setPath( routing.toString());
                    neighbor.updateAll("neighborName = ?", name);
                }

            }
        }
        updateView();
    }
    private void handleMultihop(String inputBuffer) {
        String[] strings = inputBuffer.split("@");
        label = "multi_hop";
        if (strings.length > 4) {
            String DESNAME = strings[4];
            String ROUTER = strings[8];
            String SOURCENAME = strings[6];
            String UPLOAD = strings[9];
            String[] routers = RouterTool.routerList(ROUTER);
            for (int i = 1; i < routers.length; i++) {
                String cur = routers[i].trim().replaceAll(" ", "");
                if(cur.equals(localName) && cur.equals(DESNAME)) {
                    String END = TimeParse.stampToDate(TimeParse.getTimestamp());
                    if (UPLOAD.equals("0")) {
                        try {
                            String api = APIUrl.APICloudSendUnreadMessage;
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("msg", inputBuffer);
                            jsonObject.put("isRead", "1");
                            jsonObject.put("readDate", END);
                            jsonObject.put("uploadName", localName);
                            jsonObject.put("uploadMAC", localMacAddress);
                            String uploadTime = TimeParse.stampToDate(TimeParse.getTimestamp());
                            jsonObject.put("uploadTime", uploadTime);
                            RxHttp.postJson(api).addAll(jsonObject.toString())
                                    .asClass(Response.class)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(data -> {
                                        if (data.getCode() == 0) {
                                            sendFeedback(inputBuffer, "1",END);
                                        }
                                    }, throwable -> {
                                        Log.d(TAG, throwable.toString());
                                        sendFeedback(inputBuffer, "0",END);
                                    });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            sendFeedback(inputBuffer, "0",END);
                        }
                    } else {
                        sendFeedback(inputBuffer, "0",END);
                    }
                   break;
                }

                if(cur.equals(localName)) {
                    int index = i;
                    if (UPLOAD.equals("0")) {
                        try {
                            String api = APIUrl.APICloudSendUnreadMessage;
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("msg", inputBuffer);
                            jsonObject.put("isRead", "0");
                            jsonObject.put("uploadName", localName);
                            jsonObject.put("uploadMAC", localMacAddress);
                            String uploadTime = TimeParse.stampToDate(TimeParse.getTimestamp());
                            jsonObject.put("uploadTime", uploadTime);
                            RxHttp.postJson(api).addAll(jsonObject.toString())
                                    .asClass(Response.class)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(data -> {
                                        if (data.getCode() == 0) {
                                            sendNextDevice(inputBuffer, routers, index, "1");
                                        }
                                    }, throwable -> {
                                        Log.d(TAG, throwable.toString());
                                        sendNextDevice(inputBuffer, routers, index, "0");
                                    });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            sendNextDevice(inputBuffer, routers, index, "0");
                        }
                    } else {
                        sendNextDevice(inputBuffer, routers, index, "1");
                    }
                    break;

                }
            }
            if(!localName.equals(SOURCENAME)){
                constructNeighbor(ROUTER);
            }

        } else {
            String ID = strings[0];
            String END = strings[1];
            String ROUTER = strings[2];
            String[] routers = RouterTool.routerList(ROUTER);
            String UPLOAD = strings[3];
            if (chatUtils != null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
                chatUtils.stop();
            }
            for (int i = routers.length - 1; true; i--) {
                if (routers[0].trim().replaceAll(" ", "").equals(localName)) {
                    if (UPLOAD.equals("0")) {
                        try {
                            String api = APIUrl.APIDeleteMessage;
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("uuid", ID);
                            RxHttp.postJson(api).addAll(jsonObject.toString())
                                    .asClass(Response.class)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(data -> {
                                        if (data.getCode() == 0) {
                                            arriveMessage(ID, END);
                                        }
                                    }, throwable -> {
                                        Log.d(TAG, throwable.toString());
                                        arriveMessage(ID, END);
                                    });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            arriveMessage(ID, END);
                        }
                    } else {
                        arriveMessage(ID, END);
                    }
                    break;
                }

                String cur = routers[i].trim().replaceAll(" ", "");
                if (cur.equals(localName)) {
                    int index = i;
                    if (UPLOAD.equals("0")){
                        try {
                            String api = APIUrl.APIUpdateMessage;
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("uuid", ID);
                            jsonObject.put("isRead", "1");
                            jsonObject.put("readDate", END);
                            RxHttp.postJson(api).addAll(jsonObject.toString())
                                    .asClass(Response.class)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(data -> {
                                        if (data.getCode() == 0) {
                                            sendNextDeviceFeedback(inputBuffer,routers[index-1]);
                                        }
                                    }, throwable -> {
                                        multiConnection(routers[index-1], inputBuffer);
                                    });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            multiConnection(routers[index-1], inputBuffer);
                        }
                    }else {
                        multiConnection(routers[index-1], inputBuffer);
                    }

                    break;
                }
            }
        }
    }

    private  String localName = null;


    private void multiConnection(String name,String msg) {
        Timer timer = new Timer();
        count = 0;
        name = name.trim();
        if (pairedDevicesSet.size() == 0) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            pairedDevicesSet = new ArrayList<>(pairedDevices);
        }
        for (BluetoothDevice device : pairedDevicesSet) {
            if (device.getName().equals(name)) {
                timer.schedule(new TimerTask() {
                    public void run() {
                        chatUtils.connect(device, label);
                        count++;
                    }
                }, 0, 4000);
                break;
            }
        }
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            public void run() {
                if (count > 3 ) {
                    timer.cancel();
                    timer2.cancel();
                } else  if (chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
                    timer.cancel();
                    timer2.cancel();
                    if (!msg.equals("") || msg != null) {
                        chatUtils.write(msg.getBytes());
                    }
                }
            }
        }, 0, 300);



    }


    //{des mac, des name, source name, message,start,end}
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


    //更改路由结构
    private String combinationRouterPathMultihop(String MESSAGE, String uuid) {
        String model = ProtocolModel.Multi_hop;
        String time = TimeParse.stampToDate(TimeParse.getTimestamp());
        model = model.replaceAll("DESNAME", currentConnectDevice.getNeighborName())
                .replaceAll("DESMAC", currentConnectDevice.getNeighborMac())
                .replaceAll("SOURCENAME", localName)
                .replaceAll("SOURCEMAC", localMacAddress)
                .replaceAll("ID", uuid)
                .replaceAll("START", time)
                .replaceAll("MESSAGE", MESSAGE)
                .replaceAll("ROUTER", currentConnectDevice.getPath());
        return model;
    }

    NeighborInfo currentConnectDevice = null;

    @Override
    public void onItemClickPair(int position) {
        label = "";
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        if(list.size()==0) {
            Toast.makeText(context, "Please finish the scanning", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chatUtils != null && chatUtils.getState() == ChatUtils.STATE_CONNECTED) {
            chatUtils.stop();
        }
        NeighborInfo tmp = list.get(position);
        if (tmp.getHop() == 1){
            label = "";
            isNoneOrShowBtn(1);
            if(pairedDevicesSet.size() == 0){
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                pairedDevicesSet = new ArrayList<>(pairedDevices);
            }
            for (BluetoothDevice device : pairedDevicesSet){
                if(device.getName().equals(tmp.getNeighborName())){
                    chatUtils.connect(device,"directly");
                    currentConnectDevice = tmp;
                    break;
                }
            }
        }else {
            label = "multihop";
            currentConnectDevice = tmp;
            chatUtils.setState(ChatUtils.STATE_NONE);
            isNoneOrShowBtn(2);
            String[] path = RouterTool.routerList(tmp.getPath());
            multiConnection(path[1],"");
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
        initBluetooth();
        init();
        if(chatUtils == null){
            chatUtils = new ChatUtils(context,handler);
        }
        cloudNotification();
    }

    private void cloudNotification() {
        Timer timer_enable = new Timer();
        timer_enable.schedule(new TimerTask() {
            public void run() {
                enable();
            }

        }, 1000, 3000 * 1000);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                scanDevices();
                MessageDB.checkUnSendOrUnreadMessage(localName,localMacAddress);
            }
        }, 1000, 30 * 1000);


        Timer timerViewupdate = new Timer();
        timerViewupdate.schedule(new TimerTask() {
            public void run() {
                if(isupdateView){
                    Message msg = new Message();
                    msg.arg1 = 3;
                    handlerView.sendMessage(msg);
                    isupdateView = false;
                }
                if(isupdateNeiView){
                    Message msg = new Message();
                    msg.arg1 = 2;
                    handlerView.sendMessage(msg);
                    isupdateNeiView = false;
                }
            }
        }, 0, 300);

        //TODO 自动登录 。 测试阶段 主要是确定在同一个MANET

        Timer resendTimer = new Timer();
        resendTimer.schedule(new TimerTask() {
            public void run() {
                LoginTool.funclogin(localName);
                if(isupdateUser || times>=3){
                    List<DeviceInfo> list = LitePal.findAll(DeviceInfo.class);
                    if(list.size()==1){
                        deviceInfo = list.get(0);
                    }
                    isupdateUser = false;
                    resendTimer.cancel();
                }
                times++;
            }
        }, 0, 3 * 1000);


    }
    int times = 0;
    public static boolean isupdateView = false;
    public static boolean isupdateNeiView = false;
    public static boolean isupdateUser = false;


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
                if (!message.isEmpty()) {
                    String uuid = String.valueOf(RandomID.genIDWorker());
                    String msg = combinationRouterPathMultihop(message,uuid);
                    try {
                        String api = APIUrl.APICloudSendUnreadMessage;
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("isUpload", "1");
                        jsonObject.put("msg", msg);
                        jsonObject.put("uploadName", localName);
                        jsonObject.put("isRead", "0");
                        jsonObject.put("uploadMAC", localMacAddress);
                        String uploadTime = TimeParse.stampToDate(TimeParse.getTimestamp());
                        jsonObject.put("uploadTime", uploadTime);
                        RxHttp.postJson(api).addAll(jsonObject.toString())
                                .asClass(Response.class)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(data -> {
                                    if (data.getCode() == 0) {
                                        String com =msg.replaceAll("UPLOAD","1");
                                        edCreateMessage.setText("");
                                        chatUtils.write(com.getBytes());
                                    }
                                }, throwable -> {
                                    String com =msg.replaceAll("UPLOAD","0");
                                    edCreateMessage.setText("");
                                    chatUtils.write(com.getBytes());

                                });
                    } catch (JSONException e) {
                        String com =msg.replaceAll("UPLOAD","0");
                        edCreateMessage.setText("");
                        chatUtils.write(com.getBytes());
                    }
                }
            }
        });


        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(chatUtils.getState() != ChatUtils.STATE_CONNECTED) return;
                if(currentConnectDevice == null){
                    BluetoothDevice current = null;
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    pairedDevicesSet = new ArrayList<>(pairedDevices);
                    for(int i =0;i<pairedDevicesSet.size();i++){
                        if(pairedDevicesSet.get(i).getName().equals(connectedDevice) ){
                            current = pairedDevicesSet.get(i);
                            currentConnectDevice = new NeighborInfo();
                            currentConnectDevice.setHop(1);
                            currentConnectDevice.setNeighborMac(current.getAddress());
                            currentConnectDevice.setNeighborName(current.getName());
                            break;
                        }
                    }
                }
                String message = edCreateMessage.getText().toString().trim();
                String tmp = ProtocolModel.directMsg;
                if (!message.isEmpty()) {
                    edCreateMessage.setText("");
                    label = "";
                    String time = TimeParse.stampToDate(TimeParse.getTimestamp());
                    String TAREGETNAME = currentConnectDevice.getNeighborName() == null ? connectedDevice :  currentConnectDevice.getNeighborName()   ;
                    String TARGETMAC = currentConnectDevice.getNeighborMac();
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
        if (chatUtils != null) {
            if (chatUtils.getState() == ChatUtils.STATE_NONE) {
                chatUtils.start();
            }
        }
        reLoadMessageFromDB();
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
                        chatUtils.connect(device,label);
                        return;

                    }
                }
            });

    //处理界面
    private  Handler handlerView = new Handler(){
        public void handleMessage(android.os.Message msg) {
            if(msg.arg1 == 1){
//                progress_scan_devices.setVisibility(View.VISIBLE);
                Log.e(TAG,"start scan");
            } else if(msg.arg1 == 2){
                progress_scan_devices.setVisibility(View.GONE);
                updateView();
            } else if(msg.arg1 == 3){
                reLoadMessageFromDB();
            }
        };
    };
    private void scanDevices(){
        Message msg = new Message();
        msg.arg1 = 1;
        handlerView.sendMessage(msg);


        allDevices.clear();
        pairedDevicesSet.clear();
        registerReceiver(receiver, makeFilters());
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
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
                Toast.makeText(context, "Clicked enable", Toast.LENGTH_SHORT).show();
                enable();
//                try {
//                    boolean isConnect = NetworkTool.ping(UrlDomain.pingURL,10);
//                    if (isConnect) {
//                        RouterTool.getRouterOwne(localName);
//                        Message msg = new Message();
//                        msg.arg1 = 2;
//                        handlerView.sendMessage(msg);
//                    } else {
//                        noNetworkOnlyGetOneHopNode();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
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
        String title = "device "+info.getUsername() + "'s basic information";
        builder.setTitle(title);
        String body = "uuid: "+ info.getUuid()+"\n"
                +"MANET UUID: "+  info.getManet_UUID()+"\n"
                + "MAC: "+ info.getMac()+"\n"
                +"Login date: "+  info.getLoginDate()+"\n"
                +"Role: "+  info.getRole()+"\n"
               ;
        builder.setMessage(body);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
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

    private void printLog(BluetoothDevice device){
        String s = "";
        int BondState = device.getBondState();
        switch (BondState){
            case BluetoothDevice.BOND_BONDED:
                s ="---name：" + device.getName() + "\n" + "MAC：" + device.getAddress() + "\n" + "state：paired" + "\n";
                break;
            default:
                s ="---name：" + device.getName() + "\n" + "MAC：" + device.getAddress() + "\n" + "state：unknown" + "\n";
                break;
        }
        Log.e(TAG,s);
    }


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
                getPairedDevices();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                int rssi = Math.abs(intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI));
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

                if (name != null) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int BondState = device.getBondState();
                    switch (BondState){
                        case BluetoothDevice.BOND_BONDED:
                            if(!allDevices.containsKey(device.getAddress())){
                                allDevices.put(device.getAddress(),device);
                                pairedDevicesSet.add(device);
                            }
                            break;
                    }
                    printLog(device);
                }
            }
        }



    };

    private void noNetworkOnlyGetOneHopNode(){
        LitePal.deleteAll(NeighborInfo.class);
        for (BluetoothDevice device : pairedDevicesSet){
            String address = device.getAddress();
            NeighborInfo neighbor = new NeighborInfo(address, device.getName(),1,new Date(),"");
            neighbor.save();
        }
//        if(pairedDevicesSet.size() == 0){
//            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//            if(pairedDevices != null && pairedDevices.size()>0){
//                for (BluetoothDevice device : pairedDevices){
//                    String address = device.getAddress();
//                    NeighborInfo neighbor = new NeighborInfo(address, device.getName(),1,new Date(),"");
//                    neighbor.save();
//                }
//            }
//        }else {
//            for (BluetoothDevice device : pairedDevicesSet){
//                String address = device.getAddress();
//                NeighborInfo neighbor = new NeighborInfo(address, device.getName(),1,new Date(),"");
//                neighbor.save();
//            }
//        }
        isupdateNeiView = true;
    }

    private void getPairedDevices(){
        List<NeighborInfo> neighborInfoList = new ArrayList<>();
        for(BluetoothDevice device : pairedDevicesSet) {
            String address = device.getAddress();
            NeighborInfo neighbor = new NeighborInfo(address, device.getName(), 1, new Date(), "");
            neighborInfoList.add(neighbor);
        }
        if(deviceInfo!=null){
            List<DeviceInfo> infos = LitePal.findAll(DeviceInfo.class);
            RouterTool.setDeviceInfo(infos.get(0));
            deviceInfo = infos.get(0);
            String mid = deviceInfo.getManet_UUID();
            if(neighborInfoList.size() != 0){
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
                                    HashMap<String,Object> map = JsonUtils.jsonToPojo(r1,HashMap.class);
                                    String MANET_UUID = map.get("MANET_UUID") == null? "none":map.get("MANET_UUID").toString();
                                    String userInfo = map.get("userInfo") == null? "member":map.get("userInfo").toString();
                                    if(!MANET_UUID.equals("none") || !MANET_UUID.equals("")){
                                        String uuid = deviceInfo.getUuid();
                                        DeviceInfo info = new DeviceInfo();
                                        info.setManet_UUID(MANET_UUID);
                                        info.setRole(userInfo);
                                        info.updateAll("uuid = ?", uuid);
                                    }
                                    String router = map.get("router").toString();
                                    map = JsonUtils.jsonToPojo(router,HashMap.class);
                                    String member = map.get("member").toString();
                                    List<HashMap> memberlist = JsonUtils.jsonToList(member,HashMap.class);
                                    HashMap<String,String> allnodes = new HashMap<>();
                                    for(HashMap tp : memberlist){
                                        String tname = tp.get("sourceName").toString();
                                        String tMAC = tp.get("sourceMAC") == null ? "none": tp.get("sourceMAC").toString() ;
                                        allnodes.put(tname,tMAC);
                                    }

                                    router = map.get("router").toString();
                                    Log.e(TAG,router);
                                    List<HashMap> routerlist = JsonUtils.jsonToList(router,HashMap.class);
                                    LitePal.deleteAll(NeighborInfo.class);
                                    for(HashMap tp : routerlist){
                                        int hop = Integer.parseInt(tp.get("hop").toString());
                                        String tname = tp.get("dest").toString();
                                        String path = tp.get("path").toString();
                                        String tMAC = allnodes.get(tname);
                                        NeighborInfo neighbor = new NeighborInfo(tMAC, tname,hop,new Date(),path);
                                        neighbor.save();
                                    }
                                    isupdateNeiView = true;
                                }else if(data.getCode() == 2) {
                                    String MANET_UUID = data.getData() == null ? "none" : data.getData().toString();
                                    if (!MANET_UUID.equals("none") || !MANET_UUID.equals("")) {
                                        String uuid = deviceInfo.getUuid();
                                        DeviceInfo info = new DeviceInfo();
                                        info.setManet_UUID(MANET_UUID);
                                        info.updateAll("uuid = ?", uuid);
                                    }

                                }
                            }, throwable -> {
                                Log.d(TAG, throwable.toString());
                                noNetworkOnlyGetOneHopNode();
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                    noNetworkOnlyGetOneHopNode();
                }
            }
        }else {
            noNetworkOnlyGetOneHopNode();
        }
    }
}