package com.yang.myapplication.entity;


import com.yang.myapplication.Tools.RandomID;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;
import java.util.Date;

public class MessageInfo extends LitePalSupport implements Serializable {
    private String  uuid;
    private String content;
    private String sendDate;
    private String readDate;
    private int isRead; // 0 no 1 already
    private int isUpload; // 0 no 1 already
    private String targetName;
    private String targetMAC;
    private String sourceName;
    private String sourceMAC;


    public MessageInfo( String uuid, String content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC) {
        this.uuid = uuid;
        this.content = content;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.isUpload = 0;
    }

    public MessageInfo() {
    }

    public MessageInfo(String uuid, String content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC, int isUpload) {
        this.uuid = uuid;
        this.content = content;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.isUpload = isUpload;
    }

    public int getIsUpload() {
        return isUpload;
    }

    public void setIsUpload(int isUpload) {
        this.isUpload = isUpload;
    }



    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSendDate() {
        return sendDate;
    }

    public void setSendDate(String sendDate) {
        this.sendDate = sendDate;
    }

    public String getReadDate() {
        return readDate;
    }

    public void setReadDate(String readDate) {
        this.readDate = readDate;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetMAC() {
        return targetMAC;
    }

    public void setTargetMAC(String targetMAC) {
        this.targetMAC = targetMAC;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceMAC() {
        return sourceMAC;
    }

    public void setSourceMAC(String sourceMAC) {
        this.sourceMAC = sourceMAC;
    }
}
