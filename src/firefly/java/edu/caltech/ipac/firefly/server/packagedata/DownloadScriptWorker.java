/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.packagedata;

import com.google.common.net.MediaType;
import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.DownloadScript;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsServerParams;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.firefly.core.background.Job;
import edu.caltech.ipac.firefly.core.background.JobInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobManager.getJobInfo;
import static edu.caltech.ipac.firefly.core.background.JobManager.updateJobInfo;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.*;
import static edu.caltech.ipac.firefly.server.packagedata.PackagingWorker.makeDownloadUrl;
import static edu.caltech.ipac.firefly.server.ws.WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Downloads a script with products from the List<FileInfo>
 *
 * @author kartik
 * @version : $
 */
public final class DownloadScriptWorker implements Job.Worker {

    private static final String DOWNLOAD_SERVLET_PATH = "servlet/Download";
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    private Job job;
    private List<String> failed = new ArrayList<>();
    private int curFileInfoIdx = 0;
    private long lastUpdatedTime = System.currentTimeMillis();
    private int lastUpdatedPct;
    private int totalFiles;
    private String wsDestPath;
    private boolean hasErrors;
    private String suggestedName;

    private static final List<ScriptAttributes> wget= Arrays.asList(Wget);
    private static final List<ScriptAttributes> curl= Arrays.asList(Curl);
    private static final List<ScriptAttributes> text= Arrays.asList(URLsOnly);

    public static final String SCRIPT = "scriptType";

    private static final Map<String, List<ScriptAttributes>> scriptTypeMap = new HashMap<>();
    static {
        scriptTypeMap.put("wget", wget);
        scriptTypeMap.put("curl", curl);
        scriptTypeMap.put("urls", text);
    }

    private static final Map<String, String> scriptTypeToExtension = new HashMap<>();
    static {
        scriptTypeToExtension.put("wget", ".sh");
        scriptTypeToExtension.put("curl", ".sh");
        scriptTypeToExtension.put("urls", ".txt");
    }

    public Job.Type getType() { return Job.Type.PACKAGE; }

    public void setJob(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    public String getLabel() { return getJob().getParams().getDownloadRequest().getTitle(); }

    public String doCommand(SrvParam params) throws Exception {

        DownloadRequest dlreq = params.getDownloadRequest();

        String scriptType = dlreq.getParam(SCRIPT); //curl, wget, or "urls" for plain list of URLs

        wsDestPath = isEmpty(dlreq.getParam(DownloadRequest.FILE_LOC)) ? null : dlreq.getParam(DownloadRequest.WS_DEST_PATH);

        SearchProcessor processor = SearchManager.getProcessor(dlreq.getRequestId());
        if (processor instanceof FileGroupsProcessor) {
            List<FileGroup> result = ((FileGroupsProcessor) processor).getData(dlreq);

            totalFiles = result.stream().mapToInt(fg -> fg.getSize()).sum();

            suggestedName = dlreq.getBaseFileName();

            try {
                for (FileGroup fg : result) {
                    List<URL> urlList = new ArrayList<>();
                    for (FileInfo fi : fg) {
                        updateJobProgress();
                        HttpServiceInput req = fi.getRequestInfo();
                        String url = req.getRequestUrl();
                        urlList.add(new URL(url));
                    }

                    File outFile = File.createTempFile("download-results", ".sh", ServerContext.getStageWorkDir());

                    //retrieve attributes based on scriptType
                    List<ScriptAttributes> attribute = scriptTypeMap.getOrDefault(scriptType.toLowerCase(), text);
                    DownloadScript.composeDownloadScript(outFile, "Euclid", urlList, attribute);

                    if (outFile.exists()) {
                        String fileExtension = scriptTypeToExtension.getOrDefault(scriptType.toLowerCase(), ".txt");
                        getJob().addResult(new JobInfo.Result(makeDownloadUrl(outFile, suggestedName + fileExtension), null, MediaType.PLAIN_TEXT_UTF_8.toString(), outFile.length() + ""));

                        // handle 'save to Workspace' option:  pushes downloaded script (.sh or .txt) to workspace
                        if (!isEmpty(wsDestPath)) {
                            String wsFilePath = wsDestPath + suggestedName + fileExtension;
                            try {
                                new WsServerUtils().putFile(
                                        new WsServerParams().set(CURRENTRELPATH, wsFilePath),
                                        outFile);
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        }
                    }
                }
            }
            catch(Exception e){
                failed.add(e.getMessage());
            }
            hasErrors = hasErrors || !failed.isEmpty();
        }
        else {
            throw new DataAccessException("Operation aborted:" + dlreq.getRequestId(), new IllegalArgumentException("Unable to resolve a search processor for this request"));
        }

        updateJobInfo(getJob().getJobId(), ji -> {
            // JobInfo completion update
            String summary = String.format("%,d files were processed.", totalFiles);
            if (hasErrors) summary += "\nPlease, note:  There were error(s) while processing your request.";
            ji.setProgress(100);
            ji.setProgressDesc(summary);
            ji.setSummary(summary);
        });
        getJob().setPhase(JobInfo.Phase.COMPLETED);

        PackagedEmail.send(getJob().getJobId());

        return "";
    }

    private void updateJobProgress() throws DataAccessException.Aborted {
        Job job = getJob();
        if (job != null) {
            JobInfo.Phase phase = ifNotNull(getJobInfo(getJob().getJobId())).eval(JobInfo::getPhase);
            if (phase == JobInfo.Phase.ABORTED) throw new DataAccessException.Aborted();
            if (System.currentTimeMillis() - lastUpdatedTime > 2000) {
                lastUpdatedTime = System.currentTimeMillis();
                int pct = (int) ((float)curFileInfoIdx / totalFiles * 100);
                if ( pct != lastUpdatedPct) {
                    lastUpdatedPct = pct;
                    job.progress(pct, String.format("%d of %d completed", curFileInfoIdx, totalFiles));
                }
            }
        }
    }
    
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
