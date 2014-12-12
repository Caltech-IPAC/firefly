package edu.caltech.ipac.util.dd;

import edu.caltech.ipac.util.action.ActionConst;

import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Feb 10, 2010
 * Time: 1:31:38 PM
 */


/**
 * @author Trey Roby
 */
public class MappedFieldDefSource implements FieldDefSource, UIComponent {

    private Map<String,String> map= new HashMap<String,String>();

    protected MappedFieldDefSource() {}

    public MappedFieldDefSource(String fieldDefType) {
        set(ActionConst.DATA_TYPE, fieldDefType);
    }


    public MappedFieldDefSource(String name, String fieldDefType) {
        set(ActionConst.DATA_TYPE, fieldDefType);
        set(ActionConst.NAME, name);
    }

    public MappedFieldDefSource(MappedFieldDefSource fds) {
        map.putAll(fds.map);
    }

    public void set(String key, String value) {
        map.put(key,value);
    }


    protected String get(String key) { return map.get(key); }

    public String getDataType() {
        return map.get(ActionConst.DATA_TYPE);
    }

    public String getId() {
        return map.get(ActionConst.ID);
    }

    public String getName() {
        return map.get(ActionConst.NAME);
    }

    public String getTitle() {
        return map.get(ActionConst.TITLE);
    }

    public String getDesc() {
        return map.get(ActionConst.LONG_DESCRIPTION);
    }

    public String getShortDesc() {
        return map.get(ActionConst.SHORT_DESCRIPTION);
    }

    public String getHowToEnableTip() {
        return map.get(ActionConst.HOW_TO_ENABLE_TIP);
    }

    public String getErrorDescUnits() {
        return map.get(ActionConst.ERROR_DESCRIPTION_UNITS);
    }

    public String getIcon() {
        return map.get(ActionConst.ICON);
    }

    public String getDefaultValue() {
        return map.get(ActionConst.DEFAULT);
    }

    public String getPreferenceKey() {
        return map.get(ActionConst.PREFERENCE_KEY);
    }

    public String getURL() {
        return map.get(ActionConst.URL);
    }

    public String getErrMsg() {
        return map.get(ActionConst.ERROR_DESCRIPTION);
    }

    public String getMask() {
        return map.get(ActionConst.MASK);
    }

    public String isNullAllow() {
        return map.get(ActionConst.NULL_ALLOWED);
    }

    public String isEditable() { return map.get(ActionConst.IS_EDITABLE); }

    public String isTextImmutable() {
        return map.get(ActionConst.TEXT_IMMUTABLE);
    }

    public String getMaxWidth() {
        return map.get(ActionConst.MAX_WIDTH);
    }

    public String getIncrement() {
        return map.get(ActionConst.INCREMENT);
    }

    public String getPreferWidth() {
        return map.get(ActionConst.PREFER_WIDTH);
    }

    public String getCached() {
        return map.get(ActionConst.CACHED);
    }

    public String getSize() {
        return map.get(ActionConst.SIZE);
    }

    public String getSciNotation() {
        return map.get(ActionConst.SCIENTIFIC_ALLOWED);
    }

    public String getMaxBoundType() {
        return map.get(ActionConst.MAX_BOUND_TYPE);
    }

    public String getMinBoundType() {
        return map.get(ActionConst.MIN_BOUND_TYPE);
    }

    public String getPrecision() {
        return map.get(ActionConst.PRECISION);
    }

    public String getValue() { return map.get(ActionConst.VALUE); }

    public String getMinValue() { return map.get(ActionConst.MIN); }

    public String getMaxValue() { return map.get(ActionConst.MAX); }

    public String isSelected() {
        String v= map.get(ActionConst.DEFAULT);
        if (v==null) v= map.get(ActionConst.SELECTED);
        return v;
    }

    public String getPattern() { return map.get(ActionConst.PATTERN); }

    public String getAccelerator() { return map.get(ActionConst.ACCELERATOR); }
    public String getMnemonic() { return map.get(ActionConst.MNEMONIC); }
    public String getUnits() { return map.get(ActionConst.UNITS); }

    public String getInternalUnits() { return map.get(ActionConst.INTERNAL_UNITS); }

    public String getOptional() { return map.get(ActionConst.OPTIONAL); }

    public String getOrientation() { return map.get(ActionConst.ORIENTATION); }

    public String getValidationType() { return map.get(ActionConst.VALIDATION); }

    public String[] getItems() {
        String retval[]= null;
        String items= map.get(ActionConst.ITEMS);
        if (items!=null) {
            retval= items.split(" ");
        }
        return retval;
    }

    public String getItemName(String item) {
        return item;
    }

    public String getItemTitle(String item) {
        return map.get(item +"." +ActionConst.TITLE);
    }

    public String getItemIntValue(String item) {
        return map.get(item +"." +ActionConst.INT_VALUE);
    }

    public String getItemShortDesc(String item) {
        return map.get(item +"." +ActionConst.SHORT_DESCRIPTION);
    }

    public String getItemMaxBoundType(String item) {
        return map.get(item +"." +ActionConst.MAX_BOUND_TYPE);
    }

    public String getItemMinBoundType(String item) {
        return map.get(item +"." +ActionConst.MIN_BOUND_TYPE);
    }

    public String getItemMinValue(String item) {
        return map.get(item +"." +ActionConst.MIN);
    }

    public String getItemMaxValue(String item) {
        return map.get(item +"." +ActionConst.MAX);
    }

    public String getKey() { return null; }
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
