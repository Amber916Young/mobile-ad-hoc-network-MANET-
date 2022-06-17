package com.yang.myapplication.entity;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

public class UnreadMsgDB  {
    private String uuid;
    private String content;
    private String sendDate;
    private String readDate;
    private int isRead; // 0 no 1 already
    private String targetName;
    private String targetMAC;
    private String sourceName;
    private String sourceMAC;
    private String oMAC;
    private String oName;
    private String uploadName;
    private String uploadMAC;
    private String uploadTime;
    private int isUpload;

    public UnreadMsgDB() {
    }

    public UnreadMsgDB(String uuid, String content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC, String uploadName, String uploadMAC, String uploadTime, int isUpload) {
        this.uuid = uuid;
        this.content = content;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.uploadName = uploadName;
        this.uploadMAC = uploadMAC;
        this.uploadTime = uploadTime;
        this.isUpload = isUpload;
    }

    public UnreadMsgDB(String uuid, String content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC, String oMAC, String oName, String uploadName, String uploadMAC, String uploadTime, int isUpload) {
        this.uuid = uuid;
        this.content = content;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.oMAC = oMAC;
        this.oName = oName;
        this.uploadName = uploadName;
        this.uploadMAC = uploadMAC;
        this.uploadTime = uploadTime;
        this.isUpload = isUpload;
    }

    public UnreadMsgDB(String uuid, String content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC, String oMAC, String oName) {
        this.uuid = uuid;
        this.content = content;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.oMAC = oMAC;
        this.oName = oName;
    }

    public String getUploadName() {
        return uploadName;
    }

    public void setUploadName(String uploadName) {
        this.uploadName = uploadName;
    }

    public String getUploadMAC() {
        return uploadMAC;
    }

    public void setUploadMAC(String uploadMAC) {
        this.uploadMAC = uploadMAC;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
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

    public String getoMAC() {
        return oMAC;
    }

    public void setoMAC(String oMAC) {
        this.oMAC = oMAC;
    }

    public String getoName() {
        return oName;
    }

    public void setoName(String oName) {
        this.oName = oName;
    }
}
