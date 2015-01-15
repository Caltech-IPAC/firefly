/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
