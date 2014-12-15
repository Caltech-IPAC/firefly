package edu.caltech.ipac.visualize.draw;

    
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

import java.io.Serializable;

public class Metric implements Serializable, HandSerialize {

    private final static String SPLIT_TOKEN= "--Metric--";
    public static final double NULL_DOUBLE = Double.NaN;

    String desc;
    ImageWorkSpacePt ip;
    double value;
    String units;

    public Metric(){}

    public Metric(String desc, ImageWorkSpacePt ip, double value, String units) {
        this.desc = desc;
        this.ip = ip;
        this.value = value;
        this.units = units;
    }

    boolean hasPosition() {
        return ip != null;
    }

    boolean hasValue() {
        return value != NULL_DOUBLE;
    }

    public String getDesc() { return desc;}
    public ImageWorkSpacePt getImageWorkSpacePt() {return ip;}
    public double getValue() {return value;}
    public String getUnits() {return units;}

    public String serialize() {
        return StringUtils.combine(SPLIT_TOKEN,desc,ip!=null? ip.serialize() : null,value+"",units);
    }

    public static Metric parse(String s) {
        try {
            String sAry[]= StringUtils.parseHelper(s,4,SPLIT_TOKEN);
            int i= 0;
            String desc=         StringUtils.checkNull(sAry[i++]);
            ImageWorkSpacePt ip= ImageWorkSpacePt.parse(sAry[i++]);
            double value=        StringUtils.parseDouble(sAry[i++]);
            String units=        StringUtils.checkNull(sAry[i++]);
            return new Metric(desc,ip,value,units);
        } catch (IllegalArgumentException e) {
            return null;
        }

    }
}

 /*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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