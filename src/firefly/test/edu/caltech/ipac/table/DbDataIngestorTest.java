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
