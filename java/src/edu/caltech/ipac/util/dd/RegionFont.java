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
