package com.yang.myapplication.Tools;

import com.yang.myapplication.entity.DeviceInfo;
public class RouterTool {
    private static DeviceInfo deviceInfo = null;
    private static String MANET_UUID = null;

    private static final String TAG = "RouterTool";
    public static void setDeviceInfo(DeviceInfo desvice) {
        deviceInfo = desvice;
        MANET_UUID = deviceInfo.getManet_UUID();
    }
    public static String[] routerList(String routerPath){
        return  routerPath.replaceAll("]","").replaceAll("\\[","").trim().split(",");
    }

}
