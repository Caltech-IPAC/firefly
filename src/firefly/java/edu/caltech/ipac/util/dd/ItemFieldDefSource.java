/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: Sep 23, 2010
 * Time: 3:00:00 PM
 */


/**
 * @author Trey Roby
 */
public class ItemFieldDefSource implements FieldDefSource {

    private String itemStr;
    private FieldDefSource parent;

    public ItemFieldDefSource(String itemStr, FieldDefSource parent) {
        this.itemStr= itemStr;
        this.parent= parent;
    }

    public String getDataType() { return null; }

    public String getId() {
        return getName();
    }

    public String getName() { return parent.getItemName(itemStr); }

    public String getTitle() { return parent.getItemTitle(itemStr); }
    public String getDesc() { return null; }

    public String getShortDesc() { return parent.getItemShortDesc(itemStr); }

    public String getPreferenceKey() { return null; }
    public String getHowToEnableTip() { return null;  }
    public String getErrorDescUnits() { return null; }
    public String getIcon() { return null; }
    public String getDefaultValue() { return null; }
    public String getURL() { return null; }
    public String getErrMsg() { return null; }
    public String getMask() { return null; }
    public String isNullAllow() { return null; }
    public String isEditable() { return null; }
    public String isTextImmutable() { return null; }
    public String getMaxWidth() { return null; }
    public String getPreferWidth() { return null; }
    public String getCached() { return null; }
    public String getSize() { return null; }
    public String getSciNotation() { return null; }

    public String getMaxBoundType() { return parent.getItemMaxBoundType(itemStr); }
    public String getMinBoundType() { return parent.getItemMinBoundType(itemStr); }
    public String getPrecision() { return null; }

    public String getValue() { return null; }

    public String isSelected() { return null; }

    public String getMinValue() { return parent.getItemMinValue(itemStr); }
    public String getMaxValue() { return parent.getItemMaxValue(itemStr); }

    public String getIncrement() { return null; }
    public String getPattern() { return null; }
    public String getAccelerator() { return null; }
    public String getMnemonic() { return null; }
    public String getUnits() { return null; }
    public String getInternalUnits() { return null; }
    public String getOptional() { return null; }
    public String getOrientation() { return null; }
    public String[] getItems() { return new String[0]; }

    public String getItemName(String item) { return null; }
    public String getItemTitle(String item) { return null; }

    public String getItemIntValue(String item) { return parent.getItemIntValue(itemStr); }

    public String getValidationType() { return null; }

    public String getItemShortDesc(String item) { return null; }

    public String getItemMaxBoundType(String item) { return null; }

    public String getItemMinBoundType(String item) { return null; }

    public String getItemMinValue(String item) { return null; }

    public String getItemMaxValue(String item) { return null; }

}

