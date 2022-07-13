package com.yang.myapplication.service;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.yang.myapplication.Activity.BluetoothChat;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.NetworkTool;
import com.yang.myapplication.Tools.ProtocolModel;
import com.yang.myapplication.Tools.TimeParse;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.entity.UnreadMsgDB;
import com.yang.myapplication.http.APIUrl;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

public class MessageDB {
    private static String TAG = "MessageDB";
    public static DeviceInfo getDeviceInfo(){
        List<DeviceInfo> deviceInfos = LitePal.findAll(DeviceInfo.class);
        DeviceInfo deviceInfo = null;
        if(deviceInfos.size() == 1){
            deviceInfo = deviceInfos.get(0);
        }
        return deviceInfo;
    }

    public static Boolean storeMessage(String uuid, String END) {
        List<MessageInfo> exists = LitePal.where("uuid = ?", uuid).find(MessageInfo.class);
        if (exists.size() > 1) {
            Log.e(TAG, "出错出错，怎么会uuid不是唯一的？");
            return false;
        }else {
            MessageInfo messageInfo = exists.get(0);
            messageInfo.setIsRead(1);
            messageInfo.setReadDate(END);
            messageInfo.updateAll("uuid = ?", uuid);
        }
        return true;
    }
    public static Boolean storeMessage(String uuid, MessageInfo message) {
    List<MessageInfo> exists = LitePal.where("uuid = ?", uuid).find(MessageInfo.class);
    if (exists.size() > 1) {
        Log.e(TAG, "出错出错，怎么会uuid不是唯一的？");
        return false;
    }
    if (exists.size() == 0) {
        message.save();
    }
    if (exists.size() == 1) {
        if(exists.get(0).getIsRead() == 1) return true;
        LitePal.deleteAll(MessageInfo.class, "uuid = ?", uuid);
        message.save();
    }
    return true;
}
    public static Boolean storeMessage(String inputBuffer) {
        String[] strings = inputBuffer.split("@");
        String ID = strings[0];
        String MESSAGE = strings[1];
        String START = strings[2];
        String END = "END";
        String DESNAME = strings[4];
        String DESMAC = strings[5];
        String SOURCENAME = strings[6];
        String SOURCEMAC = strings[7];
        MessageInfo message = new MessageInfo(ID, MESSAGE, START, END, 0, DESNAME, DESMAC, SOURCENAME, SOURCEMAC);
        return storeMessage(ID, message);
    }

    public static String FormatMessage(int type,String inputBuffer,String ACK){
        String format ="";
        if(type == 1){
            format = ProtocolModel.directMsg;
            String[] tmp = inputBuffer.split("&");
            String MESSAGE = tmp[0];
            String SOURCENAME = tmp[1];
            String SOURCMAC = tmp[2];
            String TAREGETNAME = tmp[3];
            String TARGETMAC = tmp[4];
            String START = tmp[5];
            String ID = tmp[8];
            String time = TimeParse.stampToDate(TimeParse.getTimestamp());
            format = format.replaceAll("MESSAGE", MESSAGE).replaceAll("SOURCENAME", SOURCENAME)
                    .replaceAll("SOURCMAC", SOURCMAC).replaceAll("TAREGETNAME", TAREGETNAME).replaceAll("TARGETMAC", TARGETMAC)
                    .replaceAll("START", START).replaceAll("END", time)
                    .replaceAll("ACK", ACK).replaceAll("ID", ID);

        }

        return format;
    }

    public static Boolean storeMessageEachTime(String inputBuffer, int isRead) {
        Log.d("storeMessageEachTime",inputBuffer);
        inputBuffer = inputBuffer.replaceAll("ACK", String.valueOf(isRead));
        String[] tmp = inputBuffer.split("&");
        String MESSAGE = tmp[0];
        String SOURCENAME = tmp[1];
        String SOURCMAC = tmp[2];
        String TAREGETNAME = tmp[3];
        String TARGETMAC = tmp[4];
        String START = tmp[5];
        String END = tmp[6];
        String ID = tmp[8];
        MessageInfo message = new MessageInfo(ID, MESSAGE, START, END, isRead, TAREGETNAME, TARGETMAC, SOURCENAME, SOURCMAC);
        return storeMessage(ID, message);
    }

    public static List<MessageInfo> queryAllFromDB(String sourceName) {
        List<MessageInfo> list = new ArrayList<>();
        if(sourceName == null){
            list = LitePal.order("sendDate").find(MessageInfo.class);

        }else {
            list = LitePal.where("sourceName = ? or targetName = ?",sourceName,sourceName).order("sendDate").find(MessageInfo.class);
        }
        return list;
    }

