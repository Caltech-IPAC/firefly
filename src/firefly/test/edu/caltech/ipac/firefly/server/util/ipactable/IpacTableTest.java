/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
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

    private static final String TEST_ROOT = "test" + File.separatorChar;
    private static final String TEST_RES_PATH = IpacTableTest.class.getCanonicalName().replaceAll("\\.", "/").replace(IpacTableTest.class.getSimpleName(), "") + File.separatorChar;

    private static final File ipacTable = new File(TEST_ROOT, TEST_RES_PATH + "test_data.tbl");
    private static final File jsonResults = new File(TEST_ROOT + TEST_RES_PATH + "test_data.json");
    private static TableServerRequest request;

    @BeforeClass
    public static void setup() {
        request = new TableServerRequest("searchProcID");
        request.setMeta("test-meta", "test-meta-value");
        request.setDecimateInfo(new DecimateInfo("ra", "dec", 1234, .5f));
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

        JSONObject json = JsonTableUtil.toJsonTableModel(page, new TableMeta(tableDef.getSource()), request);
        Assert.assertNotNull(json);
        String jsonStr = json.toJSONString();
        String expected = FileUtil.readFile(jsonResults);
        Assert.assertEquals(expected, jsonStr);
    }
}
