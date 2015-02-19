/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;

import edu.caltech.ipac.util.StringUtils;


/**
 * This abstract class provides validation and definition of a range field.
 * @version $Id: RangeFieldDef.java,v 1.1 2010/09/28 17:58:39 roby Exp $
 */
public abstract class RangeFieldDef extends StringFieldDef {

    public static final String INCLUSIVE = "inclusive";
    public static final String EXCLUSIVE = "exclusive";
    protected static final String UNDEFINED = "undefined";

    /**
	 * This field defines the minimum value range type.  
	 * It can be one of the followings:
	 * INCLUSIVE
	 * EXCLUSIVE 
	 */
	private String minBoundType= UNDEFINED ;

    /**
	 * This field defines the maximum value range type.  
	 * It can be one of the followings:
	 * INCLUSIVE
	 * EXCLUSIVE 
	 */
	private String maxBoundType= UNDEFINED ;


    public RangeFieldDef() {}

    public RangeFieldDef(String name) {
        super(name);
    }

    public boolean validate(Object aValue) throws ValidationException {

        if (StringUtils.isEmpty(aValue)) {
            if (isNullAllow()) {
                return true;
            }
            else {
                throw new ValidationException(getErrMsg());
            }
        }

        if (!isValidForm(aValue)) {
            throw new ValidationException(getErrMsg());
        }

        if (minBoundType != null && !minBoundType.equals(UNDEFINED) ) {
            int v;
            try {
                v = compareToMin(aValue);
            } catch (Exception e) {
                throw new ValidationException(getErrMsg());
            }
            if (v < 0) {
                throw new ValidationException(getErrMsg());
            } else if (v == 0) {
                if (minBoundType.equals(EXCLUSIVE)){
                    throw new ValidationException(getErrMsg());
                }
            }
        }

        if (maxBoundType != null && !maxBoundType.equals(UNDEFINED) ) {
            int v;
            try {
                v = compareToMax(aValue);
            } catch (Exception e) {
                throw new ValidationException(getErrMsg());
            }
            if (v > 0) {
                throw new ValidationException(getErrMsg());
            } else if (v == 0) {
                if (maxBoundType.equals(EXCLUSIVE)){
                    throw new ValidationException(getErrMsg());
                }
            }
        }
        return true;
    }


    public String getMinBoundType() {
        return minBoundType;
    }

    /**
     * Set the minimum boundary type of this field.  It can be either inclusive or exclusive.
     * If this property is not given, then it implies this field has no mininum boundary.
     * @param minBoundType  either {@link #INCLUSIVE} or {@link #EXCLUSIVE}
     */
    public void setMinBoundType(String minBoundType) {
        this.minBoundType = minBoundType==null ? UNDEFINED : minBoundType;
    }

    public String getMaxBoundType() {
        return maxBoundType;
    }

    /**
     * Set the maximum boundary type of this field.  It can be either inclusive or exclusive.
     * If this property is not defined, then it implies this field has no maximum boundary.
     * @param maxBoundType  either {@link #INCLUSIVE} or {@link #EXCLUSIVE}
     */
    public void setMaxBoundType(String maxBoundType) {
        this.maxBoundType = maxBoundType==null ? UNDEFINED : maxBoundType;
    }

    public abstract int compareToMin(Object val);

    public abstract int compareToMax(Object val);

    public abstract boolean isValidForm(Object val);

    public abstract String getDefaultMask();
}