package com.yang.myapplication.Tools;

public class ProtocolModel {
    public final static String  feedback = "ID&END&FLAG";
    public final static String  directMsg = "MESSAGE&SOURCENAME&SOURCMAC&TAREGETNAME&TARGETMAC&START&END&ACK&ID";
    public final static String  cloudMultihop = "DESTMAC@DESTNAME@SOURCENAME@SOURCEMAC@LASTNAME@END@ACK@ID@TYPE";
    public final static String  Multihop = "MESSAGE@NEIGHBORMAC@NEIGHBORNAME@SOURCENAME@LASTNAME@HOP@START@END@ACK@ID@ORGINAL@SOURCEMAC";

    /**
     * 0 ID
     * 1 MESSAGE
     * 2 START
     * 3 END
     * 4 DESNAME
     * 5 DESMAC
     * 6 SOURCENAME
     * 7 SOURCEMAC
     * 8 Router
     */
    public final static String  Multi_hop = "ID@MESSAGE@START@END@DESNAME@DESMAC@SOURCENAME@SOURCEMAC@ROUTER";
}
