/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DecimationProcessor;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static edu.caltech.ipac.table.JsonTableUtil.getMetaFromAllMeta;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class IpacTableTest {

    private static final File ipacTable = FileLoader.resolveFile(IpacTableTest.class,  "IpacTableTest.tbl");


    @Test
    public void testReadIpacTable() throws IOException {
        DataGroup data = IpacTableReader.read(ipacTable);
        checkTableData(data);
    }

    @Test
    public void testWriteIpacTable() throws IOException {
        DataGroup data = IpacTableReader.read(ipacTable);

        // instead of comparing the output of write, we'll write it out, read it back in and use the same test to
        // validate that the round-trip preserves the data.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IpacTableWriter.save(output, data, true);
        data = IpacTableReader.read(new ByteArrayInputStream(output.toByteArray()));
        checkTableData(data);
    }

    @Test
    public void testGetMetaInfo() throws IOException {
        IpacTableDef tableDef = IpacTableUtil.getMetaInfo(ipacTable);

        Assert.assertNotNull(tableDef);
        Assert.assertEquals(21, tableDef.getKeywords().size());    // includes 3 empty comments lines
        Assert.assertEquals("___ declination (J2000 decimal deg)", tableDef.getKeywords().get(16).getValue());
    }

    @Test
    public void testJsonTableUtil() throws IOException {
        // used to test info from request get into the json tablemodel
        TableServerRequest request = new TableServerRequest("searchProcID");
        request.setMeta("test-meta", "test-meta-value");
        DecimationProcessor.setDecimateInfo(request, new DecimateInfo("ra", "dec", 1234, .5f));
        request.setSortInfo(new SortInfo("ra", "dec"));
        request.setFilters(Arrays.asList("ra > 0", "dec > 0"));
        DataGroup data = IpacTableReader.read(ipacTable);
        DataGroupPart page = new DataGroupPart(data, 0, data.size());

        JSONObject json = JsonTableUtil.toJsonTableModel(page, request);
        Assert.assertNotNull(json);

        // selectively test converted json object.
        Assert.assertEquals("totalRows", 16, json.get("totalRows"));

        // the request
        Assert.assertEquals("meta-info", "test-meta-value", JsonTableUtil.getPathValue(json, "request", TableServerRequest.META_INFO, "test-meta"));
        Assert.assertEquals("sort-info", "ASC,ra,dec", JsonTableUtil.getPathValue(json, "request", TableServerRequest.SORT_INFO));
        Assert.assertEquals("filter-into", "ra > 0;dec > 0", JsonTableUtil.getPathValue(json, "request", TableServerRequest.FILTERS));
        Assert.assertEquals("decimate-info", "decimate=ra,dec,1234,0.5,,,,,-1", JsonTableUtil.getPathValue(json, "request", DecimateInfo.DECIMATE_TAG));

        // tableMeta
        Assert.assertEquals("SKYAREA", "'within 500.0 arcsec of  ra=10.68479 dec=+41.26906 Eq J2000 '", getMetaFromAllMeta(json,  "SKYAREA"));

        // tableData.columns
        checkJsonColumn(json, "tableData.columns.0", "ra", "double", "deg", "null", null);          // first column
        checkJsonColumn(json, "tableData.columns.3", "clat", "char", null, "null", null);           // middle column
        checkJsonColumn(json, "tableData.columns.7", "designation", "char", null, "null", null);    // last column

        // tableData.data
        Assert.assertEquals("cell(0,0)", "10.733387", JsonTableUtil.getPathValue(json, "tableData", "data", "0", "0"));             // cell (0,0) first row, first column
        Assert.assertEquals("cell(4,7)", "0.35", JsonTableUtil.getPathValue(json, "tableData", "data",  "7", "4"));                 // cell (4,7) middle row, middle column
        Assert.assertEquals("cell(7,15)", "00425009+4111543", JsonTableUtil.getPathValue(json, "tableData", "data",  "15", "7"));   // cell (7,15) last row, last column
    }


    private void checkJsonColumn(JSONObject jsonModel, String path, String name, String type, String units, String nullStr, Integer width) {
        JSONObject acol = (JSONObject) JsonTableUtil.getPathValue(jsonModel, path.split("\\."));
        Assert.assertEquals("column(" + name + ") name", name, acol.get("name"));
        Assert.assertEquals("column(" + name + ") type", type, acol.get("type"));
        Assert.assertEquals("column(" + name + ") units", units, acol.get("units"));
        Assert.assertEquals("column(" + name + ") nullString", nullStr, acol.get("nullString"));
        Assert.assertEquals("column(" + name + ") width", width, acol.get("width"));
    }

    private void checkColumn(DataType acol, String name, String type, String units, String nullStr, Integer width) {
        Assert.assertEquals("column(" + name + ") name", name, acol.getKeyName());
        Assert.assertEquals("column(" + name + ") type", type, acol.getTypeDesc());
        Assert.assertEquals("column(" + name + ") units", units, acol.getUnits());
        Assert.assertEquals("column(" + name + ") nullString", nullStr, acol.getNullString());
        Assert.assertEquals("column(" + name + ") width", (int)width, acol.getWidth());
    }


    private void checkTableData(DataGroup table) {
        Assert.assertNotNull(table);

        // table meta
        Assert.assertEquals(21, table.getTableMeta().getKeywords().size());    // includes 3 empty comments lines
        Assert.assertEquals("___ declination (J2000 decimal deg)", table.getTableMeta().getKeywords().get(16).getValue());
        Assert.assertEquals("2412", table.getAttribute("RowsRetrieved"));

        // columns
        checkColumn(table.getDataDefintion("ra"),"ra", "double", "deg", "null", 0);                 // first column
        checkColumn(table.getDataDefintion("clat"),"clat", "char", "", "null", 0);                // middle column
        checkColumn(table.getDataDefintion("designation"),"designation", "char", "", "null", 0);  // last column

        // rows
        Assert.assertEquals(16, table.size());

        // tableData.data
        Assert.assertEquals("cell(0,0)", 10.733387, table.getData("ra", 0));             // cell (0,0) first row, first column
        Assert.assertEquals("cell(4,7)", 0.35, table.getData("err_maj", 7));                 // cell (4,7) middle row, middle column
        Assert.assertEquals("cell(7,15)", "00425009+4111543", table.getData("designation", 15));   // cell (7,15) last row, last column
    }
}
