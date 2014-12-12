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
