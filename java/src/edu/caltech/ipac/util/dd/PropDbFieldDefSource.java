package edu.caltech.ipac.util.dd;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.action.Prop;

import java.util.Properties;
/**
 * User: roby
 * Date: Feb 10, 2010
 * Time: 12:06:50 PM
 */


/**
 * @author Trey Roby
 */
public class PropDbFieldDefSource implements FieldDefSource {

    private final String propName;
    private String id;
    private final Properties alternatePdb;

    public PropDbFieldDefSource(String propName) {
        this(propName,null);
    }

    public PropDbFieldDefSource(String propName, Properties alternatePdb) {
        this.propName = propName;
        this.alternatePdb= alternatePdb;
    }

    public String getDataType() {
        return Prop.getDataType(propName, alternatePdb);
    }

    public String getId() {
        return AppProperties.getProperty(propName + "." + ActionConst.ID, null, alternatePdb);
    }

    public String getName() {
        return propName;
    }

    public String getTitle() {
        String t=AppProperties.getProperty(propName + "." + ActionConst.TITLE, null, alternatePdb);
        if (t==null) t=AppProperties.getProperty(propName + "." + ActionConst.NAME, null, alternatePdb);
        if (t==null) t= propName;
        return t;
    }

    public String getDesc() {
        return AppProperties.getProperty(propName + "." + ActionConst.LONG_DESCRIPTION, null, alternatePdb);
    }

    public String getShortDesc() {
        return AppProperties.getProperty(propName + "." + ActionConst.SHORT_DESCRIPTION, null, alternatePdb);
    }

    public String getHowToEnableTip() {
        return AppProperties.getProperty( propName +"."+ ActionConst.HOW_TO_ENABLE_TIP, null, alternatePdb);
    }

    public String getErrorDescUnits() {
        return AppProperties.getProperty( propName +"."+ ActionConst.ERROR_DESCRIPTION_UNITS, null, alternatePdb);
    }


    public String getIcon() {
        String val= AppProperties.getProperty(propName + "." + ActionConst.ICON, null, alternatePdb);
        if (val==null) val= AppProperties.getProperty(propName + "." + ActionConst.SMALL_ICON, null, alternatePdb);
        return val;
    }

    public String getOptional() {
        return AppProperties.getProperty(propName + "." + ActionConst.OPTIONAL, null, alternatePdb);
    }

    public String getDefaultValue() {
        String val= AppProperties.getProperty(propName + "." + ActionConst.DEFAULT, null, alternatePdb);
        if (val==null) val= AppProperties.getProperty(propName + "." + ActionConst.RADIO_VALUE, null, alternatePdb);
        return val;
    }

    public String getPreferenceKey() {
        return AppProperties.getProperty(propName + "." + ActionConst.PREFERENCE_KEY, null, alternatePdb);
    }

    public String getURL() {
        return AppProperties.getProperty( propName +"."+ ActionConst.URL, null, alternatePdb);
    }

    public String getErrMsg() {
        return AppProperties.getProperty( propName +"."+ ActionConst.ERROR_DESCRIPTION, null, alternatePdb);
    }

    public String getMask() {
        return AppProperties.getProperty(propName + "." +ActionConst.MASK, null, alternatePdb);
    }

    public String isNullAllow() {
        return AppProperties.getProperty(propName + "." + ActionConst.NULL_ALLOWED, null, alternatePdb);
    }

    public String isEditable() {
        return AppProperties.getProperty(propName + "." + ActionConst.IS_EDITABLE, null, alternatePdb);
    }

    public String isTextImmutable() {
        return AppProperties.getProperty(propName + "." + ActionConst.TEXT_IMMUTABLE, null, alternatePdb);
    }

    public String getMaxWidth() {
        String val= AppProperties.getProperty(propName + "." + ActionConst.MAX_WIDTH, null, alternatePdb);
        if (val==null) val= AppProperties.getProperty(propName + "." + ActionConst.MAX_LENGTH, null, alternatePdb);
        return val;
    }

    public String getIncrement() {
        return AppProperties.getProperty(propName + "." + ActionConst.INCREMENT, null, alternatePdb);
    }

    public String getPreferWidth() {
        return AppProperties.getProperty(propName + "." + ActionConst.PREFER_WIDTH, null, alternatePdb);
    }

