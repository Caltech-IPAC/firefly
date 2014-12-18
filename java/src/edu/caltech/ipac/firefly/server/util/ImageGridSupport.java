package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Mar 16, 2012
 * Time: 3:45:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageGridSupport {
    /**
     * COLUMN basic IPAC table columns for ImageGrid.
     */
    public enum COLUMN {TYPE, GROUP, DESC, THUMBNAIL}

    /**
     * ATTRIBUTE ImageGrid's IPAC table attribute fields:
     *   -column definitions: DATA_TYPE_COLUMN, String DESCRIPTION_COLUMN
     *   -grouping: GROUPING, GROUPING_COLUMN
     *   -grid color: GRID_BACKGROUND, GRID_FOREGROUND
     *   -auto-resize column's total: COLUMNS
     *   -info column definition: INFO
     *   -source id definition: SOURCE_ID_COLUMN
     *   -JPEG: JPEG_SELECTION_HILITE, JPEG_SELECTION_DOUBLE_CLICK, FULL_SIZE_URL_COLUMN, THUMBNAIL_URL_COLUMN
     */
    public enum ATTRIBUTE {COLUMNS, DATA_TYPE_COLUMN, DESCRIPTION_COLUMN, ALL_EVENTWORKER_COLUMN, EVENTWORKER_COLUMN, 
        FULL_SIZE_URL_COLUMN, GROUPING, GROUPING_COLUMN, GRID_BACKGROUND, GRID_FOREGROUND, INFO,
        JPEG_SELECTION_DOUBLE_CLICK, JPEG_SELECTION_HILITE, SOURCE_ID_COLUMN, THUMBNAIL_URL_COLUMN}


    /**
     * SHOW_LABEL the position of group label.
     * top=top, on=left, off=disable
     */
    public enum SHOW_LABEL {ON, OFF, TOP}

    /**
     * GROUP_BY group images by the first word, alphabetic prefix, numeric prefix on the string in GROUPING_COLUMN (or
     * in DESCRIPTION_COLUMN if GROUPING_COLUMN is absent).
     */
    public enum GROUP_BY {firstWord, alphabeticPrefix, numericPrefix}

    private static ArrayList<COLUMN> columns = null;



// --------------------------------- Create basic IPAC table ---------------------------------

    /**
     * Create basic ImageGrid IPAC table.
     * @return IPAC Table in DataGroup format
     **/
    public static DataGroup createBasicDataGroup() {
        return createBasicDataGroup( createBasicDataDefinitions(), true );
    }

    /**
     * Create basic ImageGrid IPAC table.
     * @param dataDefinitions columns defined for the specific IPAC table
     * @param grouped enable grouped
     * @return IPAC Table in DataGroup format
     **/
    public static DataGroup createBasicDataGroup(ArrayList<DataType> dataDefinitions, boolean grouped) {
        DataGroup table = new DataGroup("ImageGrid Table", dataDefinitions);
        addDataGroupAttribute(table, "datatype", "fitshdr");
        addDataGroupAttribute(table, "fixlen", "T");
        addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.GRID_BACKGROUND, "#f6f6f6");
        addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.GRID_FOREGROUND, "#000");
        addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.DATA_TYPE_COLUMN, COLUMN.TYPE.toString());
        addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.DESCRIPTION_COLUMN, COLUMN.DESC.toString());
        addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.FULL_SIZE_URL_COLUMN, COLUMN.THUMBNAIL.toString());
        addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.THUMBNAIL_URL_COLUMN, COLUMN.THUMBNAIL.toString());
        if (grouped) {
            addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.GROUPING_COLUMN, COLUMN.GROUP.toString());
            addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.GROUPING, createBasicGroupingParams(true, SHOW_LABEL.TOP, null));
        }
        return table;
    }


// --------------------------------- Create basic IPAC table columns ---------------------------------

    /**
     * Create DataDefinitions for ImageGrid IPAC table.
     * @return an ArrayList of DataType with TYPE, GROUP, DESC, THUMBNAIL columns
     */
    public static ArrayList<DataType> createBasicDataDefinitions() {
        ArrayList<DataType> dataDefs = new ArrayList<DataType>();

        for (COLUMN key: getBasicWebPlotRequestColumns()) {dataDefs.add(new DataType(key.toString(), String.class));}

        dataDefs.get(getBasicWebPlotRequestColumns().indexOf(COLUMN.THUMBNAIL)).getFormatInfo().setWidth(500);
        dataDefs.get(getBasicWebPlotRequestColumns().indexOf(COLUMN.TYPE)).getFormatInfo().setWidth(8);
        dataDefs.get(getBasicWebPlotRequestColumns().indexOf(COLUMN.DESC)).getFormatInfo().setWidth(40);
        return dataDefs;
    }


