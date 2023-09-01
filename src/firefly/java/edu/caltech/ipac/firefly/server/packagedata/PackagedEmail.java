/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;
/**
 * User: roby
 * Date: 1/27/11
 * Time: 10:07 AM
 */


import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.util.DownloadScript;
import edu.caltech.ipac.firefly.server.util.EMailUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.firefly.core.background.JobInfo;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.Curl;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.URLsOnly;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.Unzip;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.Wget;
import static edu.caltech.ipac.firefly.data.ServerParams.EMAIL;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


/**
 * @author Trey Roby
 */
public class PackagedEmail {

    private final static String DEF_SUCCESS_MESSAGE =   "Your packaging request has been completed";
    private final static String MAIL_SUCCESS_MESSAGE = AppProperties.getProperty("download.mail.success", DEF_SUCCESS_MESSAGE);
    private static final String BASE_SERVLET = "servlet/Download?"+ AnyFileDownload.LOG_PARAM +"=true&" + AnyFileDownload.FILE_PARAM +"=";
    private static final String RET_FILE = "&"+AnyFileDownload.RETURN_PARAM+"=";

    private static final List<ScriptAttributes> wget= Arrays.asList(Wget);
    private static final List<ScriptAttributes> wgetUnzip= Arrays.asList(Wget,Unzip);
    private static final List<ScriptAttributes> curl= Arrays.asList(Curl);
    private static final List<ScriptAttributes> curlUnzip= Arrays.asList(Curl,Unzip);
    private static final List<ScriptAttributes> text= Arrays.asList(URLsOnly);

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final Logger.LoggerImpl statsLogger = Logger.getLogger(Logger.DOWNLOAD_LOGGER);


    /**
     * Send an email to the user that his files are ready
     * @param jobInfo JobInfo
     */
    public static void send(JobInfo jobInfo) {
        DownloadRequest dlreq = jobInfo.getSrvParams().getDownloadRequest();
        String email = dlreq.getEmail();

        if (isEmpty(email))   email = jobInfo.getSrvParams().getOptional(EMAIL);

        String dataSource = dlreq.getDataSource();

        if (email == null) return;

        StringWriter sw = new StringWriter();
        try{
            String label= jobInfo.getLabel();
            JobInfo.Phase phase = jobInfo.getPhase();
            if (phase.equals(JobInfo.Phase.ABORTED)) {
                sw.append("\nYour packaging was aborted.\n\n");
            } else if (phase.equals(JobInfo.Phase.ERROR)) {
                sw.append("\nYour packaging request did not complete.\n\n");
            } else if (phase.equals(JobInfo.Phase.COMPLETED)) {
                sw.append("\n");
                sw.append(MAIL_SUCCESS_MESSAGE);
                sw.append("\n\n");
            } else {
                logger.warn(jobInfo.getJobId(),
                                         "Cannot send completion email. Unexpected state in JobInfo: " + phase);
                return;
            }

            sw.append(jobInfo.getSummary());

            if (jobInfo.getResults().size() > 1) {
                int cnt= jobInfo.getResults().size();
                sw.append("\n");
                sw.append("You have ");
                sw.append(Integer.toString(cnt));
                sw.append(" zip files available.\n\n");
                sw.append("This email contains two sections.\n");
                sw.append("Section 1 will give you a list of retrieval script options using wget, curl or a simple list of URLs.\n");
                sw.append("Section 2 will give the direct url links for each zip file.\n");
                sw.append("\n\n");
                sw.append("-------------------------- Section 1: Retrieval Scripts --------------------------\n");
                sw.append("wget script (best for Unix/Linux):\n");
                sw.append(makeScriptAndLink(jobInfo, dataSource, wget)).append("\n\n");
                sw.append("wget/unzipping script (best for Unix/Linux):\n");
                sw.append(makeScriptAndLink(jobInfo, dataSource, wgetUnzip)).append("\n\n");
                sw.append("curl script (best for Mac):\n");
                sw.append(makeScriptAndLink(jobInfo, dataSource, curl)).append("\n\n");
                sw.append("curl/unzipping script (best for Mac):\n");
                sw.append(makeScriptAndLink(jobInfo, dataSource, curlUnzip)).append("\n\n");
                sw.append("text file of urls (best for Windows, see Windows advice below):\n");
                sw.append(makeScriptAndLink(jobInfo, dataSource, text)).append("\n\n");
                sw.append("\n\n");
                sw.append("-------------------------- Section 2: Download URLs --------------------------\n");
            }
            else {
                sw.append("\n\n-------------------------- Download URL --------------------------\n");
            }

            // print out result url
            sw.append("\n\n");
            jobInfo.getResults().forEach(result -> sw.append("\n").append(result.href()));


            if (jobInfo.getResults().size() > 1) {
                sw.append("\n\n\n\n");
                sw.append("Advice for Windows users:\n");
                sw.append("   1. Go to the Windows wget web page at: https://www.gnu.org/software/wget/\n");
                sw.append("   2. Scroll to the Download section and retrieve the wget installation.\n");
                sw.append("   3. Install wget and add the binary to your path.\n");
                sw.append("   4. Download the text only file of urls (above, in section 1)\n");
                sw.append("   5. At the command prompt: wget --content-disposition -i <file_of_urls_downloaded.txt>\n");

            }


            sw.flush();
            EMailUtil.sendMessage(new String[]{email}, null, null, "Download Status - " + label, sw.toString());
        } catch(Throwable e) {
            logger.warn(jobInfo.getJobId()!=null ? jobInfo.getJobId() : "Unknown Background ID",
                                     "Failed to send completion email to "+email+": "+e.getMessage() + sw.toString());
        }
    }


    public static String makeScriptAndLink(JobInfo jobInfo, String dataSource, List<ScriptAttributes> attributes) {

        String scriptUrl = null;
        List<URL> urlList= new ArrayList<>(jobInfo.getResults().size());
        for(JobInfo.Result part : jobInfo.getResults()) {
            try {
                urlList.add(new URL(part.href()));
            } catch (MalformedURLException e) {
                logger.warn("Bad url for download script: " + part + "Background ID: " + jobInfo.getJobId());
            }
        }
        String fName = "download-results";

        if (urlList.size()>0) {
            String  ext = attributes.contains(ScriptAttributes.URLsOnly) ? ".txt" : ".sh";
            try {
                File outFile = File.createTempFile(fName, ext, ServerContext.getStageWorkDir());
                String retFile= fName+ext;
                DownloadScript.composeDownloadScript(outFile, dataSource, urlList, attributes);
                String fStr= ServerContext.replaceWithPrefix(outFile);
                try {
                    fStr = URLEncoder.encode(fStr, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // if it fails, then use the original
                }

                if (fStr!= null) {

                    scriptUrl=  BASE_SERVLET  + fStr + RET_FILE + retFile;
                    logger.info("download script built, returning: " + outFile, "Background ID: " + jobInfo.getJobId());
                    statsLogger.stats("create_script", "fname", outFile);
                }
            } catch (IOException e) {
                logger.warn(e,"Could not create temp file",
                        "Background ID: " + jobInfo.getJobId(),
                        "file root: "  + fName,
                        "ext: "+ ext);
            }
        }
        else {
            logger.warn("Could not build a download script list, urlList length==0",
                    "Background ID: " + jobInfo.getJobId());
        }
        return ServerContext.getRequestOwner().getBaseUrl() + scriptUrl;
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



}

