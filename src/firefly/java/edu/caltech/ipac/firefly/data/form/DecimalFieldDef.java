/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.util.dd.RangeFieldDef;


/**
 * Field definition that allows float input
 * This code can be can be used on the server side except for the format() method.
 * That can only be call on the client side.
 * @version $Id: DecimalFieldDef.java,v 1.10 2011/02/24 23:43:41 roby Exp $
 */
public abstract class DecimalFieldDef extends RangeFieldDef {

    public static String DEFAULT_MASK = "[+-]?[0-9]*[.]?[0-9]+";

    private int precision= 2;
    private boolean  sciNote= false;

    public DecimalFieldDef() { }

    public DecimalFieldDef(String name) {
        super(name);
    }


    public void setPrecision(int precision) { this.precision= precision; }

    public int getPrecision() { return precision; }

    public void setUseSciNotation(boolean use) { sciNote= use; }

    public boolean getUseSciNotation() { return sciNote; }

    /**
     * This method should only be call when running on the client side in GWT compiled code.
     * @param n the number to format
     * @return a string formatted version of the number
     */
    public String format(Number n) {
        return getNumberFormat().format(n.doubleValue());
    }
    
    private NumberFormat getNumberFormat() {
        NumberFormat nf;
        if (sciNote) {
            nf= NumberFormat.getScientificFormat();
        }
        else {
            StringBuffer sb= new StringBuffer(9);
            if (precision==0) {
                nf= NumberFormat.getFormat("#");
            }
            else {
                sb.append("#.");
                for(int i= 0; (i<precision); i++) sb.append("#");
                nf= NumberFormat.getFormat(sb.toString());
            }
        }
        return nf;
    }

    public abstract void setMinValue(Number min);
    public abstract Number getMinValue();
    public abstract void setMaxValue(Number min);
    public abstract Number getMaxValue();

    public String getDefaultMask() { return DEFAULT_MASK; }

    public double getDoubleValue(Object v) {
        if (v != null) {
            if (v instanceof Double) {
                return (Double)v;
            } else {
                return Double.parseDouble(v.toString());
            }
        }
        return Float.NaN;
    }

}
