package com.yang.myapplication.entity;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

public class TransferMessage extends LitePalSupport implements Serializable {
    private String des_name;
    private String des_mac;
    private String message;
    private int hop;
    private String source_name;
    private String source_mac;

    public TransferMessage(String des_name, String des_mac, String message, int hop, String source_name, String source_mac) {
        this.des_name = des_name;
        this.des_mac = des_mac;
        this.message = message;
        this.hop = hop;
        this.source_name = source_name;
        this.source_mac = source_mac;
    }

    public TransferMessage() {
    }

    public String getDes_name() {
        return des_name;
    }

    public void setDes_name(String des_name) {
        this.des_name = des_name;
    }

    public String getDes_mac() {
        return des_mac;
    }

    public void setDes_mac(String des_mac) {
        this.des_mac = des_mac;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getHop() {
        return hop;
    }

    public void setHop(int hop) {
        this.hop = hop;
    }

    public String getSource_name() {
        return source_name;
    }

    public void setSource_name(String source_name) {
        this.source_name = source_name;
    }

    public String getSource_mac() {
        return source_mac;
    }

    public void setSource_mac(String source_mac) {
        this.source_mac = source_mac;
    }
}
