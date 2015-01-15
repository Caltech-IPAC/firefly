/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;

/**
 * User: roby
 * Date: Feb 10, 2010
 * Time: 12:05:06 PM
 */


/**
 * @author Trey Roby
 */
public interface FieldDefSource {

    String getDataType();

    String getId();
    String getName();
    String getTitle();
    String getDesc();
    String getShortDesc();
    String getHowToEnableTip();
    String getErrorDescUnits();
    String getIcon();

    String getDefaultValue();
    String getPreferenceKey();
    String getURL();
    String getErrMsg();
    String getMask();
    String isNullAllow();
    String isEditable();
    String isTextImmutable();
    String getMaxWidth();
    String getPreferWidth();
    String getCached();
    String getSize();

    String getSciNotation();
    String getMaxBoundType();
    String getMinBoundType();

    String getPrecision();
    String getValue();
    String isSelected();
    String getMinValue();
    String getMaxValue();
    String getIncrement();

    String getPattern();
    String getAccelerator();
    String getMnemonic();

    String getUnits();
    String getInternalUnits();

    String getOptional();

    String getOrientation();

    String[] getItems();

    String getItemName(String item);
    String getItemTitle(String item);
    String getItemIntValue(String item);

    String getValidationType();

    String getItemShortDesc(String item);
    String getItemMaxBoundType(String item);
    String getItemMinBoundType(String item);
    String getItemMinValue(String item);
    String getItemMaxValue(String item);

}

