/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbDataIngestor;
import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.io.IpacTableReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class DbDataIngestorTest extends ConfigTest {

    @Test
    public void voTableWithoutTable() throws Exception {
        File votable = File.createTempFile("no-table-", ".xml");
        votable.deleteOnExit();
        Files.writeString(votable.toPath(), """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
  <RESOURCE type="results"/>
</VOTABLE>
""");
        File dbFile = new File(votable.getParentFile(), votable.getName() + ".duckdb");
        dbFile.deleteOnExit();

        try {
            DbDataIngestor.ingestData(new TableServerRequest("test"), new DuckDbAdapter(dbFile), null, votable, 0);
            Assert.fail("a VOTable without a TABLE must not ingest as if it succeeded");
        } catch (DataAccessException e) {
            Assert.assertEquals("No table found in the VOTable", e.getMessage());
        }
    }

    /**
     * An IPAC table without a type line: streaming into DuckDB must type columns the same way the in-memory read does.
     * 'ra' is numeric in the first row -> double.  'note' is blank in the first row -> String, even though a later row is numeric.
     */
    @Test
    public void ipacTableWithoutTypeLine() throws Exception {
        File ipac = File.createTempFile("no-type-line-", ".tbl");
        ipac.deleteOnExit();
        Files.writeString(ipac.toPath(), """
|   ra     | note  |
   10.5            \s
   20.25     42    \s
""");
        File dbFile = new File(ipac.getParentFile(), ipac.getName() + ".duckdb");
        dbFile.deleteOnExit();

        DuckDbAdapter dbAdapter = new DuckDbAdapter(dbFile);
        DbDataIngestor.ingestData(new TableServerRequest("test"), dbAdapter, null, ipac, 0);
        DataGroup ingested = dbAdapter.execQuery("select * from DATA", "DATA");
        DataGroup inMemory = IpacTableReader.read(ipac);

        Assert.assertEquals(2, ingested.size());
        Assert.assertEquals(Double.class, ingested.getDataDefintion("ra").getDataType());
        Assert.assertEquals(String.class, ingested.getDataDefintion("note").getDataType());
        Assert.assertEquals(20.25, ingested.getData("ra", 1));
        Assert.assertEquals("42", ingested.getData("note", 1));

        Assert.assertEquals(Double.class, inMemory.getDataDefintion("ra").getDataType());
        Assert.assertEquals(String.class, inMemory.getDataDefintion("note").getDataType());
    }

    /**
     * The same VOTable on a database other than DuckDB.
     */
    @Test
    public void voTableWithoutTableNonDuckDb() throws Exception {
        File votable = File.createTempFile("no-table-", ".xml");
        votable.deleteOnExit();
        Files.writeString(votable.toPath(), """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
  <RESOURCE type="results"/>
</VOTABLE>
""");
        File dbFile = new File(votable.getParentFile(), votable.getName() + ".hsql");
        dbFile.deleteOnExit();

        try {
            DbDataIngestor.ingestData(new TableServerRequest("test"), new HsqlDbAdapter(dbFile), null, votable, 0);
            Assert.fail("a VOTable without a TABLE must not ingest as if it succeeded");
        } catch (DataAccessException e) {
            Assert.assertEquals("No table found in the VOTable", e.getMessage());
        }
    }
}
