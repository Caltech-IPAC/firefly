/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata.obscorepackager;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.MappedData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;


/**
 * Date: 1/13/26
 *
 * @author kartik
 * @version : $
 */
public class DatalinkUtilTest extends ConfigTest {

    @Test
    public void testCreateUrlFromServDesc_withValuesAndRefs() {
        //note: createUrlFromServDesc expects JSON keys "accessURL" and "inputParams",
        //and within inputParams it expects objects with either "value" or "ref" (or both).
        String json = "{\n" +
                "  \"accessURL\": \"https://example.org/datalink?\",\n" +
                "  \"inputParams\": {\n" +
                "    \"ID\": {\"ref\": \"id_col\"},\n" +
                "    \"FORMAT\": {\"value\": \"application/x-votable+xml\"},\n" +
                "    \"RUNID\": {\"value\": \"myRun\"}\n" +
                "  }\n" +
                "}";

        DatalinkUtil.ServDescUrl out = DatalinkUtil.createUrlFromServDesc(json);
        Assert.assertNotNull(out);
        Assert.assertTrue(out.isValid());

        String partial = out.partialUrl();
        Assert.assertTrue(partial.startsWith("https://example.org/datalink?"));
        Assert.assertTrue("contains FORMAT", partial.contains("FORMAT=application/x-votable+xml"));
        Assert.assertTrue("contains RUNID", partial.contains("RUNID=myRun"));

        //ref-based params should show up in missingParams
        Assert.assertEquals(1, out.missingParams().size());
        Assert.assertEquals("ID", out.missingParams().get(0).paramName());
        Assert.assertEquals("id_col", out.missingParams().get(0).refId());
    }

    @Test
    public void testGetAccessUrlFromNonObsCore() { //resolves ref param from mapped data
        //ServDescUrl: base already has FORMAT=value, but ID must be resolved from ref column
        DatalinkUtil.ServDescUrl servDescUrl = new DatalinkUtil.ServDescUrl(
                "https://example.org/datalink?FORMAT=votable",
                List.of(new DatalinkUtil.MissingParam("ID", "id_col"))
        );

        //DataGroup defines a column with ID "id_col" and keyName "obs_publisher_did" (or anything)
        DataType idCol = new DataType("obs_publisher_did", String.class);
        idCol.setID("id_col"); // this is what getAccessUrlFromNonObsCore matches on (data.getID())

        DataGroup dg = new DataGroup("test", new DataType[]{idCol});
        DataObject row = new DataObject(dg);
        row.setDataElement(idCol, "ivo://irsa.ipac/wise/allsky#000123456");
        dg.add(row);

        //MappedData provides the actual value by keyName
        MappedData md = new MappedData();
        md.put(0, "obs_publisher_did", "ivo://irsa.ipac/wise/allsky#000123456");

        String finalUrl = DatalinkUtil.getAccessUrlFromNonObsCore(servDescUrl, dg, md, 0);
        Assert.assertEquals("https://example.org/datalink?FORMAT=votable&ID=ivo://irsa.ipac/wise/allsky#000123456", finalUrl);
    }

    @Test
    public void testGetAccessUrlFromNonObsCore_NoID() { //case where ID param is missing in MappedData
        DatalinkUtil.ServDescUrl servDescUrl = new DatalinkUtil.ServDescUrl(
                "https://example.org/datalink?",
                List.of(new DatalinkUtil.MissingParam("ID", "id_col"))
        );

        DataType idCol = new DataType("obs_publisher_did", String.class);
        idCol.setID("id_col");

        DataGroup dg = new DataGroup("test", new DataType[]{idCol});
        dg.add(new DataObject(dg));

        MappedData md = new MappedData(); //no value for obs_publisher_did at row 0

        String finalUrl = DatalinkUtil.getAccessUrlFromNonObsCore(servDescUrl, dg, md, 0);
        //partial ends with '?', and since ID is missing we should keep it as-is
        Assert.assertEquals("https://example.org/datalink?", finalUrl);
    }

    @Test
    public void testIsLinksTable() {
        DataType[] defs = new DataType[] {
                new DataType("ID", String.class),
                new DataType("ACCESS_URL", String.class),
                new DataType("SERVICE_DEF", String.class),
                new DataType("ERROR_MESSAGE", String.class),
                new DataType("SEMANTICS", String.class),
                new DataType("DESCRIPTION", String.class),
                new DataType("CONTENT_TYPE", String.class),
                new DataType("CONTENT_LENGTH", String.class)
        };

        DataGroup dg = new DataGroup("links", defs);
        Assert.assertTrue("true when all required cols present", DatalinkUtil.isLinksTable(dg));

        //convert to invalid schema by removing one required column (error_message)
        DataType[] defsMissingOne = Arrays.stream(defs)
                .filter(dt -> !"ERROR_MESSAGE".equalsIgnoreCase(dt.getKeyName()))
                .toArray(DataType[]::new);

        DataGroup dgMissingOne = new DataGroup("linksMissingOneCol", defsMissingOne);
        Assert.assertFalse("false when missing required col", DatalinkUtil.isLinksTable(dgMissingOne));
    }


}
