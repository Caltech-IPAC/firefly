/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.table.TableUtil;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

public class HttpServCommands {

    static abstract class BaseServletCommand extends ServerCommandAccess.HttpCommand {
        // leaving a stuff here just in case it's useful.
    }


    public static class TableSave extends BaseServletCommand {
        static final Logger.LoggerImpl DL_LOGGER = Logger.getLogger(Logger.DOWNLOAD_LOGGER);

        public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception {
            TableServerRequest request = sp.getTableServerRequest();
            if (request == null) throw new IllegalArgumentException("Invalid request");

            String fileName = sp.getOptional("file_name");
            TableUtil.Mode mode = sp.contains("mode") ? TableUtil.Mode.valueOf(sp.getOptional("mode")) : TableUtil.Mode.displayed;

            boolean tableSave = sp.getOptionalBoolean("save_to_temp", false); //if this is true, don't want to download tbl, instead load it into a tmp file

            TableUtil.Format tblFormat = sp.getTableFormat();
            String fileNameExt = tblFormat.getFileNameExt();

            if (fileNameExt.equalsIgnoreCase(".reg")) {
                String cols = sp.getOptional("center_cols");
                request.setParam("center_cols", sp.getOptional("center_cols"));
            }

            if (isEmpty(fileName)) {
                fileName = request.getRequestId();
            }
            if (fileName.toLowerCase().endsWith(fileNameExt)) {
                fileNameExt = "";
            }

            SearchManager am = new SearchManager();

            if (tableSave) {
                JSONObject json = new JSONObject();
                try {
                    File myTmpFile = File.createTempFile("TableSave-", ".tbl", QueryUtil.getTempDir(request));
                    String replacedPrefix = ServerContext.replaceWithPrefix(myTmpFile);
                    FileOutputStream out= new FileOutputStream(myTmpFile);
                    am.save(out, request, tblFormat, mode);
                    json.put("success", true);
                    json.put("serverFile", replacedPrefix);
                    out.close();

                }
                catch (Exception e) {
                    json.put("success", false);
                    json.put("error",  e.getMessage());
                }
                String rVal = json.toJSONString();
                res.setContentType("application/json");
                res.setContentLength(rVal.length());
                res.getOutputStream().print(rVal);
                return;
            }

            res.setHeader("Content-Disposition", "attachment; filename=" + fileName + fileNameExt);
            FileInfo fi = am.save(res.getOutputStream(), request, tblFormat, mode);
            if (fi != null) {
                long length = fi.getSizeInBytes(); // if written from the db, the length is 0
                logStats(length, "fileName", fi.getExternalName());
                // maintain counters for applicaiton monitoring
                Counters.getInstance().increment(Counters.Category.Download, "SaveAsIpacTable");
                Counters.getInstance().incrementKB(Counters.Category.Download, "SaveAsIpacTable (KB)", length/1024);
            }
        }

        void logStats(long fileSize, Object... params) {
            DL_LOGGER.stats("SaveAsIpacTable", "fsize(MB)", (double) fileSize / StringUtils.MEG,
                    "params", CollectionUtil.toString(params, ","));
        }
    }


    public static class Upload extends BaseServletCommand {

        public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception {
            AnyFileUpload.doFileUpload(req, res);
        }

    }

}

