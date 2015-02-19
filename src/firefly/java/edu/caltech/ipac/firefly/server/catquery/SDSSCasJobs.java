/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.download.URLDownload;
import edu.jhu.Cas.Services.CJJob;
import edu.jhu.Cas.Services.JobsLocator;
import edu.jhu.Cas.Services.JobsSoap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 * This class contains helper methods to query SDSS catalog using CasJobs.
 * It is relying on casjob.jar, which is a command line interface to CasJobs
 * and contains stubs for CasJobs web interface methods.
 * A CasJob account must be created on SDSS skyserver, and user id
 * and password recorded in CasJobs.config file in the application
 * config directory.
 * @author tatianag
 */
public class SDSSCasJobs {

    private static int WAITSTART = 4000;
    private static int WAITMAX = 20*1000;
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static String QUERY = "SELECT u.up_id,x.distance,"+
            SDSSQuery.SELECT_COLUMNS+
            " INTO %TBL%_out "+
            " FROM MyDB.%TBL% u "+
              " CROSS APPLY %FUNCTION%( u.ra, u.dec, %RAD_ARCMIN% ) as x "+
              " JOIN PhotoObj p ON p.objID = x.objID"+
              " ORDER BY u.up_id,x.distance";


    private static String getQuery(String tableName, boolean nearestOnly, String radiusArcMin) {
        return QUERY.replaceAll("%TBL%", tableName).
                replace("%RAD_ARCMIN%", radiusArcMin).
                replace("%FUNCTION%", nearestOnly ? SDSSQuery.NEAREST : SDSSQuery.NEARBY);
    }

    private static String readFile( String file ) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        String line;
        while( ( line = reader.readLine() ) != null ) {
            stringBuilder.append( line );
            stringBuilder.append( ls );
        }

