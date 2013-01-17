package edu.caltech.ipac.hydra.server.query;
/**
 * User: roby
 * Date: 1/11/12
 * Time: 2:41 PM
 */


import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.List;

/**
 * @author Trey Roby
 */
public class WiseGrid {

    private enum Level {L1, L3}

    private static DataType[] columns = new DataType[]{
            new DataType("type", String.class),
            new DataType("thumb_nail", String.class),
            new DataType("desc", String.class)
    };


    public static void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        meta.setAttribute("GRID_THUMBNAIL_COLUMN", "thumbnail");
        meta.setAttribute("PLOT_REQUEST_COLUMN", "req");
    }

    public static DataGroup getGrid(ServerRequest request, DataGroup inTable, TableMeta meta) throws DataAccessException {

//        int bandAry[]= {1,2,3};

        columns[1].getFormatInfo().setWidth(500);

        DataGroup table = new DataGroup("my dummy table", columns);

        // create table headers
        table.addAttributes(new DataGroup.Attribute("datatype", "fitshdr"));
        table.addAttributes(new DataGroup.Attribute("fixlen", "T"));
        table.addAttributes(new DataGroup.Attribute("DATA_TYPE_COLUMN", "type"));
        table.addAttributes(new DataGroup.Attribute("DESCRIPTION_COLUMN", "desc"));
        table.addAttributes(new DataGroup.Attribute("FULL_SIZE_URL_COLUMN", "thumb_nail"));
        table.addAttributes(new DataGroup.Attribute("GRID_BACKGROUND", "#e8f3f5"));
        table.addAttributes(new DataGroup.Attribute("GRID_FOREGROUND", "#000"));
        table.addAttributes(new DataGroup.Attribute("JPEG_SELECTION_HILITE", "F"));
        table.addAttributes(new DataGroup.Attribute("JPEG_SELECTION_DOUBLE_CLICK", "T"));
        table.addAttributes(new DataGroup.Attribute("THUMBNAIL_URL_COLUMN", "thumb_nail"));
        // add the rest of the headers..

        // add a row for every cutout url call
        //

        int bandCnt = 4;
        if (request.containsParam("band")) {
            bandCnt = request.getParam("band").split(",").length;
        }


        DataObject rowData;
        int band;

        Level level = inTable.containsKey("scan_id") ? Level.L1 : Level.L3;

        if (bandCnt == 1) {
            table.addAttributes(new DataGroup.Attribute("COLUMNS", 5 + ""));
        } else {
            table.addAttributes(new DataGroup.Attribute("COLUMNS", bandCnt + ""));
        }


        WebPlotRequest r;

        for (int row = 0; (row < inTable.size()); row++) {

            rowData = inTable.get(row);
            double ra = getRA(inTable, rowData);
            double dec = getDec(inTable, rowData);

            band = (Integer) rowData.getDataElement("band");
            if (level == Level.L1) {
                r = makeRequest(level, band,
                                (String) rowData.getDataElement("scan_id"),
                                (Integer) rowData.getDataElement("frame_num"),
                                ra, dec, request, meta);

            } else {
                r = makeRequest(level,  band,
                                (String) rowData.getDataElement("coadd_id"),
                                ra, dec, request, meta);

            }
            DataObject dOjb = getDataElement(table, r);
            table.add(dOjb);
        }

        if (table.size() == 0) {
            table.addAttributes(new DataGroup.Attribute("INFO", "Image data not found!"));
        }
        return table;

    }

    public static double getRA(DataGroup group, DataObject rowData) {
        double ra;
        try {
            if (group.containsKey("ra_obj")) ra = (Double) rowData.getDataElement("ra_obj");
            else ra = (Double) rowData.getDataElement("in_ra");
        } catch (IllegalArgumentException e) {
            ra= Double.NaN;
        }

        return ra;
    }

    public static double getDec(DataGroup group, DataObject rowData) {
        double dec;
        try {
            if (group.containsKey("dec_obj")) dec = (Double) rowData.getDataElement("dec_obj");
            else dec = (Double) rowData.getDataElement("in_dec");
        } catch (IllegalArgumentException e) {
            dec= Double.NaN;
        }
        return dec;
    }

    private static WebPlotRequest makeRequest(Level level,
                                              int band,
                                              String scanID,
                                              int frameNum,
                                              double ra,
                                              double dec,
                                              ServerRequest req,
                                              TableMeta meta) {
        return makeRequest(level, band, scanID, frameNum, null, ra, dec,
                           req.getParam("subsize"),
                           meta.getAttribute("host"),
                           req.getParam("schema"),
                           meta.getAttribute("schemaGroup"),
                           meta.getAttribute("table"),
                           req.getParam("ProductLevel"));
    }

    private static WebPlotRequest makeRequest(Level level,
                                              int band,
                                              String coaddID,
                                              double ra,
                                              double dec,
                                              ServerRequest req,
                                              TableMeta meta) {
        return makeRequest(level,  band, null, -1, coaddID, ra, dec,
                           req.getParam("subsize"),
                           meta.getAttribute("host"),
                           req.getParam("schema"),
                           meta.getAttribute("schemaGroup"),
                           meta.getAttribute("table"),
                           req.getParam("ProductLevel"));
    }

    private static WebPlotRequest makeRequest(Level level,
                                              int band,
                                              String scanID,
                                              int frameNum,
                                              String coaddID,
                                              double ra,
                                              double dec,
                                              String subsize,
                                              String host,
                                              String schema,
                                              String schemaGroup,
                                              String table,
                                              String productLevel) {
        ServerRequest sr = new ServerRequest("WiseFileRetrieve");
        if (scanID != null) sr.setParam("scan_id", scanID);
        if (frameNum > -1) sr.setParam("frame_num", frameNum + "");
        if (coaddID != null) sr.setParam("coadd_id", coaddID);
        sr.setParam("host", host);
        sr.setParam("schema", schema);
        sr.setParam("schemaGroup", schemaGroup);
        sr.setParam("table", "4band_i1bm_frm"); // todo fix!
        sr.setParam("ProductLevel", productLevel);
        if (!StringUtils.isEmpty(subsize)) sr.setParam("subsize", subsize);
        sr.setParam("band", band + "");
        if (!Double.isNaN(ra)) sr.setParam("ra_obj", ra + "");
        if (!Double.isNaN(ra)) sr.setParam("dec_obj", dec + "");
        sr.setParam("table", table);
        boolean full= StringUtil.isEmpty(subsize);

        String title;
        if (scanID!=null) {
            title = scanID + frameNum + "-w" + band;
//            sr.setParam("table", "4band_i1bm_frm"); //todo fix
        }
        else {
            title = coaddID + "-w" + band;
//            sr.setParam("table", "4band_i3am_cdd"); //todo fix
        }
        WebPlotRequest r = WebPlotRequest.makeProcessorRequest(sr, title);
        r.setInitialColorTable(1);
        r.setPreferenceColorKey(makeColorPrefKey(level,band));
        r.setPreferenceZoomKey(makeZoomPrefKey(level,band,full));
        RangeValues rv = new RangeValues(RangeValues.PERCENTAGE, 1, RangeValues.PERCENTAGE, 99, RangeValues.STRETCH_LOG);
        r.setInitialRangeValues(rv);
        r.setZoomType(ZoomType.STANDARD);
        r.setInitialZoomLevel(getZoomLevel(level,band,full));
        r.setUniqueKey(frameNum + scanID);
        r.setSaveCorners(true);
        return r;
    }

    private static String makeColorPrefKey(Level level, int band) {
        return "WISE-Band" + band+ "-" +level;
    }

    private static String makeZoomPrefKey(Level level, int band, boolean full) {
        String size= full ? "full" : "sub";
        return "Wise-zoomgrid-Band"+ band+ "-" +level + "-"+ size;
    }

    private static float getZoomLevel(Level level, int band, boolean full) {
        float retval;

        if (band==4) {
            if (full) {
                if (level==Level.L3) retval= .0625F;
                else                 retval= .5F;
            }
            else {
                if (level==Level.L3) retval= 1F;
                else                 retval= 2F;
            }
        }
        else {
            if (full) {
                if (level==Level.L3) retval= .0625F;
                else                 retval= .25F;
            }
            else {
                retval= 1F;
            }

        }
        return retval;
    }

    private static DataObject getDataElement(DataGroup table, WebPlotRequest req) {
        DataObject entry = new DataObject(table);
        entry.setDataElement(columns[0], "req");
        entry.setDataElement(columns[1], req.toString());
        entry.setDataElement(columns[2], req.getUserDesc());
        return entry;

    }


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
