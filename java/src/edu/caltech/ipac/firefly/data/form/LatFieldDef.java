package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.firefly.visualize.CoordinateSysListener;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

/**
 * Float field definition that allows sexagesimal input common for latitude (DMS)
 * @version $Id: LatFieldDef.java,v 1.9 2010/11/03 19:55:56 roby Exp $
 */
public class LatFieldDef extends FloatFieldDef
    implements CoordinateSysListener {

    public static String DEFAULT_MASK = "";

    private boolean _isEquatorial = true;

    public LatFieldDef() {}

    public LatFieldDef(String name) {
        super(name);
    }

    public Float getFloat(Object val) {
        try {
            return getFloat(val, _isEquatorial);
        } catch (CoordException ce) {
            throw new NumberFormatException(ce.getMessage());
        }
    }

    public static Float getFloat(Object val, boolean isEquatorial)
            throws CoordException {
        if (val instanceof Float) {
            return (Float)val;
        } else {
            String  strVal = val.toString();
            strVal= strVal.trim();
            if (strVal.matches(FloatFieldDef.DEFAULT_MASK)) {
                return new Float(strVal);
            } else {
                return (float) CoordUtil.sex2dd(strVal, true, isEquatorial);
            }
        }
    }

    public boolean validate(Object aValue) throws ValidationException {
        try {
            return super.validate(getFloat(aValue));
        } catch (Exception e) {
            throw new ValidationException(getErrMsg());
        }
    }

    public void onCoordinateSysChange(CoordinateSys newCoordSys) {
        _isEquatorial = newCoordSys.isEquatorial();
    }

    public String getDefaultMask() { return null; } // returning null will disable mask checking and force validate to be called
}
