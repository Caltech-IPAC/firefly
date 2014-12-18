package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 5/8/12
 * Time: 2:17 PM
 */


import edu.caltech.ipac.util.StringUtils;

/**
* @author Trey Roby
*/
public enum DrawSymbol {
    X,SQUARE,CROSS,DIAMOND,DOT,CIRCLE, SQUARE_X, EMP_CROSS,EMP_SQUARE_X;


    public static DrawSymbol getSymbol(String s) {
        DrawSymbol retval= DrawSymbol.X;
        if (!StringUtils.isEmpty(s)) {
            s= s.toUpperCase();
            retval= Enum.valueOf(DrawSymbol.class, s);
        }
        return retval;
    }

}

