package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.firefly.visualize.CoordinateSysListener;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Daniel Balandran
 *
 * @version $Id:
 */
public class MultiCoordFieldDef extends FloatFieldDef
    implements CoordinateSysListener {

    public static String DEFAULT_MASK = "";

    private boolean _isEquatorial = true;
    private float minRaValue;
    private float maxRaValue;
    private float minDecValue;
    private float maxDecValue;
    /*
    public static final String INCLUSIVE = "inclusive";
    public static final String EXCLUSIVE = "exclusive";
    protected static final String UNDEFINED = "undefined";
    */

    /**
	 * This field defines the minimum value range type.
	 * It can be one of the followings:
	 * INCLUSIVE
	 * EXCLUSIVE
	 */
	private String minRaBoundType= UNDEFINED ;
    private String minDecBoundType= UNDEFINED ;

    /**
	 * This field defines the maximum value range type.
	 * It can be one of the followings:
	 * INCLUSIVE
	 * EXCLUSIVE
	 */
	private String maxRaBoundType= UNDEFINED ;
    private String maxDecBoundType= UNDEFINED ;

    public MultiCoordFieldDef() {}

    public MultiCoordFieldDef(String name) {
        super(name);
    }

    //Ra Values----------------------------------------------
    public float getRaMinValue() {
        return minRaValue;
    }

    public void setRaMinValue(Number minValue) {
        String boundType = getRaMinBoundType().equals(UNDEFINED) ? INCLUSIVE : getRaMinBoundType();
        setRaMinValue(minValue.floatValue(),  boundType);
    }

    public void setRaMinValue(float minValue, String boundType) {
        this.minRaValue = minValue;
        setRaMinBoundType(boundType);
    }

    public float getRaMaxValue() {
        return maxRaValue;
    }

    public void setRaMaxValue(Number maxValue) {
        String boundType = getRaMaxBoundType().equals(UNDEFINED) ? INCLUSIVE : getRaMaxBoundType();
        setRaMaxValue(maxValue.floatValue(),  boundType);
    }

    public void setRaMaxValue(float maxValue, String boundType) {
        this.maxRaValue = maxValue;
        setRaMaxBoundType(boundType);
    }

    public String getRaMinBoundType() {
        return minRaBoundType;
    }

    /**
     * Set the minimum boundary type of this field.  It can be either inclusive or exclusive.
     * If this property is not given, then it implies this field has no mininum boundary.
     * @param minBoundType  either {@link #INCLUSIVE} or {@link #EXCLUSIVE}
     */
    public void setRaMinBoundType(String minBoundType) {
        this.minRaBoundType = minBoundType==null ? UNDEFINED : minBoundType;
    }

    public String getRaMaxBoundType() {
        return maxRaBoundType;
    }

    /**
     * Set the maximum boundary type of this field.  It can be either inclusive or exclusive.
     * If this property is not defined, then it implies this field has no maximum boundary.
     * @param maxBoundType  either {@link #INCLUSIVE} or {@link #EXCLUSIVE}
     */
    public void setRaMaxBoundType(String maxBoundType) {
        this.maxRaBoundType = maxBoundType==null ? UNDEFINED : maxBoundType;
    }

    ///END Ra Values-----------------------------------------


    //Dec Values----------------------------------------------
    public float getDecMinValue() {
        return minDecValue;
    }

    public void setDecMinValue(Number minValue) {
        String boundType = getDecMinBoundType().equals(UNDEFINED) ? INCLUSIVE : getDecMinBoundType();
        setDecMinValue(minValue.floatValue(),  boundType);
    }

    public void setDecMinValue(float minValue, String boundType) {
        this.minDecValue = minValue;
        setDecMinBoundType(boundType);
    }

    public float getDecMaxValue() {
        return maxDecValue;
    }

    public void setDecMaxValue(Number maxValue) {
        String boundType = getDecMaxBoundType().equals(UNDEFINED) ? INCLUSIVE : getDecMaxBoundType();
        setDecMaxValue(maxValue.floatValue(),  boundType);
    }

    public void setDecMaxValue(float maxValue, String boundType) {
        this.maxDecValue = maxValue;
        setDecMaxBoundType(boundType);
    }



    public String getDecMinBoundType() {
        return minDecBoundType;
    }

    /**
     * Set the minimum boundary type of this field.  It can be either inclusive or exclusive.
     * If this property is not given, then it implies this field has no mininum boundary.
     * @param minBoundType  either {@link #INCLUSIVE} or {@link #EXCLUSIVE}
     */
    public void setDecMinBoundType(String minBoundType) {
        this.minDecBoundType = minBoundType==null ? UNDEFINED : minBoundType;
    }

    public String getDecMaxBoundType() {
        return maxDecBoundType;
    }

    /**
     * Set the maximum boundary type of this field.  It can be either inclusive or exclusive.
     * If this property is not defined, then it implies this field has no maximum boundary.
     * @param maxBoundType  either {@link #INCLUSIVE} or {@link #EXCLUSIVE}
     */
    public void setDecMaxBoundType(String maxBoundType) {
        this.maxDecBoundType = maxBoundType==null ? UNDEFINED : maxBoundType;
    }

    ///END Dec Values-----------------------------------------

    public Float getFloat(Object val, boolean isLat) {
        try {
            return getFloat(val, _isEquatorial, isLat);
        } catch (CoordException ce) {
            throw new NumberFormatException(ce.getMessage());
        }
    }

    public static Float getFloat(Object val, boolean isEquatorial, boolean isLat)
            throws CoordException {
        if (val instanceof Float) {
            return (Float)val;
        } else {
            String  strVal = val.toString();
            if (strVal.matches(FloatFieldDef.DEFAULT_MASK)) {
                return new Float(strVal);
            } else {
                return (float) CoordUtil.sex2dd(strVal, isLat, isEquatorial);
            }
        }
    }

    public boolean validate(Object aValue) throws ValidationException {
        return parseCoords(aValue);

       /*
         try {
            return parseCoords(aValue);
        } catch (ValidationException e) {
            throw new ValidationException(e.getMessage());
        }   */
    }

    public void onCoordinateSysChange(CoordinateSys newCoordSys) {
        _isEquatorial = newCoordSys.isEquatorial();
    }

    public String getDefaultMask() { return null; } // returning null will disable mask checking and force validate to be called


    public boolean parseCoords (Object aValue)throws ValidationException {
            String coords = aValue.toString();
            String[] values = coords.trim().split(",");

                if (StringUtils.isEmpty(aValue)) {
                    return true;
                }

                if(values.length < 3 || values.length > 15){
                    setErrMsg("You must enter minimum 3 vertices and maximum 15 vertices.");
                    throw new ValidationException("You must enter minimum 3 vertices and maximum 15 vertices.");
                }
                for (String str : values){
                    String[] s;
                    s = str.trim().split(" +");
                    if (s.length != 2) {
                        setErrMsg("RA and Dec must be in pairs, separated by space");
                        throw new ValidationException("RA and Dec must be in pairs, separated by space");
                    }
                    validateRA(getFloat(s[0], false));
                    validateDEC(getFloat(s[1], true));
                }
            return true;
        }

       private boolean validateRA(Object aValue) throws ValidationException {
            String errNotValue =    "RA is not a valid decimal value: "+ aValue.toString();
            String errRangeIn =     "RA is out of range [" + minRaValue +", "+ maxRaValue +"] deg";
            String errRangeEx =     "RA is out of range (" + minRaValue +", "+ maxRaValue +") deg";

             /*
            if (StringUtils.isEmpty(aValue)) {
                if (isNullAllow()) {
                    return true;
                }
                else {
                    throw new ValidationException("Missing RA in position pair");
                }
            } */

            if (!isValidForm(aValue)) {
                throw new ValidationException(errNotValue);
            }
            if (minRaBoundType != null && !minRaBoundType.equals(UNDEFINED) ) {
                int v;
                try {
                    v = compareToRaMin(aValue);
                } catch (Exception e) {
                    setErrMsg(errNotValue);
                    throw new ValidationException(errNotValue);
                }
                if (minRaBoundType.equals(EXCLUSIVE)){
                    if (v <= 0) {
                       setErrMsg(errRangeEx);
                       throw new ValidationException(errRangeEx);
                    }
                } else if (v < 0) {
                         setErrMsg(errRangeIn);
                         throw new ValidationException(errRangeIn);
                    }
                }


            if (getRaMaxBoundType() != null && !getRaMaxBoundType().equals(UNDEFINED) ) {
                int v;
                try {
                    v = compareToRaMax(aValue);
                } catch (Exception e) {
                    setErrMsg(errNotValue);
                    throw new ValidationException(errNotValue);
                }
                if (getRaMinBoundType().equals(EXCLUSIVE)){
                    if (v >= 0) {
                       setErrMsg(errRangeEx);
                       throw new ValidationException(errRangeEx);
                    }
                } else if (v > 0) {
                     setErrMsg(errRangeIn);
                     throw new ValidationException(errRangeIn);
                    }
                }

            return true;
        }

        private boolean validateDEC(Object aValue) throws ValidationException {
            String errNotValue =    "Dec is not a valid decimal value: "+ aValue.toString();
            String errRangeIn =     "Dec is out of range [" + minDecValue +", "+ maxDecValue +"] deg";
            String errRangeEx =     "Dec is out of range (" + minDecValue +", "+ maxDecValue +") deg";
            /*
            if (StringUtils.isEmpty(aValue)) {
                if (isNullAllow()) {
                    return true;
                }
                else {
                    throw new ValidationException("Missing Dec in a position pair");
                }
            }
            */

            if (!isValidForm(aValue)) {
                setErrMsg(errNotValue);
                throw new ValidationException(errNotValue);
            }

            if (getDecMinBoundType() != null && !getDecMinBoundType().equals(UNDEFINED) ) {
                int v;
                try {
                    v = compareToDecMin(aValue);
                } catch (Exception e) {
                    setErrMsg(errNotValue);
                    throw new ValidationException(errNotValue);
                }
                if (getDecMinBoundType().equals(EXCLUSIVE)){
                    if (v <= 0) {
                        setErrMsg(errRangeEx);
                        throw new ValidationException(errRangeEx);
                    }
                } else if (v < 0) {
                        setErrMsg(errRangeIn);
                        throw new ValidationException(errRangeIn);
                    }
            }

            if (getDecMaxBoundType() != null && !getDecMaxBoundType().equals(UNDEFINED) ) {
                int v;
                try {
                    v = compareToDecMax(aValue);
                } catch (Exception e) {
                    setErrMsg(errNotValue);
                    throw new ValidationException(errNotValue);
                }
                if (getDecMinBoundType().equals(EXCLUSIVE)){
                    if (v >= 0) {
                        setErrMsg(errRangeEx);
                        throw new ValidationException(errRangeEx);
                    }
                } else if (v > 0) {
                         setErrMsg(errRangeIn);
                        throw new ValidationException(errRangeIn);
                }
            }
            return true;
        }

        private int compareToRaMin(Object val) {
            return getFloat(val).compareTo(getRaMinValue());
        }

        private int compareToRaMax(Object val) {
            return getFloat(val).compareTo(getRaMaxValue());
        }

        private int compareToDecMin(Object val) {
            return getFloat(val).compareTo(getDecMinValue());
        }

        private int compareToDecMax(Object val) {
            return getFloat(val).compareTo(getDecMaxValue());
        }
}
