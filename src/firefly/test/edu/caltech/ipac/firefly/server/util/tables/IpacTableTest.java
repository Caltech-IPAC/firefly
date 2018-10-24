/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DecimationProcessor;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.io.IpacTableReader;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class IpacTableTest {

    private static final File ipacTable = FileLoader.resolveFile(IpacTableTest.class,  "IpacTableTest.tbl");
    private static TableServerRequest request;

    @BeforeClass
    public static void setup() {
        request = new TableServerRequest("searchProcID");
        request.setMeta("test-meta", "test-meta-value");
        DecimationProcessor.setDecimateInfo(request, new DecimateInfo("ra", "dec", 1234, .5f));
        request.setSortInfo(new SortInfo("ra", "dec"));
        request.setFilters(Arrays.asList("ra > 0", "dec > 0"));

    }

    @Test
    public void testReadIpacTable() throws IOException {
        DataGroup data = IpacTableReader.read(ipacTable);
        Assert.assertNotNull(data);
    }

    @Test
    public void testGetMetaInfo() throws IOException {
        IpacTableDef tableDef = IpacTableUtil.getMetaInfo(ipacTable);
        Assert.assertNotNull(tableDef);
    }

    @Test
    public void testJsonTableUtil() throws IOException {
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
        Assert.assertEquals("SKYAREA", "'within 500.0 arcsec of  ra=10.68479 dec=+41.26906 Eq J2000 '", JsonTableUtil.getPathValue(json, "tableMeta", "SKYAREA"));

        // tableData.columns
        checkColumn(JsonTableUtil.getPathValue(json, "tableData", "columns", "0"),           // first column
                                        "ra", "double", "deg", "null", null);
        checkColumn(JsonTableUtil.getPathValue(json, "tableData", "columns", "17"),           // middle column
                                        "k_cmsig", "double", "mag", "null", null);
        checkColumn(JsonTableUtil.getPathValue(json, "tableData", "columns", "43"),           // last column
                                        "j_k", "double", null, "-", null);

        // tableData.data
        Assert.assertEquals("cell(0,0)", "10.733387", JsonTableUtil.getPathValue(json, "tableData", "data", "0", "0"));     // cell (0,0) first row, first column
        Assert.assertEquals("cell(8,16)", "14.947", JsonTableUtil.getPathValue(json, "tableData", "data",  "8", "16"));      // cell (8,16) middle row, middle column
        Assert.assertEquals("cell(15,43)", "1.066", JsonTableUtil.getPathValue(json, "tableData", "data",  "15", "43"));     // cell (15,43) last row, last column
    }


    private void checkColumn(Object col, String name, String type, String units, String nullStr, Integer width) {
        JSONObject acol = (JSONObject) col;
        Assert.assertEquals("column(" + name + ") name", name, acol.get("name"));
        Assert.assertEquals("column(" + name + ") type", type, acol.get("type"));
        Assert.assertEquals("column(" + name + ") units", units, acol.get("units"));
        Assert.assertEquals("column(" + name + ") nullString", nullStr, acol.get("nullString"));
        Assert.assertEquals("column(" + name + ") width", width, acol.get("width"));
    }

}
