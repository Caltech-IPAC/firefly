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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class VoTableReaderTest extends ConfigTest {

    private static File midFile;
    private static File largeFile;

    @BeforeClass
    public static void setUp() {
        midFile = FileLoader.resolveFile("FileUpload-samples/VOTable/tabledata/multiTables_Ned.xml");                   // 8.6 MB
        largeFile = FileLoader.resolveFile("LSSTFoorprintSources/combined_sources_and_footprints_5000.xml");            // 44 MB
    }


//====================================================================
//  error document detection
//====================================================================

    /**
     * captured: IRSA SIA, bad POS.  The DALI 1.1 sec 4.4 shape.
     * https://irsa.ipac.caltech.edu/SIA?COLLECTION=spitzer_seip&POS=xxx+83.6+22.0+0.1
     */
    @Test
    public void daliQueryStatusError() throws Exception {
        String votable = """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.ivoa.net/xml/VOTable/v1.3" xsi:schemaLocation="http://www.ivoa.net/xml/VOTable/v1.3 http://www.ivoa.net/xml/VOTable/v1.3">
  <DESCRIPTION>Caltech/IPAC-IRSA IVOA Simple Image Access v2 Service</DESCRIPTION>
  <RESOURCE type="results">
    <INFO name="QUERY_STATUS" value="ERROR">UsageFault: BAD_REQUEST: Unknown shape in POS.  Expected CIRCLE, RANGE, or POLYGON, but found: xxx</INFO>
  </RESOURCE>
</VOTABLE>
""";
        Assert.assertEquals("UsageFault: BAD_REQUEST: Unknown shape in POS.  Expected CIRCLE, RANGE, or POLYGON, but found: xxx",
                getError(votable));
    }

    /**
     * NED example, unresolvable object name.  The QUERY_STATUS marker is a PARAM, not an INFO,
     * and the message is in its DESCRIPTION.
     */
    @Test
    public void nedQueryStatusParam() throws Exception {
        String votable = """
<VOTABLE xmlns="http://www.ivoa.net/xml/VOTable/v1.3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.5" xsi:schemaLocation="http://www.ivoa.net/xml/VOTable/v1.3 https://www.ivoa.net/xml/VOTable/VOTable-1.5.xsd">
<RESOURCE type="results">
 <PARAM name="QUERY_STATUS" datatype="char" arraysize="*" value="ERROR">
  <DESCRIPTION> GeneralFault: Service could not complete request; Failed to resolve input object name (6) </DESCRIPTION>
</PARAM>
</RESOURCE>
</VOTABLE>
""";
        Assert.assertEquals("GeneralFault: Service could not complete request; Failed to resolve input object name (6)",
                getError(votable));
    }

    /**
     * captured: IRSA SCS, RA=abc.  Message in the value attribute, INFO at VOTABLE top level.
     * https://irsa.ipac.caltech.edu/SCS?table=fp_psc&RA=abc&DEC=22.0&SR=0.01
     */
    @Test
    public void scsErrorInValueAttribute() throws Exception {
        String votable = """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.ivoa.net/xml/VOTable/v1.3" xsi:schemaLocation="http://www.ivoa.net/xml/VOTable/v1.3 http://www.ivoa.net/xml/VOTable/v1.3">
  <DESCRIPTION>Caltech/IPAC-IRSA IVOA Simple Cone Search Service</DESCRIPTION>
  <INFO name="Error" ID="Error" value="BAD_REQUEST: The value of parameter &apos;RA&apos; could not be converted to a number: abc"/>
  <RESOURCE type="results"/>
</VOTABLE>
""";
        // not the top-level DESCRIPTION, which holds the service's name
        Assert.assertEquals("BAD_REQUEST: The value of parameter 'RA' could not be converted to a number: abc",
                getError(votable));
    }

    /**
     * captured: HEASARC cone search, unknown table.  Same shape, but nested in RESOURCE and with no ID attribute.
     * https://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=xxxnosuchtable&RA=83.6&DEC=22.0&SR=0.1
     */
    @Test
    public void scsErrorNestedWithoutId() throws Exception {
        String votable = """
<?xml version="1.0"?>
<VOTABLE xmlns="http://www.ivoa.net/xml/VOTable/v1.3" xsi:schemaLocation="http://www.ivoa.net/xml/VOTable/v1.3 http://www.ivoa.net/xml/VOTable/v1.3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.4">
 <RESOURCE type="results">
  <INFO name="Error" value="Unknown table: No information available on xxxnosuchtable. Is this a valid table?"/>
 </RESOURCE>
</VOTABLE>
""";
        Assert.assertEquals("Unknown table: No information available on xxxnosuchtable. Is this a valid table?",
                getError(votable));
    }

    /**
     * captured: VizieR cone search, unknown catalog.  The Error INFO is one of nine, and a Warning follows it.
     * https://vizier.cds.unistra.fr/viz-bin/conesearch/NOSUCHCAT?RA=83.6&DEC=22.0&SR=0.1
     * The body repeats that URL in its own INFO name="request", so it carries its provenance with it.
     * Shortened: the DESCRIPTION is cut down and the XML comment after it is removed.
     */
    @Test
    public void scsErrorAmongManyInfos() throws Exception {
        String votable = """
<?xml version="1.0" encoding="UTF-8"?>
<VOTABLE version="1.4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://www.ivoa.net/xml/VOTable/v1.3"
  xsi:schemaLocation="http://www.ivoa.net/xml/VOTable/v1.3 http://www.ivoa.net/xml/VOTable/v1.3">
 <DESCRIPTION>
   VizieR Astronomical Server vizier.cds.unistra.fr
   In case of problem, please report to: cds-question@unistra.fr
 </DESCRIPTION>
<INFO ID="standardID" name="standardID" value="ivo://ivoa.net/std/ConeSearch/1.03"/>
<INFO name="service_protocol" value="ivo://ivoa.net/std/ConeSearch/1.03">  IVOID of the protocol through which the data was retrieved</INFO>
<INFO name="request_date" value="2026-09-18T18:11:46">  Query execution date</INFO>
<INFO name="request" value="https://vizier.cds.unistra.fr/viz-bin/conesearch/NOSUCHCAT?RA=83.6&amp;DEC=22.0&amp;SR=0.1">  Full request URL</INFO>
<INFO name="contact" value="cds-question@unistra.fr">  Email or URL to contact publisher</INFO>
<INFO name="server_software" value="VizieR/7.6">  Software version</INFO>
<INFO name="publisher" value="CDS">  Data centre that produced the VOTable</INFO>
<INFO ID="Error" name="Error" value="Table or Catalog not found: NOSUCHCAT"/>

<INFO name="Warning" value="can't find table or catalogue: NOSUCHCAT"/>
<RESOURCE>
</RESOURCE>
</VOTABLE>
""";
        // neither the Warning that follows it, nor the text content of the INFOs that precede it
        Assert.assertEquals("Table or Catalog not found: NOSUCHCAT", getError(votable));
    }

    /** an error marker with nothing in it used to yield "", which is non-null and so reaches the user blank */
    @Test
    public void errorWithNoMessageIsNotBlank() throws Exception {
        String votable = """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
  <RESOURCE type="results">
    <INFO name="QUERY_STATUS" value="ERROR"/>
  </RESOURCE>
</VOTABLE>
""";
        String error = getError(votable);
        Assert.assertNotNull("an error document must not report a null error", error);
        Assert.assertFalse("an error document must not report a blank error", error.trim().isEmpty());
    }

    /**
     * A successful response is not an error document, even though it carries INFO, PARAM and a column
     * named Error.  Adapted, not verbatim: the shape is a real IRSA SIA success --
     * https://irsa.ipac.caltech.edu/SIA?COLLECTION=spitzer_seip&POS=circle+83.6+22.0+0.002
     * -- cut to one row, with the "Error" FIELD added by hand.  Real SIA emits no
     * element named Error, so only the two TABLEs and QUERY_STATUS="OK" carry over from the live body.
     */
    @Test
    public void successfulResponseHasNoError() throws Exception {
        String votable = """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
  <DESCRIPTION>Caltech/IPAC-IRSA IVOA Simple Image Access v2 Service</DESCRIPTION>
  <RESOURCE type="results">
    <INFO name="QUERY_STATUS" value="OK"/>
    <TABLE>
      <FIELD name="obs_collection" datatype="char" arraysize="*"/>
      <FIELD name="Error" datatype="double" unit="arcsec"/>
      <DATA><TABLEDATA>
        <TR><TD>spitzer_seip</TD><TD>0.12</TD></TR>
      </TABLEDATA></DATA>
    </TABLE>
  </RESOURCE>
  <RESOURCE type="meta" utype="adhoc:service">
    <PARAM name="accessURL" datatype="char" arraysize="*" value="https://irsa.ipac.caltech.edu/SIA"/>
    <TABLE>
      <FIELD name="ID" datatype="char" arraysize="*"/>
      <DATA><TABLEDATA>
        <TR><TD>cutout</TD></TR>
      </TABLEDATA></DATA>
    </TABLE>
  </RESOURCE>
</VOTABLE>
""";
        Assert.assertNull(getError(votable));
    }

    @Test
    public void errorMessageIsTrimmed() throws Exception {
        String votable = """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
  <RESOURCE type="results">
    <INFO name="QUERY_STATUS" value="ERROR">
        UsageFault: BAD_REQUEST: missing required parameter
    </INFO>
  </RESOURCE>
</VOTABLE>
""";
        Assert.assertEquals("UsageFault: BAD_REQUEST: missing required parameter", getError(votable));
    }

    /** the Error INFO is read even when the document also carries a TABLE */
    @Test
    public void errorIsReadAlongsideATable() throws Exception {
        String votable = """
<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.3" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
  <INFO name="Error" value="BAD_REQUEST: unreadable position"/>
  <RESOURCE type="results">
    <TABLE>
      <FIELD name="ra" datatype="double"/>
      <DATA><TABLEDATA>
        <TR><TD>83.633107</TD></TR>
      </TABLEDATA></DATA>
    </TABLE>
  </RESOURCE>
</VOTABLE>
""";
        Assert.assertEquals("BAD_REQUEST: unreadable position", getError(votable));
    }

    private static String getError(String votable) throws Exception {
        return VoTableReader.getError(toStream(votable), "n/a");
    }

    private static InputStream toStream(String votable) {
        return new ByteArrayInputStream(votable.getBytes(StandardCharsets.UTF_8));
    }


//====================================================================
//  performance
//====================================================================

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
