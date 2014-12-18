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

