package com.yang.myapplication.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class NetworkTool {

    /**
     * @param target_name IP address or domain name
     * @param out_time    Timeout interval in milliseconds
     * @return
     * @throws IOException
     */
    public static boolean ping(String target_name, int out_time) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        String ping_command = "ping " + target_name + " -w " + out_time;
        Process process = runtime.exec(ping_command);
        if (null == process) return false;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        String line = null;
        while (null != (line = bufferedReader.readLine())) {
            if (line.startsWith("bytes from", 3)) return true;
            if (line.startsWith("from"))  return true;
        }
        bufferedReader.close();
        return false;
    }
    public static void main(String[] args) throws IOException {
        System.out.println(NetworkTool.ping("127.0.0.1", 10));
        System.out.println("====================================");
        System.out.println(NetworkTool.ping("baidu.com", 10));
    }


    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

}
