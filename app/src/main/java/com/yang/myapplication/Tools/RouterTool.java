package com.yang.myapplication.Tools;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.yang.myapplication.Activity.BluetoothChat;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rxhttp.RxHttp;

public class RouterTool {
    private static DeviceInfo deviceInfo = null;
    private static String MANET_UUID = null;
    private static String Param_MANET_UUID = "MANET_UUID";
    private static String Param_sourceName = "sourceName";
    private static String Param_sourceMAC = "sourceMAC";
    private static String Param_source = "source";

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
                           // 目前逻辑是，上传邻居节点，等形成graph
                            flag_stoploop = false;
                            getCloudRouterOwn(name);
                        }
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
   static Boolean flag_stoploop = false;
    //gain its own router all
    private static void getCloudRouterOwn(String name){
        Timer resendTimer = new Timer();
        resendTimer.schedule(new TimerTask() {
            public void run() {
                if(flag_stoploop) resendTimer.cancel();
                getRouterOwne(name);
            }
        }, 0,  20*1000);
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
                            flag_stoploop = true;
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

                            /* *
                             *     private int id;
                             *     private String source;
                             *     private String dest;
                             *     private String path;
                             *     private String hop;
                             *
                             * {
                                  "router": [
                                    {
                                      "id": 49,
                                      "source": "Nexus 5",
                                      "dest": "Galaxy A7 (2018)",
                                      "path": "[Galaxy A7 (2018)]",
                                      "hop": "1"
                                    }
                                  ],
                                  "member": [
                                    {
                                      "id": 23,
                                      "sourceName": "Galaxy A7 (2018)",
                                      "sourceMAC": "CC:21:19:F3:4C:65",
                                      "desName": "Nexus 5",
                                      "desMAC": "2C:54:CF:71:D7:71",
                                      "manet_UUID": "AQ2049425156H"
                                    },
                                    {
                                      "id": 27,
                                      "sourceName": "Nexus 5",
                                      "sourceMAC": "2C:54:CF:E3:C4:1D",
                                      "desName": "Galaxy A7 (2018)",
                                      "desMAC": "CC:21:19:F3:4C:64",
                                      "manet_UUID": "AQ2049425156H"
                                    }
                                  ]
                                }
                             * */
                            String router = map.get("router").toString();
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
                        }
                    }, throwable -> {
                        Log.d(TAG, throwable.toString());
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    static HashMap<String,String> MANET_device_name_address = new HashMap<>();



}
