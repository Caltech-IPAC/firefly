/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;


/**
 * User: roby
 * Date: Oct 5, 2009
 * Time: 8:05:23 PM
 */


/**
 * @author Trey Roby
 */
public class AutoColor {

    public static final String HIGHLIGHTED_PT = "AC-SEL-P";
    public static final String SELECTED_PT = "AC-AREA_SEL-P";
    public static final String  PT_1  = "AC-P-1";
    public static final String  PT_2  = "AC-P-2";
    public static final String  PT_3  = "AC-P-3";
    public static final String  PT_4  = "AC-P-4";
    public static final String  PT_5  = "AC-P-5";
    public static final String  PT_6  = "AC-P-6";

    public static final String  DRAW_1= "AC-D-1";
    public static final String  DRAW_2= "AC-D-2";

//    private static final String DEF_PT1=  "red";
    private static final String DEF_PT1=  "ff0000"; // red
    private static final String DEF_PT2=  "00ff00"; //green
    private static final String DEF_PT3=  "pink";  // point
    private static final String DEF_PT4=  "00a8ff"; //blue
    private static final String DEF_PT5=  "990099"; //purple
    private static final String DEF_PT6=  "ff8000"; //orange
    private static final String DEF_HIGHLIGHTED_PT =  "00aaff";
    private static final String DEF_SELECTED_PT =  "ffff00";

    private static final String DEF_D1=  "ff0000";
    private static final String DEF_D2=  "5500ff";

    private String _ptColor1= DEF_PT1;
    private String _ptColor2= DEF_PT2;
    private String _ptColor3= DEF_PT3;
    private String _ptColor4= DEF_PT4;
    private String _ptColor5= DEF_PT5;
    private String _ptColor6= DEF_PT6;
    private String _ptHighlighted = DEF_HIGHLIGHTED_PT;
    private String _ptSelected = DEF_SELECTED_PT;
    private String _drawColor1= DEF_D1;
    private String _drawColor2= DEF_D2;

    private final String _defColor;



    public AutoColor(int colorTable, String defColor) {
        findColors(colorTable);
        _defColor= getColor(defColor);
    }

    public AutoColor() {
        findColors(0);
        _defColor= "red";
    }

    public String getColor(String color) {
        String retval;

        if (color==null)                      retval= _defColor;
        else if (color.equals(HIGHLIGHTED_PT))   retval= _ptHighlighted;
        else if (color.equals(SELECTED_PT))   retval= _ptSelected;
        else if (color.equals(PT_1))   retval= _ptColor1;
        else if (color.equals(PT_2))   retval= _ptColor2;
        else if (color.equals(PT_3))   retval= _ptColor3;
        else if (color.equals(PT_4))   retval= _ptColor4;
        else if (color.equals(PT_5))   retval= _ptColor5;
        else if (color.equals(PT_6))   retval= _ptColor6;
        else if (color.equals(DRAW_1)) retval= _drawColor1;
        else if (color.equals(DRAW_2)) retval= _drawColor2;
        else                           retval= color;
        return retval;
    }

    private void findColors(int ct) {

        switch (ct) {
//            case 15 :
//                _ptColor1= "red";
//                _ptHighlighted = "green";
//                _ptColor3= "blue";
//                _ptColor4= "pink";
//                _drawColor1= "red";
//                _drawColor2= "green";
//                break;
//            case 16 :
//                _ptColor1= "#FF00FF";
//                _ptHighlighted = "#99FF00";
//                _ptColor3= "blue";
//                _ptColor4= "pink";
//                _drawColor1= "#FF00FF";
//                _drawColor2= "green";
//                break;
//            case 3 :
//            case 17 :
//                _ptColor1= "blue";
//                _ptHighlighted = "#99FF00";
//                _ptColor3= "red";
//                _ptColor4= "pink";
//                _drawColor1= "blue";
//                _drawColor2= "green";
//                break;
//            case 7 :
//                _ptColor1= "green";
//                _ptHighlighted = "#00aaff";
//                _ptColor3= "red";
//                _ptColor4= "#99FF00";
//                _drawColor1= "green";
//                _drawColor2= "yellow";
//                break;
//            case 4 :
//                _ptColor1= "black";
//                _ptHighlighted = "4450ff";
//                _ptColor3= DEF_PT3;
//                _ptColor4= DEF_PT4;
//                _drawColor1= DEF_D1;
//                _drawColor2= DEF_D2;
//                break;
//            case 18 :
//            case 19 :
//            case 20 :
//            case 21 :
//                _ptColor1= DEF_PT1;
//                _ptHighlighted = DEF_HIGHLIGHTED_PT;
//                _ptColor3= DEF_PT3;
//                _ptColor4= DEF_PT4;
//                _drawColor1= DEF_D1;
//                _drawColor2= DEF_D2;
//                break;
            default :
                _ptHighlighted = DEF_HIGHLIGHTED_PT;
                _ptColor1= DEF_PT1;
                _ptColor2= DEF_PT2;
                _ptColor3= DEF_PT3;
                _ptColor4= DEF_PT4;
                _ptColor5= DEF_PT5;
                _ptColor6= DEF_PT6;
                _drawColor1= DEF_D1;
                _drawColor2= DEF_D2;
                break;
        }

    }

}

