/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

    
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

import java.io.Serializable;

public class Metric implements Serializable {

    private final static String SPLIT_TOKEN= "--Metric--";
    public static final double NULL_DOUBLE = Double.NaN;

    private String desc;
    private ImageWorkSpacePt ip;
    private double value;
    private String units;

    public Metric(String desc, ImageWorkSpacePt ip, double value, String units) {
        this.desc = desc;
        this.ip = ip;
        this.value = value;
        this.units = units;
    }

    public String getDesc() { return desc;}
    public ImageWorkSpacePt getImageWorkSpacePt() {return ip;}
    public double getValue() {return value;}
    public String getUnits() {return units;}

    public String toString() {
        return StringUtils.combine(SPLIT_TOKEN,desc,ip!=null? ip.serialize() : null,value+"",units);
    }
}
