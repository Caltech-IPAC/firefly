/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.io.VoTableReader;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class VoTableReaderTest extends ConfigTest {

    private static File midFile;
    private static File largeFile;

    @BeforeClass
    public static void setUp() {
        // needed when dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
        midFile = FileLoader.resolveFile("FileUpload-samples/VOTable/tabledata/multiTables_Ned.xml");                   // 8.6 MB
        largeFile = FileLoader.resolveFile("LSSTFoorprintSources/combined_sources_and_footprints_5000.xml");            // 44 MB
    }



    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestMidSize() throws Exception {
        // before code refactor
        // [main] INFO  console  - getheader ran 10 times., Elapsed Time: 0.3430 SECONDS., Total time is 4.2630 SECONDS, Avg time is 0.4263 SECONDS.

        Logger.setLogLevel(Level.DEBUG, "edu.caltech");     // exclude starlink warning logs
        StopWatch.getInstance().start("perfTestMidSize");
        for(int i=0; i < 10; i++) {
            StopWatch.getInstance().start("getheader");
            VoTableReader.analyze(midFile, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("getheader");
        }
        StopWatch.getInstance().printLog("getheader", StopWatch.Unit.SECONDS);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestLargeSize() throws Exception {
        // before code refactor
        // [main] INFO  console  - getheader ran 10 times., Elapsed Time: 0.6150 SECONDS., Total time is 6.6650 SECONDS, Avg time is 0.6665 SECONDS

        Logger.setLogLevel(Level.DEBUG, "edu.caltech");
        StopWatch.getInstance().start("perfTestLargeSize");
        for(int i=0; i < 10; i++) {
            StopWatch.getInstance().start("getheader");
            VoTableReader.analyze(largeFile, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("getheader");
        }
        StopWatch.getInstance().printLog("getheader", StopWatch.Unit.SECONDS);
    }

}
