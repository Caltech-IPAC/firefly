/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DecimationProcessor;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;
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

    private static final File ipacTable = FileLoader.resolveFile(IpacTableTest.class,  "test_data.tbl");
    private static final File jsonResults = FileLoader.resolveFile(IpacTableTest.class,"test_data.json");
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
        DataGroup data = DataGroupReader.read(ipacTable);
        Assert.assertNotNull(data);
    }

    @Test
    public void testGetMetaInfo() throws IOException {
        TableDef tableDef = IpacTableUtil.getMetaInfo(ipacTable);
        Assert.assertNotNull(tableDef);
    }

    @Test
    public void testJsonTableUtil() throws IOException {
        TableDef tableDef = IpacTableUtil.getMetaInfo(ipacTable);
        DataGroup data = DataGroupReader.read(ipacTable);
        DataGroupPart page = new DataGroupPart(tableDef, data, 0, tableDef.getRowCount());

        // set fields that changes based on env to static values
        page.getTableDef().setSource("test data");
        // -- end

        JSONObject json = JsonTableUtil.toJsonTableModel(page, request);
        Assert.assertNotNull(json);
        String jsonStr = json.toJSONString().trim();
        String expected = FileUtil.readFile(jsonResults).trim();
        Assert.assertEquals(expected, jsonStr);


        //JSONObject json = JsonTableUtil.toJsonTableModel(page, new TableMeta(tableDef.getSource()), request);
        // set fields that changes based on env to static values
       /* HashMap<Band, DataGroup> dataMap = new HashMap<>();
        dataMap.put(Band.NO_BAND, data);
        jsonStr = JsonTableUtil.dataGroupMapToJasonString(dataMap, request);
        Assert.assertNotNull(jsonStr );*/
    }



}
