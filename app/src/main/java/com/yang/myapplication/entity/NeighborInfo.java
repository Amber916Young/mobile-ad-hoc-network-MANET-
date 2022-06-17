package com.yang.myapplication.entity;

import android.bluetooth.BluetoothDevice;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;
import java.util.Date;

public class NeighborInfo extends LitePalSupport implements Serializable {
    private int hop;
    private Date timestamp;
//    private int rssi;
    private String neighborMac;
    private String neighborName;
    private String path;

    public NeighborInfo(String neighborMac, String neighborName, int hop, Date timestamp,String path) {
        this.neighborMac = neighborMac;
        this.neighborName = neighborName;
        this.hop = hop;
        this.timestamp = timestamp;
        this.path = path;
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
