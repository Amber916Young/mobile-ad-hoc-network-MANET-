package com.yang.myapplication.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yang.myapplication.Adapter.DeviceCloudAdapter;
import com.yang.myapplication.Adapter.NeighborAdapter;
import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.LoginTool;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;

import org.angmarch.views.NiceSpinner;
import org.angmarch.views.OnSpinnerItemSelectedListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

public class DeviceMainActivity extends AppCompatActivity implements RecycleViewInterface {
    private RecyclerView recyclerView,recyclerView2;
    DeviceInfo deviceInfo = null;
    private String username = null;

    private Context context;
    NiceSpinner niceSpinner;

    List<String> names = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_main);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView2 = findViewById(R.id.recyclerView2);
        niceSpinner = findViewById(R.id.niceSpinner);
        context = this;

        niceSpinner.setOnSpinnerItemSelectedListener(new OnSpinnerItemSelectedListener() {
            @Override
            public void onItemSelected(NiceSpinner parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();


            }
        });


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getSerializable("username") != null) {
                username = String.valueOf(bundle.getSerializable("username"));
                LoginTool.funclogin(username);
            }
        }
    }




    private void loadDeviceInfoDB() {
        List<DeviceInfo> deviceInfos = LitePal.findAll(DeviceInfo.class);
        if(deviceInfos.size() == 1){
            deviceInfo = deviceInfos.get(0);
            loadCloudDevice();
        }
    }

    List<HashMap<String,List<HashMap>>> sipnner = new ArrayList<>();



    private void loadCloudDevice() {
        String api = APIUrl.APIDeviceMANET;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", deviceInfo.getmanet_UUID());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RxHttp.postJson(api).addAll(jsonObject.toString())
                .asClass(Response.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (data.getCode() == 0) {
                        String r1 = data.getData().toString();
                        HashMap<String,Object> map = JsonUtils.jsonToPojo(r1,HashMap.class);
                        String other = map.get("other").toString();
                        List<HashMap> otherMap = JsonUtils.jsonToList(other,HashMap.class);
                        for(HashMap tp : otherMap){
                            HashMap<String,List<HashMap>> tmp = new HashMap<>();
                            String l = JsonUtils.objectToJson(tp.get("list"));
                            List<HashMap> d = JsonUtils.jsonToList(l,HashMap.class);
                            names.add(tp.get("uuid").toString());
                            tmp.put(tp.get("uuid").toString(),d);
                            sipnner.add(tmp);
                        }
                        niceSpinner.attachDataSource(names);


                        String own = map.get("own").toString();
                        own = own.substring(1,own.length()-1);
                        HashMap<String,Object> ownMap = JsonUtils.jsonToPojo(own,HashMap.class);
                        String ownListStr = JsonUtils.objectToJson(ownMap.get("list"));
                        List<HashMap> ownlist =  JsonUtils.jsonToList(ownListStr,HashMap.class);

                        loadLocalMember(ownlist);
                        loadRecycle();
                    }
                }, throwable -> {

                });
    }

    private void loadLocalMember(List<HashMap>  ownlist) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(DeviceMainActivity.this);
        recyclerView2.setLayoutManager(linearLayoutManager);
        recyclerView2.setItemAnimator(new DefaultItemAnimator());
        DeviceCloudAdapter adapter = new DeviceCloudAdapter(context, R.layout.device_main_item,ownlist,this);
        recyclerView2.setAdapter(adapter);
    }

    private void loadRecycle(){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(DeviceMainActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DeviceCloudAdapter adapter = new DeviceCloudAdapter(context, R.layout.device_main_item, sipnner.get(0).get(names.get(0)),this);
        recyclerView.setAdapter(adapter);
    }


    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onItemClickPair(int position) {

    }
}