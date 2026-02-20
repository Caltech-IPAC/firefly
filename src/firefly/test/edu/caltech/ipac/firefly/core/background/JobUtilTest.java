/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.server.query.UwsJobProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.apache.logging.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static edu.caltech.ipac.firefly.core.background.JobUtil.mergeJobInfo;
import static edu.caltech.ipac.firefly.core.background.JobUtil.toJsonJobList;
import static edu.caltech.ipac.util.serialization.Serializer.getJsonMapper;
import static org.junit.Assert.*;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class JobUtilTest extends ConfigTest {

    static Logger.LoggerImpl logger = Logger.getLogger();
    static List<JobInfo> jobs;
    static String xmlJobList;
    static String xmlJob;

    @BeforeClass
    public static void setUp() {
        // needed when dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
        Logger.setLogLevel(Level.DEBUG);

        xmlJob = """
                <uws:job version="1.1" xsi:schemaLocation="http://www.ivoa.net/xml/UWS/v1.0 UWS.xsd" 
                                       xmlns:xml="http://www.w3.org/XML/1998/namespace" 
                                       xmlns:uws="http://www.ivoa.net/xml/UWS/v1.0" 
                                       xmlns:xlink="http://www.w3.org/1999/xlink" 
                                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:uwsc="urn:uwscustom">
                <uws:jobId>1242749568029</uws:jobId>
                <uws:runId>myjobref</uws:runId>
                <uws:ownerId xsi:nil="true"/>
                <uws:phase>COMPLETED</uws:phase>
                <uws:creationTime>2009-05-19T17:01:44.038Z</uws:creationTime>
                <uws:startTime>2009-05-19T17:12:48.038Z</uws:startTime>
                <uws:endTime>2009-05-19T17:12:49.041Z</uws:endTime>
                <uws:executionDuration>86400</uws:executionDuration>
                <uws:destruction>2009-05-29T17:12:48.035Z</uws:destruction>
                <uws:parameters>
                <uws:parameter id="scaleFactor">1.8</uws:parameter>
                <uws:parameter id="image" byReference="true"> http://myserver.org/uws/jobs/jobid123/param/image</uws:parameter>
                </uws:parameters>
                <uws:results>
                <uws:result id="correctedImage" xlink:href="http://myserver.org/uws/jobs/jobid123/result/image" size="3000960" mime-type="image/fits" uwsc:anyattribute="a custom attribute"/>
                </uws:results>
                <uws:errorSummary type="transient" hasDetail="true">
                <uws:message>we have problem</uws:message>
                </uws:errorSummary>
                <uws:jobInfo>
                    <progress>
                        <percentComplete>77</percentComplete>
                        <message>77 of 100 items processed</message>
                        <itemsProcessed>77</itemsProcessed>
                        <totalItems>100</totalItems>
                    </progress>
                </uws:jobInfo>
                </uws:job>
        """.stripIndent();

        xmlJobList = """
            <uws:jobs xmlns:uws="http://www.ivoa.net/xml/UWS/v1.0" xmlns:xlink="http://www.w3.org/1999/xlink">
              <uws:jobref id="job-001" xlink:href="http://example.org/uws/jobs/job-001">
                  <uws:phase>COMPLETED</uws:phase>
                  <uws:runId>runid</uws:runId>
                  <uws:ownerId>sys</uws:ownerId>
                  <uws:creationTime>2025-04-08T22:12:09.705Z</uws:creationTime>
              </uws:jobref>
              <uws:jobref id="job-002">
                  <uws:phase>COMPLETED</uws:phase>
                  <uws:runId>runid</uws:runId>
                  <uws:ownerId>sys</uws:ownerId>
                  <uws:creationTime>2025-04-08T22:12:09.705Z</uws:creationTime>
              </uws:jobref>
            </uws:jobs>
        """.stripIndent();


        String testJobs = """
        [{
          "phase": "ABORTED",
          "jobId": "11111111",
          "meta": {
            "svcId": "IRSA",
            "jobId": "facade-1111",
            "type": "TAP",
          },
          "startTime": "1970-01-01T00:00:00Z",
          "endTime": "1970-01-01T00:00:00Z",
          "executionDuration": 259200,
          "parameters": {
            "oracle_session_timeout": "600",
            "request": "doQuery",
            "query_identifier": "30/63/6507d8d19c756141936a96124752",
            "maxrec": "50000",
            "query": "SELECT designation,ra,dec,sigra,sigdec,sigradec,w1mpro,w1sigmpro,w1snr,w1rchi2,w2mpro,w2sigmpro,w2snr,        w2rchi2,w3mpro,w3sigmpro,w3snr,w3rchi2,w4mpro,w4sigmpro,w4snr,w4rchi2,nb,na,w1sat,w2sat,w3sat,        w4sat,pmra,sigpmra,pmdec,sigpmdec,cc_flags,ext_flg,var_flg,ph_qual,moon_lev,w1nm,w1m,w2nm,w2m,        w3nm,w3m,w4nm,w4m  FROM allwise_p3as_psd  WHERE CONTAINS(POINT('ICRS', ra, dec),CIRCLE('ICRS', 148.888221, 69.06529461, 0.002777777777777778))=1",
            "staging_directory": "/work/TAP/6b/ed/0400761397484ec214582927d7ff",
            "lang": "ADQL",
            "webdav_directory": "/work/pubspace/30/63/6507d8d19c756141936a96124752"
          },
          "errorSummary": {"message": "Exceeded execution duration", "type": "fatal"},
          "jobInfo": {
            "jobUrl": "https://irsa.ipac.caltech.edu/TAP/async/64618972",
            "title" : "allwise_p3as_psd - irsa"                         ,
            "userId": "Guest"
          }
        },
        {
          "phase": "COMPLETED",
          "jobId": "22222222",
          "meta": {
            "svcId": "IRSA",
            "jobId": "facade-2222",
            "type": "UWS",
          },
          "startTime": "1971-01-01T00:00:00Z",
          "endTime": "1971-01-01T00:00:00Z",
          "executionDuration": 259200,
          "parameters": {
            "oracle_session_timeout": "600",
            "request": "doQuery",
          },
          "results": [
            {
              "id": "result",
              "href": "https://irsa.ipac.caltech.edu/TAP/async/64618972/results/result"
            }
          ],
          "jobInfo": {
            "jobUrl": "https://irsa.ipac.caltech.edu/TAP/async/64618972",
            "title" : "allwise_p3as_psd - irsa"                         ,
            "userId": "Guest"
          }
        },
        {
          "phase": "COMPLETED",
          "jobId": "facade-3333",
          "meta": {
            "svcId": "IRSA",
            "jobId": "facade-3333",
            "type": "SEARCH",
          },
          "startTime": "1970-01-01T00:00:00Z",
          "endTime": "1970-01-01T00:00:00Z",
          "executionDuration": 259200,
          "results": [
            {
              "id": "result",
              "href": "https://irsa.ipac.caltech.edu/TAP/async/64618972/results/result"
            }
          ],
          "jobInfo": {
            "jobUrl": "https://irsa.ipac.caltech.edu/TAP/async/64618972",
            "title" : "allwise_p3as_psd - irsa"                         ,
            "userId": "Guest"
          }
        }]
        """.stripIndent();
        JSONArray json = (JSONArray) Util.Try.it(() -> new JSONParser().parse(testJobs)).get();
        jobs = json.stream().map(j -> JobUtil.toJobInfo((JSONObject)j)).toList();
    }

    @Test
    public void parseJson() {
        assertEquals(jobs.size(), 3);
        assertEquals("11111111", jobs.get(0).getJobId());
        assertEquals("facade-1111", jobs.get(0).getMeta().getJobId());

        assertEquals("22222222", jobs.get(1).getJobId());
        assertEquals("facade-2222", jobs.get(1).getMeta().getJobId());
        assertEquals("facade-3333", jobs.get(2).getJobId());
        assertEquals("facade-3333", jobs.get(2).getMeta().getJobId());
    }

    @Test
    public void parseUwsJob() throws Exception {
        JobInfo uwsJob = UwsJobProcessor.convertToJobInfo(UwsJobProcessor.parse( new ByteArrayInputStream(xmlJob.getBytes())));
        assertEquals(uwsJob.getJobId(), "1242749568029");
        assertEquals(uwsJob.getCreationTime().toString(), "2009-05-19T17:01:44.038Z");
        assertEquals(uwsJob.getStartTime().toString(), "2009-05-19T17:12:48.038Z");
        assertEquals(uwsJob.getEndTime().toString(), "2009-05-19T17:12:49.041Z");
        assertEquals(uwsJob.getExecutionDuration(), 86400);
        assertEquals(uwsJob.getParameters().get("scaleFactor"), "1.8");
        assertEquals(uwsJob.getParameters().get("image"), "http://myserver.org/uws/jobs/jobid123/param/image");
        assertEquals(uwsJob.getResults().getFirst().id(), "correctedImage");
        assertEquals(uwsJob.getResults().getFirst().href(), "http://myserver.org/uws/jobs/jobid123/result/image");
        assertEquals(uwsJob.getResults().getFirst().size(), "3000960");
        assertEquals(uwsJob.getResults().getFirst().mimeType(), "image/fits");
        assertEquals(uwsJob.getErrorSummary().message(), "we have problem");
        assertEquals(uwsJob.getErrorSummary().type(), "transient");
        assertTrue(uwsJob.getErrorSummary().hasDetail());
        assertEquals(uwsJob.getAux().getProgress().percentComplete(), 77);
        assertEquals(uwsJob.getAux().getProgress().message(), "77 of 100 items processed");
        assertEquals(uwsJob.getAux().getProgress().itemsProcessed(), 77);
        assertEquals(uwsJob.getAux().getProgress().totalItems(), 100);
    }

    @Test
    public void parseUwsJobs() throws Exception {
        List<JobInfo> uwsJobs = UwsJobProcessor.convertToJobList( UwsJobProcessor.parse( new ByteArrayInputStream(xmlJobList.getBytes())), "http://example.org/uws/jobs");
        assertEquals(2, uwsJobs.size());
        assertEquals("job-001", uwsJobs.get(0).getJobId());
        assertEquals("http://example.org/uws/jobs/job-001", uwsJobs.get(0).getAux().getJobUrl());
        assertEquals("job-002", uwsJobs.get(1).getJobId());
        assertEquals("http://example.org/uws/jobs/job-002", uwsJobs.get(1).getAux().getJobUrl());

        String userJobs = toJsonJobList(uwsJobs, true);
        ObjectMapper mapper = getJsonMapper();
        JsonNode root = Util.Try.it(() -> mapper.readTree(userJobs)).get();
        boolean overflow = root.get("overflow").asBoolean();
        List<JobInfo> jobs = mapper.convertValue(root.get("jobs"), new TypeReference<List<JobInfo>>() {});
        assertTrue(overflow);
        assertEquals(2, jobs.size());
        assertEquals("job-001", jobs.get(0).getJobId());
        assertEquals("http://example.org/uws/jobs/job-001", jobs.get(0).getAux().getJobUrl());
        assertEquals("job-002", jobs.get(1).getJobId());
        assertEquals("http://example.org/uws/jobs/job-002", jobs.get(1).getAux().getJobUrl());

    }

    @Test
    public void convertUwsJobToJobInfo() throws Exception {
        List<JobInfo> uwsJobs = UwsJobProcessor.convertToJobList( UwsJobProcessor.parse( new ByteArrayInputStream(xmlJobList.getBytes())), "http://example.org/uws/jobs");
        List<JobInfo> jobInfos = uwsJobs.stream().map(ref -> mergeJobInfo(null, ref, "IRSA", "TAP")).toList();
        assertEquals(2, jobInfos.size());
        assertEquals("job-001", jobInfos.get(0).getJobId());
        assertEquals("http://example.org/uws/jobs/job-001", jobInfos.get(0).getAux().getJobUrl());
        assertEquals("job-002", jobInfos.get(1).getJobId());
        assertEquals("http://example.org/uws/jobs/job-002", jobInfos.get(1).getAux().getJobUrl());
        assertEquals("IRSA", jobInfos.get(0).getMeta().getSvcId());
        assertEquals(Job.Type.TAP, jobInfos.get(0).getMeta().getType());
    }

}
