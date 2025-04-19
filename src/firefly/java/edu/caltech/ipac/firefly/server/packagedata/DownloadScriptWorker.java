/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.packagedata;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import edu.caltech.ipac.firefly.core.background.JobUtil;
import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.server.SrvParam;
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

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobManager.updateJobInfo;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.*;
import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.getDownloadURL;
import static edu.caltech.ipac.firefly.server.util.DownloadScript.makeScriptFilename;
import static edu.caltech.ipac.firefly.server.ws.WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.firefly.core.Util.Try;

/**
 * Downloads a script with products from the List<FileInfo>
 *
 * @author kartik
 * @version : $
 */
public final class DownloadScriptWorker implements Job.Worker {

    public static final Job.Type JOB_TYPE = Job.Type.SCRIPT;

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    public static final String SCRIPT = "scriptType";

    private Job job;

    public Job.Type getType() { return JOB_TYPE; }

    public void setJob(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    public String getLabel() { return getJob().getParams().getDownloadRequest().getTitle(); }

    public String doCommand(SrvParam params) throws Exception {

        DownloadRequest dlreq = params.getDownloadRequest();
        String scriptType = dlreq.getParam(SCRIPT); //curl, wget, or "urls" for plain list of URLs;  (LLY: should ignore and create all 3 types)
        String suggestedName = ifNotNull(dlreq.getBaseFileName()).getOrElse("download");
        String dataDesc = dlreq.getDataSource();
        String wsDestPath = isEmpty(dlreq.getParam(DownloadRequest.FILE_LOC)) ? null : dlreq.getParam(DownloadRequest.WS_DEST_PATH);

        sendJobUpdate(ji -> ji.getMeta().setProgress(10, "Validating inputs"));

        SearchProcessor processor = SearchManager.getProcessor(dlreq.getRequestId());
        if (!(processor instanceof FileGroupsProcessor)) {
            throw new DataAccessException("Operation aborted:" + dlreq.getRequestId(), new IllegalArgumentException("Unable to resolve a search processor for this request"));
        }

        sendJobUpdate(ji -> ji.getMeta().setProgress(20, "Processing the request"));

        List<FileGroup> result = ((FileGroupsProcessor) processor).getData(dlreq);
        int totalFiles = result.stream().mapToInt(fg -> fg.getSize()).sum();

        sendJobUpdate(ji -> ji.getMeta().setProgress(80, "Generating the results"));

        File curlScript = makeScript(result, Curl, dataDesc);
        File wgetScript = makeScript(result, Wget, dataDesc);
        File urlsFile = makeScript(result, URLsOnly, dataDesc);

        // handle 'save to Workspace' option:  pushes downloaded script (.sh or .txt) to workspace  (LLY: should probably ignore.  what is the use case?)
        if (!isEmpty(wsDestPath)) {
            Try.it(() -> new WsServerUtils().putFile(new WsServerParams().set(CURRENTRELPATH, wsDestPath + makeScriptFilename(Curl, suggestedName)), curlScript));
            Try.it(() -> new WsServerUtils().putFile(new WsServerParams().set(CURRENTRELPATH, wsDestPath + makeScriptFilename(Wget, suggestedName)), wgetScript));
            Try.it(() -> new WsServerUtils().putFile(new WsServerParams().set(CURRENTRELPATH, wsDestPath + makeScriptFilename(URLsOnly, suggestedName)), urlsFile));
        }

        String curlScriptUrl = getDownloadURL(curlScript, makeScriptFilename(Curl, suggestedName));
        String wgetScriptUrl = getDownloadURL(wgetScript, makeScriptFilename(Wget, suggestedName));
        String urlsFileUrl = getDownloadURL(urlsFile, makeScriptFilename(URLsOnly, suggestedName));

        sendJobUpdate(ji -> {
            String summary = String.format("%,d files were processed.", totalFiles);
            ji.getMeta().setSummary(summary);
            ji.addResult(new JobInfo.Result("curl-script", curlScriptUrl, "text/x-shellscript", curlScript.length() + ""));
            ji.addResult(new JobInfo.Result("wget-script", wgetScriptUrl, "text/x-shellscript", wgetScript.length() + ""));
            ji.addResult(new JobInfo.Result("url-list", urlsFileUrl, "text/plain", urlsFile.length() + ""));
        });

        return "";
    }

    File makeScript(List<FileGroup> results, ScriptAttributes type, String dataDesc) {
        String id = getJob().getJobId();
        String fname = type == Curl ? "curl_script_%s.sh" : type   == Wget ? "wget_script_%s.sh" : "urls_%s.txt";
        fname = fname.formatted(id.substring(id.length()-4));      // safe; id is always greater than 4 chars
        File script = new File(JobUtil.getJobWorkDir(id), fname);
        DownloadScript.createScript(script, dataDesc, results, type);
        return script;
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
