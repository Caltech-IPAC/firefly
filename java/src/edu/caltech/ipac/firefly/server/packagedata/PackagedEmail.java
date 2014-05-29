package edu.caltech.ipac.firefly.server.packagedata;
/**
 * User: roby
 * Date: 1/27/11
 * Time: 10:07 AM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundPart;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.server.util.EMailUtil;
import edu.caltech.ipac.util.AppProperties;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.core.background.BackgroundReport.ScriptAttributes;
import static edu.caltech.ipac.firefly.core.background.BackgroundReport.ScriptAttributes.Curl;
import static edu.caltech.ipac.firefly.core.background.BackgroundReport.ScriptAttributes.URLsOnly;
import static edu.caltech.ipac.firefly.core.background.BackgroundReport.ScriptAttributes.Unzip;
import static edu.caltech.ipac.firefly.core.background.BackgroundReport.ScriptAttributes.Wget;


/**
 * @author Trey Roby
 */
public class PackagedEmail {

    private final static String DEF_SUCCESS_MESSAGE =   "Your packaging request has been completed";
    public final static String MAIL_SUCCESS_MESSAGE = AppProperties.getProperty("download.mail.success",
                                                                                DEF_SUCCESS_MESSAGE);

    private static final List<ScriptAttributes> wget= Arrays.asList(Wget);
    private static final List<ScriptAttributes> wgetUnzip= Arrays.asList(Wget,Unzip);
    private static final List<ScriptAttributes> curl= Arrays.asList(Curl);
    private static final List<ScriptAttributes> curlUnzip= Arrays.asList(Curl,Unzip);
    private static final List<ScriptAttributes> text= Arrays.asList(URLsOnly);


    /**
     * Send an email to the user that his files are ready
     * @param email the email address
     * @param info the PackageInfo object
     */
    public static void send(String email, PackageInfoCacher info) {
        send(email, info, null);
    }


    /**
     * Send an email to the user that his files are ready
     * @param email the email address
     * @param info the PackageInfo object
     * @param report All the information about what was packaged
     */
    public static void send(String email, PackageInfoCacher info, BackgroundReport report) {
        StringWriter sw = new StringWriter();
        try{
            if (report==null) report= info.getReport();
            String title= info.getTitle();
            BackgroundState state = report.getState();
            if (state.equals(BackgroundState.USER_ABORTED)) return;
            else if (state.equals(BackgroundState.CANCELED)) {
                sw.append("\nYour packaging was canceled.\n\n");
            }
            else if (state.equals(BackgroundState.FAIL)) {
                sw.append("\nYour packaging request did not complete.\n\n");
            } else if (state.equals(BackgroundState.SUCCESS)) {
                sw.append("\n");
                sw.append(MAIL_SUCCESS_MESSAGE);
                sw.append("\n\n");
            } else {
                PackageMaster.logPIDWarn(report.getID(),
                                         "Cannot send completion email. Unexpected state in PackageReport: "+state);
                return;
            }
            int numMessages = report.getNumMessages();
            if (numMessages > 0) {
                sw.append("Please, note:");
                for (int i=0; i<numMessages; i++) {
                    sw.append("\n    ").append(report.getMessage(i));
                }
            }

            int cnt= report.getPartCount();
            if (cnt>1) {
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
                sw.append(makeScriptAndLink(report, wget)).append("\n\n");
                sw.append("wget/unzipping script (best for Unix/Linux):\n");
                sw.append(makeScriptAndLink(report, wgetUnzip)).append("\n\n");
                sw.append("curl script (best for Mac):\n");
                sw.append(makeScriptAndLink(report, curl)).append("\n\n");
                sw.append("curl/unzipping script (best for Mac):\n");
                sw.append(makeScriptAndLink(report, curlUnzip)).append("\n\n");
                sw.append("text file of urls (best for Windows, see Windows advice below):\n");
                sw.append(makeScriptAndLink(report, text)).append("\n\n");
                sw.append("\n\n");
                sw.append("-------------------------- Section 2: Download URLs --------------------------\n");
            }
            else {
                sw.append("\n\n-------------------------- Download URL --------------------------\n");
            }


            PackagedBundle b;
            for(BackgroundPart p : report) {
                b= (PackagedBundle)p;
                if (b.getUrl() != null) {
                    sw.append(b.getUrl()).append("\n\n");
                }
            }


            if (cnt>1) {
                sw.append("\n\n\n\n");
                sw.append("Advice for Windows users:\n");
                sw.append("   1. Go to the Windows wget web page at: http://gnuwin32.sourceforge.net/packages/wget.htm\n");
                sw.append("   2. Scroll to the Download section and retrieve the wget installation.\n");
                sw.append("   3. Install wget and add the binary to your path.\n");
                sw.append("   4. Download the text only file of urls (above, in section 1)\n");
                sw.append("   5. At the command prompt: wget --content-disposition -i <file_of_urls_downloaded.txt>\n");

            }


            sw.flush();
            EMailUtil.sendMessage(new String[]{email}, null, null, "Download Status - " + title, sw.toString());
            report.addAttribute(BackgroundReport.JobAttributes.EmailSent);
            info.setReport(report);
        } catch(Throwable e) {
            PackageMaster.logPIDWarn(report.getID(),
                                     "Failed to send completion email to "+email+": "+e.getMessage()+sw.toString());
        }
    }


    private static String makeScriptAndLink(BackgroundReport report, List<ScriptAttributes> attList) {
        BackgroundEnv.ScriptRet retval= BackgroundEnv.createDownloadScript(report.getID(), "download-results",
                                                                           report.getDataSource(), attList);
        return ServerContext.getRequestOwner().getBaseUrl() + retval.getServlet();
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
