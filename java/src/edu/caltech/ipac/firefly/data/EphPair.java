package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 1/19/12
 * Time: 5:35 PM
 */


import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class EphPair implements Serializable {
    private final static String SPLIT_TOKEN= "--EphPair--";

    private String _name;
    private String _naifID;
    private String _primaryDes;

    private EphPair() {}

    public EphPair (String name, String naifID) {
        this(name,naifID,name);
    }

    public EphPair (String name, String naifID, String primaryDes) {
        _name      = name;
        _naifID    = naifID;
        _primaryDes= primaryDes;
    }
    public final String getName() { return _name; }
    public final String getNaifID() { return _naifID; }
    public final String getPrimaryDes() { return _primaryDes; }

    public String toString() {
        return _name+SPLIT_TOKEN+_naifID+SPLIT_TOKEN+_primaryDes;
    }

    public static EphPair parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,4);
        EphPair retval= null;
        if (sAry.length==3) {
            retval= new EphPair(sAry[0],sAry[1],sAry[2]);
        }
        return retval;
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
