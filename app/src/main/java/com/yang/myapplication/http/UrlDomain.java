package com.yang.myapplication.http;

import rxhttp.wrapper.annotation.DefaultDomain;

public class UrlDomain {
    public static String pingURL = "hello.eu-west-1.elasticbeanstalk.com";

    @DefaultDomain //设置为默认域名
//    public static String baseUrl = "http://172.20.10.9:8099/";
//    public static String baseUrl = "http://10.241.84.194:8888/";
    public static String baseUrl = "http://hello.eu-west-1.elasticbeanstalk.com/";
}