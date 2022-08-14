package com.yang.myapplication.entity;


import android.graphics.Bitmap;

import com.yang.myapplication.Tools.RandomID;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

public class MessageInfo extends LitePalSupport implements Serializable {
    public static final int DATA_IMAGE = 2;
    public static final int DATA_AUDIO = 3;
    public static final int DATA_TEXT = 1;

    @Column(unique = true, defaultValue = "unknown")
    private String  uuid;
    private Object content;
    public String message;
    private String sendDate;
    private String readDate = "END";
    private int isRead; // 0 no 1 already read by the target
    private int isUpload; // 0 no 1 already upload to cloud
    public Bitmap imageBitmap;
    public File audioFile;
    public int dataType;
    private String targetName;
    private String targetMAC;
    private String sourceName;
    private String sourceMAC;
    private String routeList;


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public File getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getRouteList() {
        return routeList;
    }

    public void setRouteList(String routeList) {
        this.routeList = routeList;
    }

    public MessageInfo(String uuid, Object content, String sendDate, String readDate, int isRead, int isUpload, String targetName, String targetMAC, String sourceName, String sourceMAC, String routeList) {
        this.uuid = uuid;
        this.content = content;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.isUpload = isUpload;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.routeList = routeList;
    }

    public MessageInfo(String uuid, Object content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC) {
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

    public MessageInfo(String uuid, Object content, String message, String sendDate, String readDate, int isRead, int isUpload, Bitmap imageBitmap, File audioFile, int dataType, String targetName, String targetMAC, String sourceName, String sourceMAC, String routeList) {
        this.uuid = uuid;
        this.content = content;
        this.message = message;
        this.sendDate = sendDate;
        this.readDate = readDate;
        this.isRead = isRead;
        this.isUpload = isUpload;
        this.imageBitmap = imageBitmap;
        this.audioFile = audioFile;
        this.dataType = dataType;
        this.targetName = targetName;
        this.targetMAC = targetMAC;
        this.sourceName = sourceName;
        this.sourceMAC = sourceMAC;
        this.routeList = routeList;
    }

    public MessageInfo(String uuid, Object content, String sendDate, String readDate, int isRead, String targetName, String targetMAC, String sourceName, String sourceMAC, int isUpload) {
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

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
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
