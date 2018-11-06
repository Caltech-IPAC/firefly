/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;
import static edu.caltech.ipac.table.JsonTableUtil.getPathValue;

public class IpacTableFromSourceTest extends ConfigTest {

    @BeforeClass
    public static void setUp() {
        // needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
    }

    @Test
    public void fromFileSystem() {
        try {
            File testFile = FileLoader.resolveFile(VotableTest.class, "/sample_multi_votable.xml");

            TableServerRequest req = new TableServerRequest(IpacTableFromSource.PROC_ID);
            req.setParam(ServerParams.SOURCE, testFile.getAbsolutePath());
            req.setParam(TBL_INDEX, "0");

            DataGroup results = new IpacTableFromSource().fetchDataGroup(req);
            verifyTableData(results, null);


            // now load the 2nd table.  columns and table IDs are appended with "-1", but the data are the same
            // using same validation test.. but for 2nd table
            req.setParam(TBL_INDEX, "1");
            results = new IpacTableFromSource().fetchDataGroup(req);
            verifyTableData(results, "-1");
        } catch (Exception e) {
            Assert.fail("IpacTableFromSourceTest.fromFileSystem failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void fromUrl() {
        try {
            TableServerRequest req = new TableServerRequest(IpacTableFromSource.PROC_ID);
            req.setParam(ServerParams.SOURCE, "http://web.ipac.caltech.edu/staff/loi/demo/sample_multi_votable.xml");

            DataGroup results = new IpacTableFromSource().fetchDataGroup(req);
            verifyTableData(results, null);

            // now load the 2nd table.  columns and table IDs are appended with "-1", but the data are the same
            // using same validation test.. but for 2nd table
            req.setParam(TBL_INDEX, "1");
            results = new IpacTableFromSource().fetchDataGroup(req);
            verifyTableData(results, "-1");
        } catch (Exception e) {
            Assert.fail("IpacTableFromSourceTest.fromUrl failed with exception: " + e.getMessage());
        }
    }

    private void verifyTableData(DataGroup data, String suffix) {
        suffix = suffix == null ? "" : suffix;
        // test table row and column count
        Assert.assertEquals("Number of rows:", 3, data.size());
        Assert.assertEquals("Number of columns:", 6, data.getDataDefinitions().length);

        // test table's data
        Assert.assertEquals(10.68, data.get(0).getFloat("RA" + suffix, Float.MIN_VALUE), 0.00001);
        Assert.assertEquals("N 6744", data.get(1).getDataElement("Name" + suffix));
        Assert.assertEquals(0.7, data.get(2).getFloat("R" + suffix, Float.MIN_VALUE), 0.00001);

        // test table's meta info
        Assert.assertNull(data.getAttribute("resource-info"));			// we ignore info outside of the table
        Assert.assertNull(data.getAttribute("resource-info2"));		// we ignore info outside of the table
        Assert.assertEquals("table-info-value", data.getAttribute("table-info"));
        Assert.assertEquals("table-info-value2", data.getAttribute("table-info2"));

        // test column info
        DataType col6 = data.getDataDefintion("R" + suffix);
        Assert.assertEquals("col6" + suffix, col6.getID());
        Assert.assertEquals(Float.class, col6.getDataType());
        Assert.assertEquals("Mpc", col6.getUnits());
        Assert.assertEquals("pos.distance;pos.heliocentric", col6.getUCD());
        Assert.assertEquals("F1", col6.getPrecision());
        Assert.assertEquals(4, col6.getWidth());
        List<LinkInfo> links = col6.getLinkInfos();
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("query", links.get(0).getRole());
        Assert.assertEquals("http://ivoa.spectr/server?obsno=${Name}", links.get(0).getHref());

        // test table's aux data info
        List<GroupInfo> groups = data.getGroupInfos();
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals("J2000" + suffix, groups.get(0).getID());
        Assert.assertEquals(2, groups.get(0).getColumnRefs().size());
        Assert.assertEquals("col1" + suffix, groups.get(0).getColumnRefs().get(0).getRef());
        Assert.assertEquals("col2" + suffix, groups.get(0).getColumnRefs().get(1).getRef());

        Assert.assertEquals(1, groups.get(0).getParamInfos().size());
        Assert.assertEquals("cooframe", groups.get(0).getParamInfos().get(0).getKeyName());
        Assert.assertEquals("pos.frame", groups.get(0).getParamInfos().get(0).getUCD());
        Assert.assertEquals("UTC-ICRS-TOPO", groups.get(0).getParamInfos().get(0).getValue());
        Assert.assertEquals("char", groups.get(0).getParamInfos().get(0).getTypeDesc());
        Assert.assertEquals("stc:AstroCoords.coord_system_id", groups.get(0).getParamInfos().get(0).getUType());
    }
}