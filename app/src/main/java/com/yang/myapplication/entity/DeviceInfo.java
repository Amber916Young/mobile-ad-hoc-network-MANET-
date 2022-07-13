package com.yang.myapplication.entity;


import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

/**
 * @ClassName:DeviceInfo
 * @Auther: yyj
 * @Description:
 * @Date: 11/06/2022 19:29
 * @Version: v1.0
 */
public class DeviceInfo  extends LitePalSupport implements Serializable  {
    private String uuid;
    private String username;
    private String password;
    private String mac;
    private String loginDate;
    private String registerDate;
    private String status;
    private String manet_UUID;
    private String role;

    public DeviceInfo() {
    }

    public DeviceInfo(String uuid, String username, String password, String mac, String loginDate, String registerDate, String status, String manet_UUID, String role) {
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.mac = mac;
        this.loginDate = loginDate;
        this.registerDate = registerDate;
        this.status = status;
        this.manet_UUID = manet_UUID;
        this.role = role;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getManet_UUID() {
        return manet_UUID;
    }

    public void setManet_UUID(String manet_UUID) {
        this.manet_UUID = manet_UUID;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getmac() {
        return mac;
    }

    public void setmac(String mac) {
        this.mac = mac;
    }

    public String getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(String loginDate) {
        this.loginDate = loginDate;
    }

    public String getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(String registerDate) {
        this.registerDate = registerDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
