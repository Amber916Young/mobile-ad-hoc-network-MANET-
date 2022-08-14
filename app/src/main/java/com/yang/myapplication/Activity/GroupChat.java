package com.yang.myapplication.Activity;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import static com.yang.myapplication.Activity.ChatMainActivity.isupdateView;
import static com.yang.myapplication.entity.MessageInfo.DATA_IMAGE;
import static com.yang.myapplication.entity.MessageInfo.DATA_TEXT;
import static com.yang.myapplication.entity.MessageInfo.DATA_AUDIO;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_WRITE_TEXT;

import androidx.annotation.NonNull;

import com.yang.myapplication.R;
import com.yang.myapplication.Tools.BluetoothTools;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.RouterTool;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.litepal.LitePal;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class GroupChat {
    public static List<ChatUtils> chatSockets = new ArrayList<>();
    List<NeighborInfo> userNames;
    HashMap<String, Boolean> deviceConnections ;
    String groupId = "";
    String groupUserNames = "";
    private final static String TAG = "GroupChat";
    Handler handler = null;
    public void newAdd(NeighborInfo user ,Handler handler){
        if(deviceConnections.containsKey(user.getNeighborMac()) && deviceConnections.get(user.getNeighborMac())){
            return;
        }
        String mac = user.getNeighborMac();
        String name = user.getNeighborName();
        this.handler = handler;
        ChatUtils chatService = new ChatUtils(handler);
        deviceConnections.put(mac, false);
        groupId += mac + "\n";
        groupUserNames += name + "\n";
        if (chatService.getState() == ChatUtils.STATE_NONE) {
            chatService.setMAC(mac);
            chatService.setName(name);
            chatService.start();
        }
        chatSockets.add(chatService);
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void newAdd(NeighborInfo user){
        if(deviceConnections.containsKey(user.getNeighborMac()) && deviceConnections.get(user.getNeighborMac())){
            return;
        }
        String mac = user.getNeighborMac();
        String name = user.getNeighborName();
        if(handler == null) {
            handler = getHandler();
        }
        ChatUtils chatService = new ChatUtils(handler);
        deviceConnections.put(mac, false);
        groupId += mac + "\n";
        groupUserNames += name + "\n";
        if (chatService.getState() == ChatUtils.STATE_NONE) {
            chatService.setMAC(mac);
            chatService.setName(name);
            chatService.start();
        }
        chatSockets.add(chatService);
    }

    public ChatUtils newAdd(BluetoothDevice user ){
        String mac = user.getAddress();
        String name = user.getName();
        ChatUtils chatService = new ChatUtils(handler);
        deviceConnections.put(mac, false);
        groupId += mac + "\n";
        groupUserNames += name + "\n";
        if (chatService.getState() == ChatUtils.STATE_NONE) {
            chatService.setMAC(mac);
            chatService.setName(name);
            chatService.start();
        }
        chatSockets.add(chatService);
        return chatService;
    }



    public GroupChat(List<NeighborInfo> users, Handler handler) {
        userNames = users;
        deviceConnections = new HashMap<>();
        this.handler = handler;
        Set<String> seen = new HashSet<>();
        for (NeighborInfo user : users) {
            String mac = user.getNeighborMac();
            String name = user.getNeighborName();
            if(seen.contains(mac)){
                continue;
            }else {
                seen.add(mac);
            }
            boolean flag = true;
            for (ChatUtils utils : chatSockets){
                if(mac.equals(utils.getMAC())){
                    flag = false;
                    break;
                }
            }
            if(!flag){
                continue;
            }
            ChatUtils chatService = new ChatUtils(handler);
            deviceConnections.put(mac, false);
            groupId += mac + "\n";
            groupUserNames += name + "\n";
            if (chatService.getState() == ChatUtils.STATE_NONE) {
                chatService.setMAC(mac);
                chatService.setName(name);
                chatService.start();
            }
            chatSockets.add(chatService);
        }
    }


    public boolean isConnectedToAll() {
        for (Boolean isConnected : deviceConnections.values()) {
            if (!isConnected) {
                return false;
            }
        }
        return true;
    }

    // Each user will attempt to connect to every other user
    // Find open socket and find user you are not connected to
    List<NeighborInfo> membership = null;
    public void startConnection( List<NeighborInfo> list) {
        this.membership = list;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (int i = 0; i < chatSockets.size(); i++) {
            ChatUtils socket = chatSockets.get(i);
            if (socket.getState() == ChatUtils.STATE_CONNECTED) {
                if(list.size()>1){
//                    socket.write(list);
                }
                continue;
            }else {
                String mac = socket.getMAC();
                BluetoothDevice device = adapter.getRemoteDevice(mac);
                setConnectionAsTrue(mac);
                reConnect3Times(socket,device);
            }
        }
    }


    public synchronized void startConnection( List<NeighborInfo> list,String name ) {
        System.out.println("重连");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.membership = list;
        for (ChatUtils socket : chatSockets) {
            if (socket.getName().equals(name)) {
                if (socket.getState() != ChatUtils.STATE_CONNECTED) {
                    String mac = socket.getMAC();
                    BluetoothDevice device = adapter.getRemoteDevice(mac);
                    reConnect3Times(socket, device);
                }else {
                    BluetoothChat.isupdateNeiView = true;
                }
                break;
            }
        }
    }



        //consider not all devices can connect at the same time,
    // the timer will check the state of each device;
    int timesR = 0;
    private void reConnect3Times(ChatUtils socket,BluetoothDevice device){
        timesR = 0;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                if(timesR>3 || socket.getState() == ChatUtils.STATE_CONNECTED ) {
                    if (socket.getState() == ChatUtils.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to " + device.getName());
                        if(membership!=null && membership.size()>1){
//                            socket.write(membership);
                        }
                    }
                    timesR = 0;
                    timer.cancel();
                }else {
                    socket.connect(device, "multi");
                    timesR++;
                }
            }
        }, 200, 5000);
    }
    int times = 0;
    private void reConnect3TimesMiddle(ChatUtils socket, BluetoothDevice device, MessageInfo message, int dataType) {
        times = 0;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                if (times > 3 || socket.getState() == ChatUtils.STATE_CONNECTED) {
                    if (socket.getState() == ChatUtils.STATE_CONNECTED) {
                        if(message!=null ){
                            System.out.println(message.getMessage());
                            System.out.println(dataType);

                            socket.write(message, dataType);
                        }
                    }
                    times = 0;
                    timer.cancel();
                } else {
                    socket.connect(device, "multi");
                    times++;
                }
            }
        }, 200, 5000);
    }


    // Once you connect to a specific bluetooth device,
    // do not try connecting to it again
    public void setConnectionAsTrue(String macAddress) {
        deviceConnections.put(macAddress, true);
    }


    public void sendMessage(byte[] message, int messageType, NeighborInfo currentConnectDevice, String localName, String localMacAddress, int isUpload, String txtWriteTime) {
        String timeSent = Integer.toString(Calendar.getInstance().get(Calendar.MILLISECOND));
        Log.d(TAG, "Time sent: " + timeSent);
        for (ChatUtils service: chatSockets) {
            if (service.getState() == ChatUtils.STATE_CONNECTED){
                if (service.getMAC().equals(currentConnectDevice.getNeighborMac())) {
                    service.write(message, messageType, timeSent,currentConnectDevice,localName,localMacAddress,isUpload,txtWriteTime);
                    break;
                }
            }
        }
    }

    public void stop() {
        for (ChatUtils userService : chatSockets) {
            userService.stop();
        }
    }

    public BluetoothDevice getMAC( String nextConnect, Set<BluetoothDevice>  list){
        for(BluetoothDevice bluetoothDevice : list){
            if (nextConnect.equals(bluetoothDevice.getName())){
                return bluetoothDevice;
            }
        }
        return null;
    }
    final static int IsConnect = 1;
    final static int NoneSocket = -1;
    final static int disConnect = 2;
    public static class ResultAJAX{
        int code;
        ChatUtils data;
        public ResultAJAX(int errcode, ChatUtils data) {
            this.code = errcode;
            this.data = data;
        }
        public ResultAJAX(int errcode ) {
            this.code = errcode;
        }
        public static ResultAJAX ResultSuccess( ChatUtils data){
            return new ResultAJAX(IsConnect,data);
        }
        public static ResultAJAX ResultError(){
            return new ResultAJAX(NoneSocket);
        }
        public static ResultAJAX ResultWarn( ChatUtils data ){
            return new ResultAJAX(disConnect,data);
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public ChatUtils getData() {
            return data;
        }

        public void setData(ChatUtils data) {
            this.data = data;
        }
    }

    public ResultAJAX isHaveInSockets(String mac){
        for (ChatUtils service: chatSockets) {
            if(service.getMAC().equals(mac)) {
                if (service.getState() == ChatUtils.STATE_CONNECTED) {
                    return ResultAJAX.ResultSuccess(service);
                }
                return ResultAJAX.ResultWarn(service);
            }
        }
        return ResultAJAX.ResultError();
    }

    public void sendMessage(MessageInfo message, int dataType, String nextConnect, Set<BluetoothDevice> list) {
        BluetoothDevice device = getMAC(nextConnect, list);
        String mac = device.getAddress();
        if (mac == null) return;
        ResultAJAX res = isHaveInSockets(mac);
        if (res.getCode() == IsConnect) {
            ChatUtils service = res.getData();
            service.write(message, dataType);
        } else if (res.getCode() == disConnect) {
            ChatUtils service = res.getData();
            reConnect3TimesMiddle(service, device, message, dataType);
        } else if (res.getCode() == NoneSocket) {
            ChatUtils service = newAdd(device);
            reConnect3TimesMiddle(service, device, message, dataType);
        }
    }

    public void sendMessage(byte[] message, int messageType, NeighborInfo currentConnectDevice, String localName, String localMacAddress, int isUpload, String txtWriteTime, String nextrouters) {
        String timeSent = Integer.toString(Calendar.getInstance().get(Calendar.MILLISECOND));
        Log.d(TAG, "Time sent: " + timeSent);
        for (ChatUtils service: chatSockets) {
            if (service.getState() == ChatUtils.STATE_CONNECTED){
                if (service.getName().equals(nextrouters)) {
                    service.write(message, messageType, timeSent,currentConnectDevice,localName,localMacAddress,isUpload,txtWriteTime);
                    break;
                }
            }
        }
    }


}
