package com.yang.myapplication.entity;

public class Response<T> {
    private int    code;
    private String msg;
    private T      data;
    public Response(int errcode, String errmsg, T data) {
        this.code = errcode;
        this.msg = errmsg;
        this.data = data;
    }
    public Response Success(T data){
        return new Response(0,"",data);
    }
    public Response Success(T data,String msg){
        return new Response(0,msg,data);
    }
    public int getCode() {
        return code;
    }

    public void setCode(int errcode) {
        this.code = errcode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String errmsg) {
        this.msg = errmsg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}