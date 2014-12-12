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
