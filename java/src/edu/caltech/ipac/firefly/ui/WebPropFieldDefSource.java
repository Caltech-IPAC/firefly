package edu.caltech.ipac.firefly.ui;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.dd.FieldDefSource;
/**
 * User: roby
 * Date: Feb 10, 2010
 * Time: 12:06:50 PM
 */


/**
 * @author Trey Roby
 */
public class WebPropFieldDefSource implements FieldDefSource {

    private final String propName;
    private final WebAppProperties wap;

    public WebPropFieldDefSource(String propName) {
        this.propName = propName;
        wap = Application.getInstance().getProperties();
    }


    public String getDataType() {
        return WebProp.getDataType(propName);
    }

    public String getId() {
        return wap.getProperty(propName + "." + ActionConst.ID);
    }


    public String getName() {
        return propName;
    }

    public String getTitle() {
        return WebProp.getTitle(propName);
    }

    public String getDesc() {
        return wap.getProperty(propName + "." + ActionConst.LONG_DESCRIPTION);
    }

    public String getShortDesc() {
        return wap.getProperty(propName + "." + ActionConst.SHORT_DESCRIPTION);
    }

    public String getHowToEnableTip() {
        return wap.getProperty( propName +"."+ ActionConst.HOW_TO_ENABLE_TIP, null);
    }

    public String getErrorDescUnits() {
        return wap.getProperty( propName +"."+ ActionConst.ERROR_DESCRIPTION_UNITS);
    }


    public String getIcon() {
        return wap.getProperty(propName + "." + ActionConst.ICON);
    }

    public String getOptional() {
        return wap.getProperty(propName + "." + ActionConst.OPTIONAL);
    }

    public String getDefaultValue() {
        return wap.getProperty(propName + "." + ActionConst.DEFAULT);
    }

    public String getPreferenceKey() {
        return wap.getProperty(propName + "." + ActionConst.PREFERENCE_KEY);
    }

    public String getURL() {
        return wap.getProperty( propName +"."+ ActionConst.URL, null);
    }

    public String getErrMsg() {
        return WebProp.getErrorDescription(propName);
    }

    public String getMask() {
        return wap.getProperty(propName + "." +ActionConst.MASK);
    }

    public String isNullAllow() {
        return wap.getProperty(propName + "." + ActionConst.NULL_ALLOWED);
    }

    public String isEditable() {
        return wap.getProperty(propName + "." + ActionConst.IS_EDITABLE);
    }

    public String isTextImmutable() {
        return wap.getProperty(propName + "." + ActionConst.TEXT_IMMUTABLE);
    }

    public String getMaxWidth() {
        return wap.getProperty(propName + "." + ActionConst.MAX_WIDTH);
    }

    public String getIncrement() {
        return wap.getProperty(propName + "." + ActionConst.INCREMENT);
    }

    public String getPreferWidth() {
        return wap.getProperty(propName + "." + ActionConst.PREFER_WIDTH);
    }

    public String getCached() {
        return wap.getProperty(propName + "." + ActionConst.CACHED);
    }

    public String getSize() {
        return wap.getProperty(propName + "." + ActionConst.SIZE);
    }

    public String getSciNotation() {
        return wap.getProperty(propName + "." + ActionConst.SCIENTIFIC_ALLOWED);
    }

    public String getMaxBoundType() {
        return wap.getProperty(propName + "." + ActionConst.MAX_BOUND_TYPE);
    }

    public String getMinBoundType() {
        return wap.getProperty(propName + "." + ActionConst.MIN_BOUND_TYPE);
    }

    public String getPrecision() {
        return wap.getProperty(propName + "." + ActionConst.PRECISION);
    }
    public String getValue() {
        return wap.getProperty(propName + "." + ActionConst.VALUE);
    }

    public String isSelected() {
        return wap.getProperty(propName + "." + ActionConst.SELECTED);
    }

    public String getMinValue() {
        return wap.getProperty(propName + "." + ActionConst.MIN);
    }

    public String getMaxValue() {
        return wap.getProperty(propName + "." + ActionConst.MAX);
    }

    public String getPattern() {
        return wap.getProperty(propName + "." + ActionConst.PATTERN);
    }

    public String getAccelerator() {
        return wap.getProperty(propName + "." + ActionConst.ACCELERATOR);
    }

    public String getMnemonic() {
        return wap.getProperty(propName + "." + ActionConst.MNEMONIC);
    }

    public String getValidationType() {
        return wap.getProperty(propName + "." + ActionConst.VALIDATION);
    }

    public String[] getItems() {
        return WebProp.getItems(propName);
    }

    public String getItemName(String item) {
        return propName + "." + item;
    }

    public String getItemTitle(String item) {
        String t= wap.getProperty(propName +"." + item +  "." + ActionConst.TITLE);
        if (t==null) t=wap.getProperty(propName + "." + item + "." + ActionConst.NAME);
        if (t==null) t= propName+"." + item;
        return t;
    }

    public String getItemIntValue(String item) {
        return wap.getProperty(propName + "." + itemPart(item) + ActionConst.INT_VALUE);
    }

    public String getUnits() {
        return wap.getProperty(propName + "." + ActionConst.UNITS);
    }

    public String getInternalUnits() {
        return wap.getProperty(propName + "." + ActionConst.INTERNAL_UNITS);
    }

    public String getOrientation() {
        return wap.getProperty(propName + "." + ActionConst.ORIENTATION);
    }

    public String getItemShortDesc(String item) {
        return wap.getProperty(propName + "."+ item + "."+ ActionConst.SHORT_DESCRIPTION);
    }
    
    public String getItemMaxBoundType(String item) {
        return wap.getProperty(propName + "."+ itemPart(item) + ActionConst.MAX_BOUND_TYPE);
    }

    public String getItemMinBoundType(String item) {
        return wap.getProperty(propName + "."+ itemPart(item) + ActionConst.MIN_BOUND_TYPE);
    }

    public String getItemMinValue(String item) {
        return wap.getProperty(propName + "."+ itemPart(item) + ActionConst.MIN);
    }

    public String getItemMaxValue(String item) {
        return wap.getProperty(propName + "."+ itemPart(item) + ActionConst.MAX);
    }
    
    private String itemPart(String item) {
        if (item==null)  return "";
        else  return item + ".";
    }
}

