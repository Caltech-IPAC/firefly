/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.io.VoTableReader;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static edu.caltech.ipac.firefly.server.db.DbAdapter.MAIN_DB_TBL;
import static edu.caltech.ipac.table.JsonTableUtil.getPathValue;

public class VotableTest extends ConfigTest {

    private static File dbFile;
    private static File testFile;

    @BeforeClass
    public static void setUp() {
        try {
            // needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
            setupServerContext(null);

            dbFile = File.createTempFile("TestDb_", ".hsql");
            dbFile.deleteOnExit();

            testFile = FileLoader.resolveFile(VotableTest.class, "/sample_votable.xml");
        } catch (IOException e) {
            Assert.fail("Unable to ingest data into the database");
        }
    }

    @Test
    public void readTest() {
        try {
            DataGroup data = VoTableReader.voToDataGroups(testFile.getAbsolutePath())[0];
            verifyTableData(data);
        } catch (Exception e) {
            Assert.fail("VotableTest.readTest failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void dbIngestTest() {
        try {
            DataGroup data = VoTableReader.voToDataGroups(testFile.getAbsolutePath())[0];

            // ingest data into db
            HsqlDbAdapter dbAdapter = new HsqlDbAdapter();
            EmbeddedDbUtil.createDbFile(dbFile,dbAdapter);
            EmbeddedDbUtil.ingestDataGroup(dbFile, data, dbAdapter, MAIN_DB_TBL);

            // get data back out
            data = EmbeddedDbUtil.execQuery(dbAdapter, dbFile, "Select RA, \"Dec\", \"Name\", \"RVel\", \"e_RVel\", R from data", MAIN_DB_TBL);

            verifyTableData(data);

        } catch (Exception e) {
            Assert.fail("VotableTest.dbIngestTest failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void toJsonTest() {
        try {
            DataGroup data = VoTableReader.voToDataGroups(testFile.getAbsolutePath())[0];
            JSONObject tableModel = JsonTableUtil.toJsonDataGroup(data);
            verifyJsonTableModel(tableModel);
        } catch (Exception e) {
            Assert.fail("VotableTest.readTest failed with exception: " + e.getMessage());
        }
    }

    private void verifyTableData(DataGroup data) {
        // test table row and column count
        Assert.assertEquals("Number of rows:", 3, data.size());
        Assert.assertEquals("Number of columns:", 6, data.getDataDefinitions().length);

        // test table's data
        Assert.assertEquals(10.68, data.get(0).getFloat("RA", Float.MIN_VALUE), 0.00001);
        Assert.assertEquals("N 6744", data.get(1).getDataElement("Name"));
        Assert.assertEquals(0.7, data.get(2).getFloat("R", Float.MIN_VALUE), 0.00001);

        // test table's meta info
        Assert.assertNull(data.getAttribute("resource-info"));			// we ignore info outside of the table
        Assert.assertNull(data.getAttribute("resource-info2"));		// we ignore info outside of the table
        Assert.assertEquals("table-info-value", data.getAttribute("table-info"));
        Assert.assertEquals("table-info-value2", data.getAttribute("table-info2"));

        // test column info
        DataType col6 = data.getDataDefintion("R");
        Assert.assertEquals("col6", col6.getID());
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
        Assert.assertEquals("J2000", groups.get(0).getID());
        Assert.assertEquals(2, groups.get(0).getColumnRefs().size());
        Assert.assertEquals("col1", groups.get(0).getColumnRefs().get(0).getRef());
        Assert.assertEquals("col2", groups.get(0).getColumnRefs().get(1).getRef());

        Assert.assertEquals(1, groups.get(0).getParamInfos().size());
        Assert.assertEquals("cooframe", groups.get(0).getParamInfos().get(0).getKeyName());
        Assert.assertEquals("pos.frame", groups.get(0).getParamInfos().get(0).getUCD());
        Assert.assertEquals("UTC-ICRS-TOPO", groups.get(0).getParamInfos().get(0).getValue());
        Assert.assertEquals("char", groups.get(0).getParamInfos().get(0).getTypeDesc());
        Assert.assertEquals("stc:AstroCoords.coord_system_id", groups.get(0).getParamInfos().get(0).getUType());
    }

    private void verifyJsonTableModel(JSONObject tm) {

        // test table's data
        Assert.assertEquals("10.68", getPathValue(tm, "tableData", "data", "0", "0" ));
        Assert.assertEquals("N 6744", getPathValue(tm, "tableData", "data", "1", "2" ));
        Assert.assertEquals("0.7", getPathValue(tm, "tableData", "data", "2", "5" ));

        // test table's meta info
        Assert.assertEquals("table-info-value", getPathValue(tm, "tableMeta", "table-info"));
        Assert.assertEquals("table-info-value2", getPathValue(tm, "tableMeta", "table-info2"));

        // test column info
        JSONObject col6 = (JSONObject) getPathValue(tm, "tableData", "columns", "5");
        Assert.assertEquals("col6", col6.get("ID"));
        Assert.assertEquals("float", col6.get("type"));
        Assert.assertEquals("Mpc", col6.get("units"));
        Assert.assertEquals("pos.distance;pos.heliocentric", col6.get("UCD"));
        Assert.assertEquals("F1", col6.get("precision"));
        Assert.assertEquals(4, col6.get("width"));

        List<JSONObject> links = (List<JSONObject>) col6.get("links");
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("query", links.get(0).get("role"));
        Assert.assertEquals("http://ivoa.spectr/server?obsno=${Name}", links.get(0).get("href"));

        // test table's aux data info
        JSONObject group1 = (JSONObject) getPathValue(tm, "groups", "0");
        Assert.assertNotNull(group1);
        Assert.assertEquals("J2000", group1.get("ID"));

        // colRefs
        Assert.assertEquals("col1", getPathValue(group1, "columnRefs", "0", "ref"));
        Assert.assertEquals("col2", getPathValue(group1, "columnRefs", "1", "ref"));

        // paramRefs
        Assert.assertEquals("param1", getPathValue(tm, "groups", "0", "paramRefs", "0", "ref"));
        Assert.assertEquals("made.up.value", getPathValue(tm, "groups", "0", "paramRefs", "0", "UCD"));

        // params
        JSONObject param1 = (JSONObject) getPathValue(tm, "groups", "0", "params", "0");
        Assert.assertNotNull(param1);
        Assert.assertEquals("cooframe", param1.get("name"));
        Assert.assertEquals("pos.frame", param1.get("UCD"));
        Assert.assertEquals("UTC-ICRS-TOPO", param1.get("value"));
        Assert.assertEquals("char", param1.get("type"));
        Assert.assertEquals("stc:AstroCoords.coord_system_id", param1.get("utype"));

    }

}