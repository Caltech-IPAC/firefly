/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.packagedata;

import com.google.common.net.MediaType;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsServerParams;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.firefly.core.background.Job;
import edu.caltech.ipac.firefly.core.background.JobInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobManager.updateJobInfo;
import static edu.caltech.ipac.firefly.core.background.JobUtil.getJobWorkDir;
import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.getDownloadURL;
import static edu.caltech.ipac.firefly.server.ws.WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * failed and denied details are in each zip file.
 *
 * @author loi
 * @version : $
 */
public final class PackagingWorker implements Job.Worker {
    public static final Job.Type JOB_TYPE = Job.Type.PACKAGE;

    private static final String DOWNLOAD_SERVLET_PATH = "servlet/Download";
    private static final long MAX_ZIP_FILE_SIZE = AppProperties.getLongProperty("download.data.bytesize", 1024*1024*1024*16L);
    private final static String README_SUCCESS_TEXT = AppProperties.getProperty("download.readme.success", "");
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    private Job job;
    private File zipFile = null;
    private ZipOutputStream zout = null;
    private List<String> failed = new ArrayList<>();
    private List<String> denied = new ArrayList<>();
    private int curZipIdx = -1;
    private int startFileInfoIdx = 0;
    private int curFileInfoIdx = 0;
    private long lastUpdatedTime = System.currentTimeMillis();
    private int lastUpdatedPct;
    private int totalFiles;
    private int totalBytes;
    private int zippedBytes;
    private String suggestedName;
    private String wsDestPath;
    private boolean hasErrors;

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

        wsDestPath = isEmpty(dlreq.getParam(DownloadRequest.FILE_LOC)) ? null : dlreq.getParam(DownloadRequest.WS_DEST_PATH);

        SearchProcessor processor = SearchManager.getProcessor(dlreq.getRequestId());
        if (processor instanceof FileGroupsProcessor) {
            List<FileGroup> result = ((FileGroupsProcessor) processor).getData(dlreq);

            suggestedName = dlreq.getBaseFileName();
            newZipFile();

            totalFiles = result.stream().mapToInt(fg -> fg.getSize()).sum();

            for (FileGroup fg : result) {
                ZipHandler zipHandler = new ZipHandler((fg.getBaseDir()));
                for (FileInfo fi : fg) {

                    rotateZipFileIfNeeded();

                    updateJobProgress();

                    curFileInfoIdx++;
                    try {
                        zippedBytes += (int) zipHandler.addZipEntry(zout, fi);
                        totalBytes += zippedBytes;
                    } catch (AccessDeniedException e) {
                        denied.add(e.getMessage());
                    } catch (Exception e) {
                        failed.add(e.getMessage());
                    }
                    hasErrors = hasErrors || denied.size() > 0 || failed.size() > 0;
                }
            }

        } else {
            throw new DataAccessException("Operation aborted:" + dlreq.getRequestId(), new IllegalArgumentException("Unable to resolve a search processor for this request"));
        }
        closeZipFile();

        // JobInfo completion update
        updateJobInfo(getJob().getJobId(), ji -> {
            String summary = String.format("%,d files were packaged for a total of %,d B creating %,d zip files.", totalFiles, totalBytes, curZipIdx);
            if (hasErrors) summary += "\nPlease, note:  There were error(s) while processing your request.  See zip's README file for details.";
            ji.getMeta().setProgress(100);
            ji.getMeta().setProgressDesc(summary);
            ji.getMeta().setSummary(summary);
        });

        return "";
    }

    private void rotateZipFileIfNeeded() throws FileNotFoundException {
        if (zipFile.length() > MAX_ZIP_FILE_SIZE) {
            closeZipFile();
            newZipFile();
        }
    }

    private void updateJobProgress() throws DataAccessException.Aborted {
        if (System.currentTimeMillis() - lastUpdatedTime > 2000) {
            lastUpdatedTime = System.currentTimeMillis();
            int pct = (int) ((float)curFileInfoIdx / totalFiles * 100);
            if ( pct != lastUpdatedPct) {
                lastUpdatedPct = pct;
                sendJobUpdate(ji -> {
                    ji.getMeta().setProgress(pct);
                    ji.getMeta().setProgressDesc(String.format("%d of %d completed", curFileInfoIdx, totalFiles));
                });
            }
        }
    }

    private void closeZipFile() {
        if (zout != null) {
            int zippedFiles = curFileInfoIdx - startFileInfoIdx;
            ZipHandler.addReadmeZipEntry(zout,zipMessage(zippedFiles, zippedBytes, failed, denied));
            zout.setComment(String.format("Files %s-%s", startFileInfoIdx, curFileInfoIdx));
            String suggName = suggestedName == null ? "DownloadPackage" : suggestedName;
            suggName = "%s%s.zip".formatted(suggName, curZipIdx > 0 ? "-part" + curZipIdx : "");

            var result = new JobInfo.Result(suggName,  getDownloadURL(zipFile, suggName), MediaType.ZIP.toString(), zipFile.length()+"");
            sendJobUpdate(ji -> ji.addResult(result));

            failed.clear();
            denied.clear();
            zippedBytes = 0;

            // handle 'save to Workspace' option:  pushes zip file to workspace
            if (!isEmpty(wsDestPath)) {
                String wsFilePath = wsDestPath + suggestedName + (curZipIdx == 0 ? "" : "-" + (curZipIdx+1)) + ".zip";
                try {
                    new WsServerUtils().putFile(
                            new WsServerParams().set(CURRENTRELPATH, wsFilePath),
                            zipFile);
                } catch (IOException e) {
                    logger.error(e);
                }
            }

            FileUtil.silentClose(zout);
        }
    }

    private void newZipFile() throws FileNotFoundException {
        curZipIdx++;
        startFileInfoIdx = curFileInfoIdx;
        zipFile = getZipFile(getJob().getJobId(), suggestedName, curZipIdx);
        zout = new ZipOutputStream(new FileOutputStream(zipFile));
        zout.setMethod(ZipOutputStream.DEFLATED);
        zout.setLevel(ZipHandler.COMPRESSION_LEVEL);
    }

    private static String zipMessage(int totalFiles, long zippedBytes, List<String> failed, List<String> denied) {
        StringBuilder msg = new StringBuilder();
        int success = totalFiles - failed.size() - denied.size();
        msg.append(String.format("\nSuccessfully packaged " + success + " files: %,d B", zippedBytes));
        msg.append(README_SUCCESS_TEXT);
        if (denied.size() > 0) {
            msg.append("\nAccess was denied to ").append(denied.size()).append(" proprietary files: \n");
            msg.append(String.join("\n", denied));
        }
        if (failed.size() > 0) {
            msg.append("\nErrors were encountered when packaging ").append(failed.size())
                    .append((failed.size() > 1) ? " files" : " file").append(": \n");
            msg.append(String.join("\n", failed));
        }
        return msg.toString();
    }

    private static File getZipFile(String jobId, String suggestedName, int packageIdx) {
        File jobDir = getJobWorkDir(jobId);
        String hash = jobId.substring(jobId.length()-4);      // safe; id is always greater than 4 chars
        String fname = String.format("%s-%s%s.zip", suggestedName, hash, (packageIdx > 0 ? "_" + packageIdx : ""));
        return new File(jobDir, fname);
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
