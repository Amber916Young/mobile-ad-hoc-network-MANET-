package com.yang.myapplication.http;

public class APIUrl {
    final public static String APIlogin =UrlDomain.baseUrl+"user/login";

    //TODO 6.1
    final public static String APINetCommunication =UrlDomain.baseUrl+"message/sendmessage";
    final public static String APISendUnreadMessage =UrlDomain.baseUrl+"message/sendUnreadMessage";
    final public static String APIGetUnreadMessage =UrlDomain.baseUrl+"message/getUnreadMessage";
    final public static String APIUpdateMessage =UrlDomain.baseUrl+"message/updateMessage";
    final public static String APIUploadFeedbackMessage =UrlDomain.baseUrl+"message/feedbackMessage";
    final public static String APISendUnreadMessageTwice =UrlDomain.baseUrl+"message/sendUnreadMessageTwice";
    final public static String APIqueryFromCloud =UrlDomain.baseUrl+"message/queryFromCloud";
    final public static String APIDeleteMessage =UrlDomain.baseUrl+"message/deleteUnreadMessage";



    final public static String APIDeviceMANET =UrlDomain.baseUrl+"MANET/membership";



    final public static String uploadRouterNeighbor =UrlDomain.baseUrl+"router/upload";
    final public static String getEachRouter =UrlDomain.baseUrl+"router/get";


    // 算法，形成路由结构
    final public static String formRouter =UrlDomain.baseUrl+"router/form/each/source";

}