        return stringBuilder.toString();
    }

    public static int getWait(int prevwait){
    	int nextwait = prevwait * 2;
    	return nextwait < WAITMAX ? nextwait : WAITMAX;
    }

    public static CJJob poll(long jobid, String jobtitle, JobsSoap cJobs, long WSID, String PW) throws InterruptedException, IOException {
        if(jobid==-1) {
            _log.briefInfo("SDSS CasJob failed: unknown syntax error");
            return null;
        }
        _log.briefInfo("SDSS CasJob "+jobtitle+" submitted, Job ID is "+jobid);

        int w = WAITSTART;
        CJJob j;
        int retries = 1;
        for(;;){
            try {
                j = cJobs.getJobs(WSID,PW,"jobid : "+jobid,false)[0];
                if(j.getStatus() == 4 || j.getStatus() == 5)
                    break;
                Thread.sleep(w);
                w = getWait(w);
            } catch (IOException e) {
                if (retries > 0) {
                    retries--;
                    _log.briefInfo("SDSS CasJobs poll failed (jobid="+jobid+", will retry.");
                    Thread.sleep(w);
                    //continue;
                } else {
                    throw e;
                }
            }
        }
        switch(j.getStatus()){
            case 5:
                _log.briefInfo("SDSS CasJob "+jobtitle+" complete. "+
                        (j.getRows()>0 ? "Rows: "+j.getRows() : "")+
                        (j.getOutputLoc() != null ? "url: "+j.getOutputLoc():""));
                break;
            default:
                _log.briefInfo("SDSS CasJob " + jobtitle + " failed. Error: " + j.getError());
                return null;
        }
        return j;
    }

    public static void getCrossMatchResults(File sdssUFile, boolean nearestOnly, String radiusArcMin, File outFile)
        throws EndUserException {
        Date start = new Date();


        String wsidVal = AppProperties.getProperty("sdss.casjobs.wsid", null);
        if (wsidVal == null) { throw new EndUserException("SDSS CasJob query failed",
                            "sdss.casjobs.wsid property is not set"); }
        long WSID = Long.parseLong(wsidVal);

        String PW = AppProperties.getProperty("sdss.casjobs.password", null);
        if (PW == null) { throw new EndUserException("SDSS CasJob query failed",
                                "sdss.casjobs.password property is not set"); }

        String TARGET = AppProperties.getProperty("sdss.casjobs.default_target", "DR10");
        int QUEUE = Integer.parseInt(AppProperties.getProperty("sdss.casjobs.default_queue", "4"));

        JobsLocator locator = new JobsLocator();
        JobsSoap cJobs;
        try {
            cJobs = locator.getJobsSoap12();
        } catch (Exception e) {
            _log.error(e, "SDSS CasJob query failed. Unable to get handle to the service.");
            throw new EndUserException("SDSS CasJob query failed",
                    "Unable to get handle to the service: "+e.getMessage());

        }

        if (cJobs == null) {
            throw new EndUserException("SDSS CasJob query failed",
                                "Unidentified service handle");
        }

        boolean tableUploaded = false;
        boolean resultTableCreated = false;
        String uploadfile = sdssUFile.getAbsolutePath();
        String tblname = uploadfile.substring(uploadfile.lastIndexOf(File.separator)+1,uploadfile.lastIndexOf("."));
        try {
            String uploaddata = readFile(uploadfile);
            cJobs.uploadData(WSID,PW,tblname,uploaddata);
            tableUploaded = true;

            long jobid;

            // submit query
            try{
                jobid = cJobs.submitJob(WSID, PW, getQuery(tblname, nearestOnly, radiusArcMin), TARGET, tblname + "-task", QUEUE);
                if (jobid>0) resultTableCreated = true;
            } catch(Exception e){
                _log.error("SDSS CasJob query failed. " + CasJobsCLOnlyTheError(e));
                throw new EndUserException("SDSS CasJob query failed",
                        "Unable to submit job.");
            }
            CJJob j = poll (jobid, "Query", cJobs, WSID, PW);
            if (j == null) {
                throw new EndUserException("SDSS CasJob query failed",
                        "Unable to get status for jobid "+jobid+", table "+tblname+"_out");
            }

            if (j.getRows()>0) {
                // submit extract job
                try{
                    jobid = cJobs.submitExtractJob(WSID,PW,tblname+"_out","CSV");
                } catch(Exception e){
                    _log.error("SDSS CasJob extract job failed.", CasJobsCLOnlyTheError(e));
                    throw new EndUserException("SDSS CasJob query failed",
                            "Unable to submit extract job for table "+tblname+"_out");
                }
                j = poll (jobid, "Extract job", cJobs, WSID, PW);
                if (j == null) {
                    throw new EndUserException("SDSS CasJob query failed",
                            "Unable to get status for extract jobid "+jobid+", table "+tblname+"_out");
                }
                if (j.getOutputLoc() != null) {
                    try {
                        URL url = new URL(j.getOutputLoc());
                        URLConnection conn = URLDownload.makeConnection(url);
                        conn.setRequestProperty("Accept", "*/*");

                        URLDownload.getDataToFile(conn, outFile);
                    } catch (Exception e) {
                        throw new EndUserException("SDSS CasJob query failed",
                                "Unable to get to download results from url "+j.getOutputLoc());
                    }
                }
            } else {
                // no rows returned by the query
                // will return with empty file
            }

        } catch (Exception e) {
            _log.error(e, "SDSS CasJob query failed");
            throw new EndUserException("SDSS CasJob query failed",
                    e.getMessage());
        } finally {
            // drop the upload table
            if (tableUploaded) {
                String qry = "DROP TABLE "+tblname;
                try {
                    cJobs.executeQuickJob(WSID,PW,qry,"MyDB","drop "+tblname,true);
                } catch (Exception e) {
                    _log.briefInfo("SDSS CasJob: unable to drop " + tblname + ": " + e.getMessage());
                }
            }

            // drop result table
            if (resultTableCreated) {
                String qry = "DROP TABLE "+tblname+"_out";
                try {
                    cJobs.executeQuickJob(WSID,PW,qry,"MyDB","drop "+tblname+"_out",true);
                } catch (Exception e) {
                    _log.briefInfo("SDSS CasJob failed: unable to drop " + tblname + "_out: " + e.getMessage());
                }
            }

            Date end = new Date();
            _log.briefInfo("SDSS CasJob completed in " + (end.getTime() - start.getTime()) + "ms");

        }

    }

    public static String CasJobsCLOnlyTheError(Exception e){
        String msg = e.getMessage();
        if(e instanceof org.apache.axis.AxisFault)
            msg = msg.replaceFirst("[\\d\\D]*-->[^:]*: ","");
        msg = CasJobsCLRemoveStackTrace(msg);
        return msg;
    }

    public static String CasJobsCLRemoveStackTrace(String s){
        s = s.replaceAll("at[^\\n]*\\n","");
        s = s.replaceAll("\\s*---[^-]*---\\s*","");
        return s;
    }



}
