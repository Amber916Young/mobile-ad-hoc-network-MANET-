package com.yang.myapplication.entity;

import android.bluetooth.BluetoothDevice;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;
import java.util.Date;

public class NeighborInfo extends LitePalSupport implements Serializable {
    public static int onLine = 1;
    public static int offLine = 0;
    private int hop;
    private Date timestamp;
//    private int rssi;
    private String neighborMac;
    private String neighborName;
    private String path;
    private String lastMessage;
    private String lastTime;
    private int   connection_status;

    public int getConnection_status() {
        return connection_status;
    }

    public void setConnection_status(int connection_status) {
        this.connection_status = connection_status;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public NeighborInfo(String neighborMac, String neighborName, int hop, Date timestamp, String path) {
        this.neighborMac = neighborMac;
        this.neighborName = neighborName;
        this.hop = hop;
        this.timestamp = timestamp;
        this.path = path;
        this.lastMessage = "";
        this.lastTime = "";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public NeighborInfo() {
    }

    public int getHop() {
        return hop;
    }

    public void setHop(int hop) {
        this.hop = hop;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public String getNeighborMac() {
        return neighborMac;
    }

    public void setNeighborMac(String neighborMac) {
        this.neighborMac = neighborMac;
    }

    public String getNeighborName() {
        return neighborName;
    }

    public void setNeighborName(String neighborName) {
        this.neighborName = neighborName;
    }


}
