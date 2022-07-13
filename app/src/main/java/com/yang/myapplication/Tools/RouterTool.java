package com.yang.myapplication.Tools;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.yang.myapplication.Activity.BluetoothChat;
import com.yang.myapplication.Activity.DeviceListActivity;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

public class RouterTool {
    private static DeviceInfo deviceInfo = null;
    private static String MANET_UUID = null;
    private static String Param_MANET_UUID = "MANET_UUID";
    private static String Param_sourceName = "sourceName";
    private static String Param_sourceMAC = "sourceMAC";
    private static String Param_source = "source";
    private static String Param_items = "items";
    private static String Param_self = "own";
    private static String Param_type = "type";

    private static final String TAG = "RouterTool";
    public static void setDeviceInfo(DeviceInfo desvice) {
        deviceInfo = desvice;
        MANET_UUID = deviceInfo.getManet_UUID();
    }

    public static void uploadRouterNeighbor( List<NeighborInfo> neighbors,String name,String MAC){
        try {
            String api = APIUrl.uploadRouterNeighbor;
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray(Collections.singletonList(neighbors));
            jsonObject.put(Param_MANET_UUID, MANET_UUID);
            jsonObject.put(Param_sourceName, name);
            jsonObject.put(Param_sourceMAC, MAC);
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

                            BluetoothChat.isupdateNeiView = true;
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
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public static void getRouterOwne(String name){
        try {
            String api = APIUrl.getEachRouter;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Param_MANET_UUID, MANET_UUID);
            jsonObject.put(Param_source, name);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .subscribe(data -> {
                        if (data.getCode() == 0) {
                            LitePal.deleteAll(NeighborInfo.class);
                            String r1 = data.getData().toString();
                            HashMap<String,Object> map = JsonUtils.jsonToPojo(r1,HashMap.class);
                            String member = map.get("member").toString();
                            List<HashMap> memberlist = JsonUtils.jsonToList(member,HashMap.class);
                            HashMap<String,String> allnodes = new HashMap<>();
                            for(HashMap tp : memberlist){
                                String tname = tp.get("sourceName").toString();
                                String tMAC = tp.get("sourceMAC").toString();
                                allnodes.put(tname,tMAC);
                            }
                            String router = map.get("router").toString();
                            List<HashMap> routerlist = JsonUtils.jsonToList(router,HashMap.class);
                            for(HashMap tp : routerlist){
                                int hop = Integer.parseInt(tp.get("hop").toString());
                                String tname = tp.get("dest").toString();
                                String path = tp.get("path").toString();
                                String tMAC = allnodes.get(tname);
                                NeighborInfo neighbor = new NeighborInfo(tMAC, tname,hop,new Date(),path);
                                neighbor.save();
                            }
                        }
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }



    }

    public static String[] routerList(String routerPath){
        return  routerPath.replaceAll("]","").replaceAll("\\[","").trim().split(",");
    }

}
