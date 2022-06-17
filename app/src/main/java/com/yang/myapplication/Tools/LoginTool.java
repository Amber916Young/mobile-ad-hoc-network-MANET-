package com.yang.myapplication.Tools;

import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.HashMap;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

public class LoginTool {
    private static DeviceInfo deviceInfo = null;
    private static String username = null;
//   .observeOn(AndroidSchedulers.mainThread())
    public static void funclogin(String name) {
        String api = APIUrl.APIlogin;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RxHttp.postJson(api).addAll(jsonObject.toString())
                .asClass(Response.class)
                .subscribe(data -> {
                    if (data.getCode() == 0) {
                        String r1 = data.getData().toString();
                        HashMap<String,Object> map = JsonUtils.jsonToPojo(r1,HashMap.class);
                        assert map != null;
                        String uuid = Objects.requireNonNull(map.get("uuid")).toString();
                        username = Objects.requireNonNull(map.get("username")).toString();
                        String password = Objects.requireNonNull(map.get("password")).toString();
                        String MAC = Objects.requireNonNull(map.get("mac")).toString();
                        String loginDate = Objects.requireNonNull(map.get("loginDate")).toString();
                        String registerDate = Objects.requireNonNull(map.get("registerDate")).toString();
                        String status = Objects.requireNonNull(map.get("status")).toString();
                        String role = Objects.requireNonNull(map.get("role")).toString();
                        String MANET_UUID = Objects.requireNonNull(map.get("manet_UUID")).toString();
                        deviceInfo = new DeviceInfo(uuid,username,password,MAC,loginDate,registerDate,status,MANET_UUID,role);
                        LitePal.deleteAll(DeviceInfo.class);
                        deviceInfo.save();

//                            loadCloudDevice();
                    }
                }, throwable -> {


                });
    }
}