// --------------------------------- Create basic ImageGrid grouping parameters ---------------------------------

    /**
     * Create a parameter string that tells an ImageGrid how to group images.
     * @param showInOneRow determine to show a group of images in a row
     * @param label determine how to show a group label
     * @param groupBy determine how to group images
     * @return a string of grouping parameters
     */
    public static String createBasicGroupingParams(Boolean showInOneRow, SHOW_LABEL label, GROUP_BY groupBy) {
        String retval="";

        if (showInOneRow != null) {
            retval="show_in_a_row="+(showInOneRow?"T":"F"); //image grid can show grouped images in the same row.
        }
        if (label != null) {
            if (retval.length()>0) retval= retval.concat("&");
            retval= retval.concat("show_label="+label.toString());
        }
        if (groupBy != null) {
            if (retval.length()>0) retval= retval.concat("&");
            retval= retval.concat("groupBy="+groupBy.toString());
        }
        return retval;
    }


// --------------------------------- add IPAC table attribute ---------------------------------

    /**
     * Add an attribute to an ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param key an attribute key
     * @param value an attribute value
     */
    public static void addDataGroupAttribute(DataGroup dg, String key, String value) {
        dg.addAttributes(new DataGroup.Attribute(key, value));
    }

    /**
     * Add an attribute to an ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param attribute an attribute key
     * @param value an attribute value
     */
    public static void addDataGroupAttribute(DataGroup dg, ATTRIBUTE attribute, String value) {
        dg.addAttributes(new DataGroup.Attribute(attribute.toString(), value));
    }

    /**
     * Add an attribute to an ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param attribute an attribute key
     */
    public static void addDataGroupAttribute(DataGroup dg, ATTRIBUTE attribute) {
        dg.addAttributes(new DataGroup.Attribute(attribute.toString(), attribute.toString()));
    }

// --------------------------------- add JPEG image to IPAC table ---------------------------------

    /**
     * Add an JPEG image into ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param url an image file URL
     * @param desc an image description
     * @throws Exception any exception
     */
    public static void addJpegRequest(DataGroup dg, String url, String desc) throws Exception {
        addJpegRequest(dg, url, desc, "");
    }

    /**
     * Add an JPEG image into ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param url an image file URL
     * @param desc an image description
     * @param groupName name of specific images group
     * @throws Exception any exception
     */
    public static void addJpegRequest(DataGroup dg, String url, String desc, String groupName) throws Exception {
        DataObject row = new DataObject(dg);
        row.setDataElement(dg.getDataDefintion(COLUMN.TYPE.toString()), "jpg");
        row.setDataElement(dg.getDataDefintion(COLUMN.THUMBNAIL.toString()), url);
        row.setDataElement(dg.getDataDefintion(COLUMN.DESC.toString()), desc);
        row.setDataElement(dg.getDataDefintion(COLUMN.GROUP.toString()), groupName);
        dg.add(row);
    }


// --------------------------------- add WebPlotRequest to IPAC table ---------------------------------

    /**
     * Add an WebPlotRequest into ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param wpReq a WebPlotRequest that represents a specific FITS
     * @throws Exception any exception
     */
    public static void addWebPlotRequest(DataGroup dg, WebPlotRequest wpReq) throws Exception {
        addWebPlotRequest(dg, wpReq, "");
    }

    /**
     * Add a WebPlotRequest into ImageGrid IPAC table.
     * @param dg an ImageGrid IPAC table
     * @param wpReq a WebPlotRequest that represents a specific FITS
     * @param groupName name of specific images group
     * @throws Exception any exception
     */
    public static void addWebPlotRequest(DataGroup dg, WebPlotRequest wpReq, String groupName) throws Exception {
        DataObject row = new DataObject(dg);
        row.setDataElement(dg.getDataDefintion(COLUMN.TYPE.toString()), "req");
        row.setDataElement(dg.getDataDefintion(COLUMN.THUMBNAIL.toString()), wpReq.toString());
        row.setDataElement(dg.getDataDefintion(COLUMN.DESC.toString()), wpReq.getTitle());
        if (groupName!=null && groupName.length()>0)
            row.setDataElement(dg.getDataDefintion(COLUMN.GROUP.toString()), groupName);
        dg.add(row);
    }



// --------------------------------- Private static methods ---------------------------------
    private static ArrayList<COLUMN> getBasicWebPlotRequestColumns() {
        if (columns==null) {
            columns = new ArrayList<COLUMN>();
            columns.addAll(Arrays.asList(COLUMN.TYPE, COLUMN.GROUP, COLUMN.DESC, COLUMN.THUMBNAIL));
        }
        return columns;
    }

}