    public static void TestMethos(String ID,String end,String type){
        String api = APIUrl.APIUpdateMessage;
        JSONObject jsonObject = new JSONObject();
        if(type.equals("DELETE")){
            api = APIUrl.APIDeleteMessage;
        }
        try {
            jsonObject.put("uuid", ID);
            jsonObject.put("readDate", end);
            jsonObject.put("isRead", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RxHttp.postJson(api).addAll(jsonObject.toString())
                .asClass(Response.class)
                .subscribe(data -> {
                    if (data.getCode() == 0) {
                        Log.d(TAG, "三次确认");
                    }else
                        Log.d(TAG, data.getMsg());
                }, throwable -> {
                    Log.d(TAG, throwable.toString());
                });
    }


    public static void checkUnSendMessageMulti(String ID,String end) {
        String api = APIUrl.APISendUnreadMessageTwice;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", ID);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .subscribe(data -> {
                        if (data.getCode() == 0) {
                            Log.d(TAG, "二次确认");
                        }
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void checkUnSendOrUnreadMessage(String sendname, String mac) {
        try {
            JSONObject jsonObject = new JSONObject();
            String api = APIUrl.APISendUnreadMessage;
            List<MessageInfo> list = LitePal.where("isRead = ? and isUpload = ? ", "0","0").find(MessageInfo.class);
            if (list.size() == 0 ) {
                JSONArray jsonArray = new JSONArray(Collections.singletonList(list));
                jsonObject.put("messageList", jsonArray);
            }
            jsonObject.put("uploadName", sendname);
            jsonObject.put("uploadMAC", mac);
            String uploadTime = TimeParse.stampToDate(TimeParse.getTimestamp());
            jsonObject.put("uploadTime", uploadTime);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(data -> {
                        if (data.getCode() == 0) {
                            System.out.println("请求请求请求请求请求");
                            if(list.size()!=0){
                                updateMessageInfoDB(list);
                            }
                            if(data.getData() != null){
                                String r1 = data.getData().toString();
                                HashMap<String,String> map = JsonUtils.jsonToPojo(r1,HashMap.class);
                                if(map.containsKey("mine")){
                                    String mine = map.get("mine");
                                    List<HashMap> mapList= JsonUtils.jsonToList(mine,HashMap.class);
                                    updateMessageInfoDB2(mapList);
                                }
                                if(map.containsKey("other")){
                                    String other = map.get("other");
                                    List<HashMap> mapList= JsonUtils.jsonToList(other,HashMap.class);
                                    updateMessageInfoDB2(mapList);
                                }
                            }
                        }
                        Log.d(TAG, data.getMsg());
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void updateMessageInfoDB(List<MessageInfo> list) {
        for (MessageInfo info : list) {
            info.setIsUpload(1);
            info.updateAll("uuid = ?", info.getUuid());
        }
        BluetoothChat.isupdateView = true;
    }


    private static void updateMessageInfoDB2(List<HashMap> list) {
        for (HashMap info : list) {
            String ID =  String.valueOf(info.get("uuid"));
            String START = String.valueOf(info.get("sendDate"));
            String content = String.valueOf(info.get("content"));
            String END = String.valueOf(info.get("readDate"));
            String DESNAME = String.valueOf(info.get("targetName"));
            String DESMAC = String.valueOf(info.get("targetMAC"));
            String SOURCENAME = String.valueOf(info.get("sourceName"));
            String SOURCEMAC = String.valueOf(info.get("sourceMAC"));
            MessageInfo message = new MessageInfo(ID, content, START, END, 1, DESNAME, DESMAC, SOURCENAME, SOURCEMAC,1);
            storeMessage(ID, message);
        }
        BluetoothChat.isupdateView = true;
    }



    public static  List<MessageInfo> queryAllUnreadMsgDB(String isread, int type) {
        List<MessageInfo> list = LitePal.where("isRead = 0").find(MessageInfo.class);
        System.err.println("queryAllUnreadMsgDB============="+type+" size "+ list.size());
        return  list;
    }

    private static void updateMessageInfo(HashMap<String, Object> map){
        MessageInfo info  = new MessageInfo();
        if(map.containsKey("readDate")){
            info.setReadDate(map.get("readDate").toString());
        }
        if(map.containsKey("isRead")){
            info.setIsRead(Integer.parseInt(map.get("isRead").toString()));
        }
        if(map.containsKey("uuid")){
            info.updateAll("uuid = ?", String.valueOf(map.get("uuid")));
        }
    }

    public static String ownerName = "Nexus 5";

    public static Boolean updateMessageByMANETResend(String uuid, MessageInfo message) {
        List<MessageInfo> exists = LitePal.where("uuid = ?", uuid).find(MessageInfo.class);
        if(exists.size() > 1) return false;
        if (exists.size() == 1) {
            message.updateAll("uuid = ?", uuid);
        }
        if(exists.size()==0){
            Log.d(TAG,"不存在该条消息，可能您已经删除");
            return false;
        }
        return true;
    }

    public static void deleteAllMsgDB() {
        LitePal.deleteAll(MessageInfo.class);
    }
}