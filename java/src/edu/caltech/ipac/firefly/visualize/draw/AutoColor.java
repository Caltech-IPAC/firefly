package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.visualize.WebPlot;

/**
 * User: roby
 * Date: Oct 5, 2009
 * Time: 8:05:23 PM
 */


/**
 * @author Trey Roby
 */
public class AutoColor {

    public static final String  DEFER  = "AC-DEFER";

    public static final String  PT_1  = "AC-P-1";
    public static final String HIGHLIGHTED_PT = "AC-SEL-P";
    public static final String SELECTED_PT = "AC-AREA_SEL-P";
    public static final String  PT_3  = "AC-P-3";
    public static final String  PT_4  = "AC-P-4";

    public static final String  DRAW_1= "AC-D-1";
    public static final String  DRAW_2= "AC-D-2";

    private static final String DEF_PT1=  "red";
    private static final String DEF_HIGHLIGHTED_PT =  "00aaff";
    private static final String DEF_SELECTED_PT =  "yellow";
    private static final String DEF_PT3=  "pink";
    private static final String DEF_PT4=  "blue";
    private static final String DEF_D1=  "red";
    private static final String DEF_D2=  "5500ff";

    private String _ptColor1= DEF_PT1;
    private String _ptHighlighted = DEF_HIGHLIGHTED_PT;
    private String _ptSelected = DEF_SELECTED_PT;
    private String _ptColor3= DEF_PT3;
    private String _ptColor4= DEF_PT4;
    private String _drawColor1= DEF_D1;
    private String _drawColor2= DEF_D2;

    private final String _defColor;


    public AutoColor(WebPlot plot, Drawer drawer) {
        if (plot!=null && !plot.isThreeColor()) {
            findColors(plot.getColorTableID());
        }
        _defColor= getColor(drawer.getDefaultColor());
    }

    public AutoColor(int colorTable, Drawer drawer) {
        findColors(colorTable);
        _defColor= getColor(drawer.getDefaultColor());
    }

    public String getColor(String color) {
        String retval;

        if (color==null)                      retval= _defColor;
        else if (color.equals(PT_1))          retval= _ptColor1;
        else if (color.equals(HIGHLIGHTED_PT))   retval= _ptHighlighted;
        else if (color.equals(SELECTED_PT))   retval= _ptSelected;
        else if (color.equals(PT_3))   retval= _ptColor3;
        else if (color.equals(PT_4))   retval= _ptColor4;
        else if (color.equals(DRAW_1)) retval= _drawColor1;
        else if (color.equals(DRAW_2)) retval= _drawColor2;
        else                           retval= color;
        return retval;
    }


    public String getPtColor1() { return _ptColor1; }
    public String getHighlightedPtColor() { return _ptHighlighted; }
    public String getPtColor3() { return _ptColor3; }
    public String getPtColor4() { return _ptColor4; }
    public String getDrawColor1() { return _drawColor1; }
    public String getDrawColor2() { return _drawColor2; }


    private void findColors(int ct) {

        switch (ct) {
            case 15 :
                _ptColor1= "red";
                _ptHighlighted = "green";
                _ptColor3= "blue";
                _ptColor4= "pink";
                _drawColor1= "red";
                _drawColor2= "green";
                break;
            case 16 :
                _ptColor1= "#FF00FF";
                _ptHighlighted = "#99FF00";
                _ptColor3= "blue";
                _ptColor4= "pink";
                _drawColor1= "#FF00FF";
                _drawColor2= "green";
                break;
            case 3 :
            case 17 :
                _ptColor1= "blue";
                _ptHighlighted = "#99FF00";
                _ptColor3= "red";
                _ptColor4= "pink";
                _drawColor1= "blue";
                _drawColor2= "green";
                break;
            case 7 :
                _ptColor1= "green";
                _ptHighlighted = "#00aaff";
                _ptColor3= "red";
                _ptColor4= "#99FF00";
                _drawColor1= "green";
                _drawColor2= "yellow";
                break;
            case 4 :
                _ptColor1= "black";
                _ptHighlighted = "4450ff";
                _ptColor3= DEF_PT3;
                _ptColor4= DEF_PT4;
                _drawColor1= DEF_D1;
                _drawColor2= DEF_D2;
                break;
            case 18 :
            case 19 :
            case 20 :
            case 21 :
                _ptColor1= DEF_PT1;
                _ptHighlighted = DEF_HIGHLIGHTED_PT;
                _ptColor3= DEF_PT3;
                _ptColor4= DEF_PT4;
                _drawColor1= DEF_D1;
                _drawColor2= DEF_D2;
                break;
            default :
                _ptColor1= DEF_PT1;
                _ptHighlighted = DEF_HIGHLIGHTED_PT;
                _ptColor3= DEF_PT3;
                _ptColor4= DEF_PT4;
                _drawColor1= DEF_D1;
                _drawColor2= DEF_D2;
                break;
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
