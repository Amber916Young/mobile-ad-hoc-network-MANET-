package com.yang.myapplication.Tools;

public class ProtocolModel {
    public final static String  feedback = "ID&END&FLAG";
    // dvdv&C&DC:0B:34:BD:65:F6&KFW&98:D6:F7:6B:6C:C8&2022-07-13 13:30:18&2022-07-13
    public final static String  directMsg = "MESSAGE&SOURCENAME&SOURCMAC&TAREGETNAME&TARGETMAC&START&END&ACK&ID";
    public final static String  cloudMultihop = "DESTMAC@DESTNAME@SOURCENAME@SOURCEMAC@LASTNAME@END@ACK@ID@TYPE";

    /**
     * 0 ID
     * 1 MESSAGE
     * 2 START
     * 3 END
     * 4 DESNAME
     * 5 DESMAC
     * 6 SOURCENAME
     * 7 SOURCEMAC
     * 8 Router [A,B,C,D]
     */
    public final static String  Multi_hop = "ID@MESSAGE@START@END@DESNAME@DESMAC@SOURCENAME@SOURCEMAC@ROUTER@UPLOAD";
    public final static String  Multi_hop_feedback = "ID@END@ROUTER@UPLOAD";
}
