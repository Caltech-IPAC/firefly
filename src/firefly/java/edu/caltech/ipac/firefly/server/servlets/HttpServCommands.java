/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.table.TableUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class HttpServCommands {

    static abstract class BaseServletCommand extends ServerCommandAccess.HttpCommand {
        // leaving a stuff here just in case it's useful.
    }


    public static class TableSave extends BaseServletCommand {
        static final Logger.LoggerImpl DL_LOGGER = Logger.getLogger(Logger.DOWNLOAD_LOGGER);

        private static Map<String, TableUtil.Format> allFormats = new HashMap<>();
        static {
            allFormats.put("ipac", TableUtil.Format.IPACTABLE);
            allFormats.put("csv", TableUtil.Format.CSV);
            allFormats.put("tsv", TableUtil.Format.TSV);
            allFormats.put("votable-tabledata", TableUtil.Format.VO_TABLE_TABLEDATA);
            allFormats.put("votable-binary-inline", TableUtil.Format.VO_TABLE_BINARY);
            allFormats.put("votable-binary2-inline", TableUtil.Format.VO_TABLE_BINARY2);
            allFormats.put("votable-fits-inline", TableUtil.Format.VO_TABLE_FITS);
            allFormats.put("fits", TableUtil.Format.FITS);
        }

        public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception {
            TableServerRequest request = sp.getTableServerRequest();
            if (request == null) throw new IllegalArgumentException("Invalid request");

            String fileName = sp.getOptional("file_name");
            final String fileFormat = sp.getOptional("file_format").toLowerCase();

            String formatInMap;
            if (StringUtils.isEmpty(fileFormat)) {
                formatInMap = "ipac";
            } else {
                Object[] formats = allFormats.keySet().stream()
                        .filter((t) -> fileFormat.equals(t))
                        .toArray();
                formatInMap = (formats.length != 1) ? "ipac" : (String)formats[0];
            }

            TableUtil.Format tblFormat = allFormats.get(formatInMap);
            String fileNameExt = tblFormat.getFileNameExt();

            if (StringUtils.isEmpty(fileName)) {
                fileName = request.getRequestId();
            }
            if (fileName.toLowerCase().endsWith(fileNameExt)) {
                fileNameExt = "";
            }

            res.setHeader("Content-Disposition", "attachment; filename=" + fileName + fileNameExt);
            SearchManager am = new SearchManager();

            FileInfo fi = am.save(res.getOutputStream(), request, tblFormat);
            if (fi != null) {
                long length = 0;
                if (fi != null) {
                    length = fi.getSizeInBytes();
                }
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

