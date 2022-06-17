package com.yang.myapplication.service;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
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

    public static Response storeMessageMultiHop(String inputBuffer, String localName) {
        String multi_hop_model = "MESSAGE@NEIGHBORMAC@NEIGHBORNAME@SOURCENAME@LASTNAME@HOP@START@END@ACK@ID@ORGINAL@SOURCEMAC";

        String[] strings = inputBuffer.split("@");
        if(strings.length<=8)
            return new Response(-1, "", "");

        String MESSAGE = strings[0];
        String NEIGHBORMAC = strings[1];
        String NEIGHBORNAME = strings[2];
        String SOURCENAME = strings[3];
        int HOP = Integer.parseInt(strings[5]);
        String start = strings[6];
        String ACK = strings[8];
        String ID = strings[9];
        String ORGINAL = strings[10];
        String SOURCEMAC = strings[11];
        String end = strings[7];
        multi_hop_model = multi_hop_model.replaceAll("SOURCEMAC", SOURCEMAC)
                .replaceAll("ORGINAL", ORGINAL).replaceAll("ID", ID)
                .replaceAll("START", start).replaceAll("MESSAGE", MESSAGE)
                .replaceAll("LASTNAME", localName).replaceAll("SOURCENAME", SOURCENAME)
                .replaceAll("NEIGHBORNAME", NEIGHBORNAME).replaceAll("NEIGHBORMAC", NEIGHBORMAC);

        if (ACK.equals("1") && HOP != 1) {
            HOP = HOP - 1;
            multi_hop_model = multi_hop_model.replaceAll("END", end).replaceAll("ACK", "1").replaceAll("HOP", String.valueOf(HOP));
            return new Response(3, "", multi_hop_model);
        }
        if (ACK.equals("1")) {
            multi_hop_model = multi_hop_model.replaceAll("ACK", ACK).replaceAll("HOP", String.valueOf(HOP));
            if (!end.equals("END"))
                multi_hop_model = multi_hop_model.replaceAll("END", end);

            return new Response(4, "", multi_hop_model);
        }

        if (HOP == 1) {
            end = TimeParse.stampToDate(TimeParse.getTimestamp());
            MessageInfo message = new MessageInfo(ID, MESSAGE, start, end, 1, NEIGHBORNAME, NEIGHBORMAC, SOURCENAME, SOURCEMAC);
            multi_hop_model = multi_hop_model.replaceAll("END", end).replaceAll("ACK", "1").replaceAll("HOP", String.valueOf(HOP));
            return storeMessage(ID, message) ? new Response(2, "", multi_hop_model) : new Response(-1, "", null);
        } else {
            HOP = HOP - 1;
            multi_hop_model = multi_hop_model.replaceAll("ACK", "0").replaceAll("HOP", String.valueOf(HOP));
            return new Response(1, "", multi_hop_model);
        }
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

    public static Boolean storeMessageTheFirstHop(String inputBuffer, int isRead) {
        String[] strings = inputBuffer.split("@");
        String MESSAGE = strings[0];
        String NEIGHBORMAC = strings[1];
        String NEIGHBORNAME = strings[2];
        String SOURCENAME = strings[3];
        String start = strings[6];
        String ID = strings[9];
        String SOURCEMAC = strings[11];
        String end = "END";
        MessageInfo message = new MessageInfo(ID, MESSAGE, start, end, isRead, NEIGHBORNAME, NEIGHBORMAC, SOURCENAME, SOURCEMAC);
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
        Log.d(TAG,"==========storeMessageEachTime==============");
        Log.d(TAG,inputBuffer);
        Log.d(TAG,"==========storeMessageEachTime==============");

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

    public synchronized static void CheckMSGFromCloud(String localName, String connectedDevice,String flag, ChatUtils chatUtils, String localMacAddress) {
        String api = APIUrl.APIqueryFromCloud;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("localName", localName);
            jsonObject.put("connectedDevice", connectedDevice);
            jsonObject.put("flag", flag);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RxHttp.postJson(api).addAll(jsonObject.toString())
                .asClass(Response.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (data.getCode() == 0) {
                        String r1 = data.getData().toString();
                        if (!r1.equals("") && !r1.equals("[]")) {
                            List<HashMap>  unreadMap = JsonUtils.jsonToList(r1, HashMap.class);
                            for(int i =0 ;i<unreadMap.size();i++){
                                String reSend = cloudunreadMSGWHENConnectAgain(unreadMap.get(i),flag,localMacAddress);
                                System.err.println("reSend==="+reSend);
                                chatUtils.write(reSend.getBytes());
                                Thread.sleep(500);
                            }
                        }
                    }
                }, throwable -> {
                    Log.d(TAG, throwable.toString());
                });
    }
    private static String cloudunreadMSGWHENConnectAgain(HashMap map,String flag ,String localMacAddress ){
        String tmp = "MESSAGE&SOURCENAME&SOURCMAC&TAREGETNAME&TARGETMAC&START&END&ID&ORGINAL&FLAG";
        String MESSAGE = "";
        String SOURCENAME = "";
        String SOURCMAC = "";
        String TAREGETNAME = "";
        String TARGETMAC = "";
        String START = "";
        String END = "";
        String ORGINAL = "";
        String FLAG = "";
        String ID = "";
        if(flag.equals("Sourceunread")){
            ID = map.get("uuid").toString();
            MESSAGE = map.get("content").toString();
            SOURCENAME = map.get("sourceName").toString();
            SOURCMAC = localMacAddress;
            TAREGETNAME = map.get("targetName").toString();
            TARGETMAC = map.get("targetMAC").toString();
            START = map.get("sendDate").toString();
            END = map.get("readDate").toString();
            ORGINAL = map.get("sourceMAC").toString();
            FLAG = "FLAG=SO";

            tmp = tmp.replaceAll("END", END)
                    .replaceAll("MESSAGE", MESSAGE)
                    .replaceAll("SOURCENAME", SOURCENAME)
                    .replaceAll("SOURCMAC", SOURCMAC)
                    .replaceAll("TAREGETNAME", TAREGETNAME)
                    .replaceAll("TARGETMAC", TARGETMAC)
                    .replaceAll("START", START)
                    .replaceAll("FLAG", FLAG)
                    .replaceAll("ORGINAL", ORGINAL).replaceAll("ID", ID);
        }else {
            ID = map.get("uuid").toString();
            MESSAGE = map.get("content").toString();
            SOURCENAME = map.get("sourceName").toString();
            SOURCMAC = localMacAddress;
            TAREGETNAME = map.get("targetName").toString();
            TARGETMAC = map.get("targetMAC").toString();
            START = map.get("sendDate").toString();
            END = "END";
            ORGINAL = map.get("sourceMAC").toString();
            FLAG = "FLAG=TA";
            tmp = tmp.replaceAll("END", END)
                    .replaceAll("MESSAGE", MESSAGE)
                    .replaceAll("SOURCENAME", SOURCENAME)
                    .replaceAll("SOURCMAC", SOURCMAC)
                    .replaceAll("TAREGETNAME", TAREGETNAME)
                    .replaceAll("TARGETMAC", TARGETMAC)
                    .replaceAll("START", START)
                    .replaceAll("FLAG", FLAG)
                    .replaceAll("ORGINAL", ORGINAL).replaceAll("ID", ID);
        }

        return tmp;
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


    public static Response checkUnSendMessageMulti(String ID,String end) {
        String api = APIUrl.APISendUnreadMessageTwice;
        JSONObject jsonObject = new JSONObject();
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
                        Log.d(TAG, "二次确认");
                    }
                }, throwable -> {
                    Log.d(TAG, throwable.toString());
                });

        return new Response(0, "", null);

    }
    public static Response checkUnSendMessage(String sendname, String mac) {
        try {
            List<MessageInfo> list = LitePal.where("isRead = ? and isUpload = ?", "0", "0").find(MessageInfo.class);
            JSONArray jsonArray = new JSONArray(Collections.singletonList(list));
            if (list.size() == 0) {
                return new Response(-1, "", null);
            }
            if (!isConnect()) {
                return new Response(2, "unable to connect to the cloud", jsonArray);
            }
            String api = APIUrl.APISendUnreadMessage;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("messageList", jsonArray);
            jsonObject.put("uploadName", sendname);
            jsonObject.put("uploadMAC", mac);
            String uploadTime = TimeParse.stampToDate(TimeParse.getTimestamp());
            jsonObject.put("uploadTime", uploadTime);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(data -> {
                        if (data.getCode() == 0) {
                            updateMessageInfoDB(list, 1);
                        }
                        Log.d(TAG, data.getMsg());
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
            return new Response(0, "", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new Response(-1, "", null);
    }

    private static void updateMessageInfoDB(List<MessageInfo> list, int flag) {
        if (flag == 1) {
            for (MessageInfo info : list) {
                info.setIsUpload(1);
                info.updateAll("uuid = ?", info.getUuid());
            }
        } else if (flag == 2) {
            for (MessageInfo info : list) {
                info.setReadDate(info.getReadDate());
                info.setIsRead(1);
                info.updateAll("uuid = ?", info.getUuid());
            }
        }

    }


    public static Boolean isConnect() {
        //test!!!!!!!!!!
        return true;
//        try {
//
//            if (NetworkTool.ping("google.com", 10)) {
//                return true;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
    }

    public static void sendUnreadToCloud(String sendname, String mac, String msg) {
        try {
            String api = APIUrl.APISendUnreadMessage;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("messageList", msg);
            jsonObject.put("uploadName", sendname);
            jsonObject.put("uploadMAC", mac);
            String uploadTime = TimeParse.stampToDate(TimeParse.getTimestamp());

            jsonObject.put("uploadTime",uploadTime);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .subscribe(data -> {
                        if (data.getCode() == 0) {

                        }
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
    //unreadList only the owner can use
    static List<UnreadMsgDB> unreadOwner = new ArrayList<>();





    public static String ownerName = "Nexus 5";

    public static Response updatecloudMessage(String inputBuffer, String localName) {
        String model = ProtocolModel.cloudMultihop;
        String[] strings = inputBuffer.split("@");
        String DESTMAC = strings[0];
        String DESTNAME = strings[1];
        String SOURCENAME = strings[2];
        String SOURCEMAC = strings[3];
        String LASTNAME = strings[4];
        String END = strings[5];
        String ACK = strings[6];
        String uuid = strings[7];
        String TYPE = strings[8];
        inputBuffer = inputBuffer.replaceAll(LASTNAME,localName);
        if (localName.equals(DESTNAME)) {
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setIsRead(1);
            messageInfo.setReadDate(END);
            return updateMessageByMANETResend(uuid, messageInfo) ? new Response(0, "", null) : new Response(-1, "update fail", null);
        }
        //upload to cloud if the node is the owner , test stage!!!!!!!!!!!
        if (localName.equals(ownerName)) {
            String api = APIUrl.APIUpdateMessage;
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("uuid", uuid);
                jsonObject.put("isRead", "1");
                jsonObject.put("readDate", END);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .subscribe(data -> {
                        Log.d(TAG, data.toString());
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        }
        return  new Response(1, "", inputBuffer);
    }

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