/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.VotableTest;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static edu.caltech.ipac.firefly.data.sofia.VOSpectraModel.SPECTRADM_UTYPE;
import static edu.caltech.ipac.firefly.server.persistence.MultiSpectrumProcessor.*;
import static edu.caltech.ipac.table.TableMeta.UTYPE;
import static org.junit.Assert.*;

/**
 * Date: 6/3/25
 *
 * @author loi
 * @version : $
 */
public class MultiSpectrumProcessorTest extends ConfigTest {

    private static File testFile
            ;

    @BeforeClass
    public static void setUp() {
        setupServerContext(null);
        testFile = FileLoader.resolveFile(VotableTest.class, "/multispectrum-array.vot");       // uses same ./table test data directory
        if (false) Logger.setLogLevel(Level.DEBUG);
    }

    @Test
    public void testExtract() {
        MultiSpectrumProcessor processor = new MultiSpectrumProcessor();
        TableServerRequest treq = new TableServerRequest(MultiSpectrumProcessor.PROC_ID);
        treq.setParam(SOURCE, testFile.getAbsolutePath());
        treq.setParam(MODE, Mode.extract.name());
        treq.setParam(SEL_ROW_IDX, "0");
        try {
            DataGroup table = processor.getData(treq).getData();
            assertNotNull("Should not be null", table);
            assertEquals("Should have 694 row", 694, table.size());
            assertEquals("Should have 18 columns", 18, table.getDataDefinitions().length);
            assertEquals("Is a Spectrum", SPECTRADM_UTYPE, table.getAttribute(UTYPE));
            assertEquals("Has defined spectrum data", SPECTRUM_DATA_UTYPE, table.getGroupInfos().getFirst().getUtype());
            assertEquals("Is photometry spectrum", "photometry", table.getGroupInfos().getFirst().getID());
            assertTrue("Has Service Descriptor", table.getResourceInfos().getFirst().getUtype().equalsIgnoreCase("adhoc:service"));
        } catch (Exception e) {
            Logger.getLogger().error(e);
            throw new RuntimeException("Failed to process multi-spectrum data", e);
        }
    }
}
