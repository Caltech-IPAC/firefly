package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.mos.QueryMOS;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
@SearchProcessorImpl(id = "shaGridMOSQuery")
public class QueryMOSGrid extends QueryMOS {

    private static DataType[] columns = new DataType[]{
            new DataType("type", String.class),
            new DataType("thumb_nail", String.class),
            new DataType("desc", String.class)
    };


    @Override
    public DataGroupPart getData(ServerRequest sr) throws DataAccessException {
        DataGroupPart primaryData= super.getData(sr);
        DataGroup gridGroup= getGrid(primaryData.getData());

        return new DataGroupPart(primaryData.getTableDef(),
                                 gridGroup,
                                 primaryData.getStartRow(),
                                 primaryData.getRowCount());
    }

    private DataGroup getGrid(DataGroup inTable) {
        columns[1].getFormatInfo().setWidth(1000);

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
        table.addAttributes(new DataGroup.Attribute("COLUMNS", 3 + ""));
        // add the rest of the headers..
        DataObject rowData;
        WebPlotRequest r;
        for (int row = 0; (row < inTable.size()); row++) {
            rowData = inTable.get(row);
            //double ra = getRA(inTable, rowData);
            //double dec = getDec(inTable, rowData);
            r = makeRequest(getBcdId(inTable, rowData));
            DataObject dOjb = getDataElement(table, r);
            table.add(dOjb);
        }

        if (table.size() == 0) {
            table.addAttributes(new DataGroup.Attribute("INFO", "Image data not found!"));
        }
        return table;
    }

    /*
    public static double getRA(DataGroup group, DataObject rowData) {
        double ra;
        try {
            if (group.containsKey("ra_obj")) ra = (Double) rowData.getDataElement("ra_obj");
            else ra= Double.NaN;
        } catch (IllegalArgumentException e) {
            ra= Double.NaN;
        }

        return ra;
    }

    public static double getDec(DataGroup group, DataObject rowData) {
        double dec;
        try {
            if (group.containsKey("dec_obj")) dec = (Double) rowData.getDataElement("dec_obj");
            else dec = Double.NaN;
        } catch (IllegalArgumentException e) {
            dec= Double.NaN;
        }
        return dec;
    }
    */

    public String getBcdId(DataGroup group, DataObject rowData) {

        try {
            if (group.containsKey("bcdid")) {
                return rowData.getDataElement("bcdid").toString();
            }
            else return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static WebPlotRequest makeRequest(String bcdid) {
        String title = "bcd-"+bcdid;
        /*
        ServerRequest sr = new HeritageFileRequest(edu.caltech.ipac.heritage.data.entity.DataType.BCD, bcdid, false);
        WebPlotRequest r = WebPlotRequest.makeProcessorRequest(sr, title);
        */

        // workaround until we add some test data to dev db and file system
        String url = "http://sha.ipac.caltech.edu/applications/Spitzer/SHA/servlet/ProductDownload?DATASET=level1&ID="+bcdid;

        WebPlotRequest r = WebPlotRequest.makeURLPlotRequest(url, title);
        r.setTitle(title);
        r.setInitialColorTable(1);
        r.setPreferenceColorKey(makeColorPrefKey());
        r.setPreferenceZoomKey(makeZoomPrefKey());
        RangeValues rv = new RangeValues(RangeValues.PERCENTAGE, 1, RangeValues.PERCENTAGE, 99, RangeValues.STRETCH_LOG);
        r.setInitialRangeValues(rv);
        r.setZoomType(ZoomType.STANDARD);
        r.setInitialZoomLevel(6F);
        r.setUniqueKey(bcdid);
        r.setSaveCorners(true);
        return r;

    }

    private static String makeColorPrefKey() {
        return "bcdgrid";
    }

    private static String makeZoomPrefKey() {
        return "zoom-bcdgrid";
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta,columns,request);
        meta.setAttribute("GRID_THUMBNAIL_COLUMN", "thumbnail");
        meta.setAttribute("PLOT_REQUEST_COLUMN", "req");
    }

    private static DataObject getDataElement(DataGroup table, WebPlotRequest req) {
        DataObject entry = new DataObject(table);
        entry.setDataElement(columns[0], "req");
        entry.setDataElement(columns[1], req.toString());
        entry.setDataElement(columns[2], req.getUserDesc());
        return entry;

    }
}