    public String getCached() {
        return AppProperties.getProperty(propName + "." + ActionConst.CACHED, null, alternatePdb);
    }

    public String getSize() {
        return AppProperties.getProperty(propName + "." + ActionConst.SIZE, null, alternatePdb);
    }

    public String getSciNotation() {
        return AppProperties.getProperty(propName + "." + ActionConst.SCIENTIFIC_ALLOWED, null, alternatePdb);
    }

    public String getMaxBoundType() {
        return AppProperties.getProperty(propName + "." + ActionConst.MAX_BOUND_TYPE, null, alternatePdb);
    }

    public String getMinBoundType() {
        return AppProperties.getProperty(propName + "." + ActionConst.MIN_BOUND_TYPE, null, alternatePdb);
    }

    public String getPrecision() {
        return AppProperties.getProperty(propName + "." + ActionConst.PRECISION, null, alternatePdb);
    }

    public String getValue() {
        return AppProperties.getProperty(propName + "." + ActionConst.VALUE, null, alternatePdb);
    }

    public String getMinValue() {
        return AppProperties.getProperty(propName + "." + ActionConst.MIN, null, alternatePdb);
    }

    public String getMaxValue() {
        return AppProperties.getProperty(propName + "." + ActionConst.MAX, null, alternatePdb);
    }

    public String isSelected() {
        String v= AppProperties.getProperty(propName + "." + ActionConst.DEFAULT, null, alternatePdb);
        if (v==null) v= AppProperties.getProperty(propName + "." + ActionConst.SELECTED, null, alternatePdb);
        return v;
    }

    public String getPattern() {
        return AppProperties.getProperty(propName + "." + ActionConst.PATTERN, null, alternatePdb);
    }

    public String getAccelerator() {
        return AppProperties.getProperty(propName + "." + ActionConst.ACCELERATOR, null, alternatePdb);
    }

    public String getMnemonic() {
        return AppProperties.getProperty(propName + "." + ActionConst.MNEMONIC, null, alternatePdb);
    }

    public String getValidationType() {
        return AppProperties.getProperty(propName + "." + ActionConst.VALIDATION, null, alternatePdb);
    }

    public String[] getItems() {
        return Prop.getItems(propName, alternatePdb);
    }

    public String getItemName(String item) {
        return propName + "." + item;

    }

    public String getItemTitle(String item) {
        String t=AppProperties.getProperty(propName +"." + item +  "." + ActionConst.TITLE, null, alternatePdb);
        if (t==null) t=AppProperties.getProperty(propName + "." + item + "." + ActionConst.NAME, null, alternatePdb);
        if (t==null) t= propName+"." + item;
        return t;
    }




    public String getItemIntValue(String item) {
        return AppProperties.getProperty(propName + "." + item + "." + ActionConst.INT_VALUE, null, alternatePdb);
    }

    public String getUnits() {
        return AppProperties.getProperty(propName + "." + ActionConst.UNITS, null, alternatePdb);
    }

    public String getInternalUnits() {
        return AppProperties.getProperty(propName + "." + ActionConst.INTERNAL_UNITS, null, alternatePdb);
    }

    public String getOrientation() {
        return AppProperties.getProperty(propName + "." + ActionConst.ORIENTATION, null, alternatePdb);
    }

    public String getItemShortDesc(String item) {
        return AppProperties.getProperty(propName + "."+ item + "."+ ActionConst.SHORT_DESCRIPTION, null, alternatePdb);
    }

    public String getItemMaxBoundType(String item) {
        return AppProperties.getProperty(propName + "."+ item + "."+ ActionConst.MAX_BOUND_TYPE, null, alternatePdb);
    }

    public String getItemMinBoundType(String item) {
        return AppProperties.getProperty(propName + "."+ item + "."+ ActionConst.MIN_BOUND_TYPE, null, alternatePdb);
    }

    public String getItemMinValue(String item) {
        return AppProperties.getProperty(propName + "."+ item +"." + ActionConst.MIN, null, alternatePdb);
    }

    public String getItemMaxValue(String item) {
        return AppProperties.getProperty(propName + "."+ item +"." + ActionConst.MAX, null, alternatePdb);
    }

    public Properties getPDB() { return alternatePdb; }
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
