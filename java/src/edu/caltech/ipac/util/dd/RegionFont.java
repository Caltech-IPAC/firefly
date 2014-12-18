package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: 2/25/13
 * Time: 10:42 AM
 */


import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class RegionFont {

    private String name;
    private float pt;
    private String weight;
    private String slant;

    public RegionFont(String name, int pt, String weight, String slant) {
        this.name = name;
        this.pt = pt;
        this.weight = weight;
        this.slant = slant;
    }

    public RegionFont(String fontDesc) {
        String s= StringUtils.crunch(fontDesc);
        String sAry[]= s.split(" ");
        if (sAry.length>=1 && !StringUtils.isEmpty(sAry[0])) name= sAry[0];
        else                                                 name= "SansSerif";

        String ptStr;
        if (sAry.length>=2 && !StringUtils.isEmpty(sAry[1])) ptStr= sAry[1];
        else                                                 ptStr= "10";

        if (sAry.length>=3 && !StringUtils.isEmpty(sAry[2])) weight= sAry[2];
        else                                                 weight= "bold";

        if (sAry.length>=4 && !StringUtils.isEmpty(sAry[3])) slant= sAry[3];
        else                                                 slant= "italic";



        try {
            pt= Float.parseFloat(ptStr);
        } catch (NumberFormatException e) {
            pt= 10;
        }

    }

    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o instanceof RegionFont) {
            RegionFont f= (RegionFont)o;
            retval= (ComparisonUtil.equals(name,f.name) &&
                     ComparisonUtil.equals(weight,f.weight) &&
                     ComparisonUtil.equals(slant,f.slant) &&
                     ComparisonUtil.equals(pt,f.pt ));
        }
        return retval;
    }


    public String getName() {
        return name;
    }

    public float getPt() {
        return pt;
    }

    public String getWeight() {
        return weight;
    }

    public String getSlant() {
        return slant;
    }


    public boolean isBold() {
        return weight.equalsIgnoreCase("bold");
    }

    public boolean isItalic() {
        return slant.equalsIgnoreCase("italic");
    }

    @Override
    public String toString() { return name +" "+pt+" "+weight+" "+slant; }

    @Override
    public int hashCode() { return toString().hashCode(); }
}

