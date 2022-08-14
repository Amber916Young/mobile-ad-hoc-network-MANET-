package com.yang.myapplication.service;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.yang.myapplication.Activity.ChatMainActivity;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.TimeParse;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    public static Boolean storeMessageEachTime(MessageInfo inputBuffer) {
        return storeMessage(inputBuffer.getUuid(), inputBuffer);
    }

    public static List<MessageInfo> queryAllFromDB(String name1 ,String name2) {
        return LitePal.where("sourceName = ? and targetName = ? or sourceName = ? and targetName = ? ",name1,name2,name2,name1).order("sendDate").find(MessageInfo.class);
    }
    public static List<MessageInfo> queryAllFromDB() {
        return LitePal.order("sendDate").find(MessageInfo.class);
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
            int dataType = Integer.parseInt(String.valueOf(info.get("dataType")));
            MessageInfo message = new MessageInfo(ID, content, START, END, 1, DESNAME, DESMAC, SOURCENAME, SOURCEMAC,1);
            message.setDataType(dataType);
            message.setMessage(content);
            storeMessage(ID, message);
            Calendar calTest = Calendar.getInstance();
            long endTime =  calTest.getTimeInMillis();
            ChatMainActivity.isupdateView = true;
            System.out.println("ACK到达cloud   "+  Math.abs(endTime - ChatUtils.startTime)/1000.0);
        }
    }




    public static void deleteAllMsgDB() {
        LitePal.deleteAll(MessageInfo.class);
    }
}