/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.core.Util.Try;
import edu.caltech.ipac.firefly.server.util.DownloadScript;
import edu.caltech.ipac.firefly.server.util.EMailUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;
import java.util.List;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.*;
import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.getDownloadURL;
import static edu.caltech.ipac.firefly.server.util.DownloadScript.makeScriptFilename;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 2/19/25
 *
 * @author loi
 * @version : $
 */
public class EmailNotification implements JobCompletedHandler {
    static String contact = AppProperties.getProperty("mail.contact", "support@acme.com");
    static String subject = AppProperties.getProperty("mail.subject", "Your Job Has Completed");
    static String success = """
        Dear %s,
        
        Your job has successfully completed. You can now download the generated data using the links below:
        
        Curl Script: %s
        
        Wget Script: %s
        
        Direct URLs: %s
        
        For your convenience, the curl and wget scripts will automate the download process. Simply run the appropriate script in your terminal.
        
        If you need help, contact us at %s.
        """.stripIndent();

    static String failure = """
        Dear %s,
        
        Your job has completed, but unfortunately, it failed.
        
        Details:
        
        Job ID: %s
        Error Message: %s
        If you need help troubleshooting, please contact us at %s.
        """.stripIndent();

    static String searchCompleted = """
        Dear %s,
        
        Your search job has successfully completed. You can now view the results using the following link:
        %s
        
        If you need help, contact us at %s.
        """.stripIndent();

    static String zipSuccess = """
        Dear %s,
        
        Your job has successfully completed. You can now download the packaged data using any of the scripts or links below:
        
        Curl Script: %s
        
        Wget Script: %s
        
        Direct URLs: %s
        
        The curl and wget scripts provide two ways to automate the download process -- simply run either script in your terminal to download all of the zip files. The third link here provides a list of URLs that point to the packaged zip files. These scripts and links should work for 72 hours. To uncompress the files you have downloaded, doubleclick on them, or type "unzip foo.zip". To unzip multiple files at once, type "unzip '*.zip'" (the single quotes are important), or "unzip *.zip" -- you have to escape the wildcard. Some Windows users have reported having difficulty unzipping files; we recommend using 7-zip (https://www.7-zip.org/download.html).
        
        If you need help, contact us at %s.
        """.stripIndent();


    public void processEvent(JobManager.JobCompletedEvent ev, JobInfo jobInfo) {
        sendNotification(jobInfo);
    }

    public static void sendNotification(JobInfo jobInfo) {
        String email = jobInfo.getAux().getUserEmail();
        String name = jobInfo.getAux().getUserName();
        name = isEmpty(name) ? "Astronomer" : name;
        Job.Type type = jobInfo.getMeta().getType();

        if (!jobInfo.getMeta().getSendNotif()) return;       // notification is disabled/off for this job

        if (isEmpty(email)) {
            Logger.getLogger().info("No email address found for job: %s;  skip Email Notification".formatted(jobInfo.getJobId()));
            return;
        }
        if (jobInfo.getError() != null) {

            String msg = failure.formatted(name, jobInfo.getJobId(), jobInfo.getError().msg(), contact);
            Try.it(() -> EMailUtil.sendMessage(new String[]{email}, null, null, subject, msg))
                    .getOrElse(e -> Logger.getLogger().error(e));

        } else if (type == Job.Type.PACKAGE) {
            try {
                String sugName = ifNotNull(jobInfo.getAux().getTitle()).getOrElse("Download");
                String curlScript = getDownloadURL(makeScript(jobInfo, Curl), makeScriptFilename(Curl, sugName), jobInfo.getMeta().getAppUrl());
                String wgetScript = getDownloadURL(makeScript(jobInfo, Wget), makeScriptFilename(Wget, sugName), jobInfo.getMeta().getAppUrl());
                String directUrls = getDownloadURL(makeScript(jobInfo, ScriptAttributes.URLsOnly), makeScriptFilename(URLsOnly, sugName), jobInfo.getMeta().getAppUrl());
                String msg = zipSuccess.formatted(name, curlScript, wgetScript, directUrls, contact);
                EMailUtil.sendMessage(new String[]{email}, null, null, subject, msg);
            } catch (Exception e) {
                Logger.getLogger().error(e);
            }

        } else if (type == Job.Type.SCRIPT) {

            String msg = success.formatted(name, getUrl(jobInfo, Curl), getUrl(jobInfo, Wget), getUrl(jobInfo, URLsOnly), contact);
            Try.it(() -> EMailUtil.sendMessage(new String[]{email}, null, null, subject, msg))
                    .getOrElse(e -> Logger.getLogger().error(e));
        } else {

            String msg = searchCompleted.formatted(name, jobInfo.getMeta().getAppUrl(), contact);
            Try.it(() -> EMailUtil.sendMessage(new String[]{email}, null, null, subject, msg))
                    .getOrElse(e -> Logger.getLogger().error(e));
        }
    }

    public static String getUrl(JobInfo jobInfo, ScriptAttributes type) {
        String matcher = type == URLsOnly ? "list" : type.name().toLowerCase();
        return jobInfo.getResults().stream()
                .filter(r -> r.id().contains(matcher))
                .map(r -> r.href()).findFirst().orElse(null);
    }

    public static File makeScript(JobInfo jobInfo, ScriptAttributes type) {
        List<DownloadScript.UrlInfo> urlInfos = ifNotNull(jobInfo.getResults())
                .get(list -> list.stream().map(r -> new DownloadScript.UrlInfo(r.href(), null, null)).toList());
        String id = jobInfo.getJobId();
        String fname = type == Curl ? "curl_script_%s.sh" : type   == Wget ? "wget_script_%s.sh" : "urls_%s.txt";
        fname = fname.formatted(id.substring(id.length()-4));      // safe; id is always greater than 4 chars
        File script = new File(JobUtil.getJobWorkDir(id), fname);
        DownloadScript.createScript(script, urlInfos, "script for Job %s".formatted(jobInfo.getJobId()), type);
        return script;
    }
}
