/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.alert;

import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.JsonStringProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.LockingRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static edu.caltech.ipac.firefly.core.FileAnalysisReport.ReportType.Details;
import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.ErrorResponse;

@SearchProcessorImpl(id = "AlertViewerSearchProcessor", params = {
        @ParamDoc(name = "source", desc = "An alert ID.")
})
public class AlertViewerSearchProcessor extends JsonStringProcessor {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public static final String ID = "AlertViewerSearchProcessor";
    public static final String SOURCE = "source";
    private static final String ANALYZER_ID = "alert";
    private static final String ALERT_ID = "ID";
    public static final String RESPONSEFORMAT = "RESPONSEFORMAT";
    private static final String FITS_RESPONSE_FORMAT = "fits";
    private static final String ALERT_SERVICE_BASE_URL_PROP = "alert.viewer.fits.service.url";

    @Override
    public boolean doCache() {
        return true;
    }

    @Override
    public String fetchData(ServerRequest req) throws DataAccessException {
        final String source = req.getParam(SOURCE) == null ? null : req.getParam(SOURCE).trim();

        if (StringUtils.isEmpty(source)) {
            return makeError(source, "An alert ID is required.");
        }

        if (source.contains("://")) {
            return makeError(source, "Please enter an alert ID, URLs are not supported.");
        }

        try {
            final String serviceUrl = makeAlertServiceUrl(source);
            FileInfo fileInfo = LockingRetrieve.downloadWithCacheMsg(serviceUrl, ServerContext.getUploadDir());
            if (fileInfo == null) {
                return makeError(source, "Unable to retrieve the FITS file for alert ID: " + source);
            }
            final int responseCode = fileInfo.getResponseCode();
            if (responseCode > 305 || responseCode < 200) {
                String responseMsg = StringUtils.isEmpty(fileInfo.getResponseCodeMsg())
                        ? "Unknown error"
                        : fileInfo.getResponseCodeMsg();

                String message = switch (responseCode) {
                    case 400 -> "400 Bad Request: unknown query parameter, or malformed alert ID";
                    case 401 -> "401 Unauthorized: missing or invalid token";
                    case 404 -> "404 Not Found: no alert exists for the given ID";
                    case 415 -> "415 Unsupported Media Type: unrecognised RESPONSEFORMAT value";
                    case 500 -> "500 Internal Server Error: alert service internal error.";
                    default -> "Unable to retrieve the FITS file: " + responseCode + " " + responseMsg;
                };

                return makeError(source, message);
            }


            FileAnalysisReport report = FileAnalysis.analyze(fileInfo, Details, ANALYZER_ID, Collections.emptyMap());
            return makeSuccess(source, fileInfo, report);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load alert data: " + e.getMessage(), e);
        }
    }

    @Override
    public String getUniqueID(ServerRequest request) {
        return ID + ":" + request.getParam(SOURCE, "");
    }

    private static String makeSuccess(String source, FileInfo fileInfo, FileAnalysisReport report) {
        JSONObject out = baseResponse(source, true, null);
        String fileKey = null;
        String fileName = null;

        try {
            if (fileInfo != null && fileInfo.getFile() != null) {
                fileKey = ServerContext.replaceWithPrefix(fileInfo.getFile());
                fileName = fileInfo.getExternalName();
                out.put("fileKey", fileKey);
                out.put("fileName", fileName);
            }

            if (report == null) {
                out.put("success", false);
                out.put("message", "Could not analyze the FITS file.");
                return out.toJSONString();
            }

            if (!StringUtils.isEmpty(report.getFileName())) {
                fileName = report.getFileName();
                out.put("fileName", fileName);
            }

            List<FileAnalysisReport.Part> parts = report.getParts();
            if (parts == null || parts.isEmpty()) {
                out.put("success", false);
                out.put("message", "Could not analyze the FITS file.");
                return out.toJSONString();
            }

            if (parts.size() == 1 && parts.get(0).getType() == ErrorResponse) {
                out.put("success", false);
                out.put("message", parts.get(0).getDesc());
                return out.toJSONString();
            }

            JSONArray entries = new JSONArray();
            for (int i = 0; i < Math.min(parts.size(), 5); i++) {
                FileAnalysisReport.Part part = parts.get(i);
                entries.add(makeEntry(part, i, fileKey, fileName));
            }

            out.put("entries", entries);
            if (entries.size() != 5) {
                out.put("success", false);
                out.put("message", "Expected 5 alert viewer entries but found " + entries.size() + ".");
                Logger.error("Expected 5 alert viewer entries but found " + entries.size() + ".");
            }
        }
        catch (Exception e) {
            Logger.error("Error processing the FITS file: " + e.getMessage());
        }
        return out.toJSONString();
    }

    private static JSONObject makeEntry(FileAnalysisReport.Part part, int fallbackIdx, String fileKey, String fileName) {
        JSONObject entry = new JSONObject();
        entry.put("type", part.getType().name());
        entry.put("desc", part.getDesc());
        entry.put("extNum", getExtNum(part, fallbackIdx));
        //have each entry take a fileKey and fileName in case the viewer needs to retrieve the file for that specific entry (e.g. if it's an image or a table that can be retrieved as a separate file)
        if (!StringUtils.isEmpty(fileKey)) entry.put("fileKey", fileKey);
        if (!StringUtils.isEmpty(fileName)) entry.put("fileName", fileName);
        return entry;
    }

    private static int getExtNum(FileAnalysisReport.Part part, int fallbackIdx) {
        return part.getFileLocationIndex() > -1 ? part.getFileLocationIndex() :
                part.getIndex() > -1 ? part.getIndex() : fallbackIdx;
    }

    private static String makeError(String source, String message) {
        return baseResponse(source, false, message).toJSONString();
    }

    private static String makeAlertServiceUrl(String alertId) throws DataAccessException {
        final String baseUrl = AppProperties.getProperty(ALERT_SERVICE_BASE_URL_PROP,
                "https://data-int.lsst.cloud/api/alerts");
        if (StringUtils.isEmpty(baseUrl)) {
            throw new DataAccessException("Missing required property: " + ALERT_SERVICE_BASE_URL_PROP);
        }
        final String joiner = baseUrl.contains("?") ? "&" : "?";
        return String.format("%s%s%s=%s&%s=%s",
                baseUrl, joiner, ALERT_ID, URLEncoder.encode(alertId, StandardCharsets.UTF_8),
                RESPONSEFORMAT, FITS_RESPONSE_FORMAT);
    }

    private static JSONObject baseResponse(String source, boolean success, String message) {
        JSONObject out = new JSONObject();
        out.put("success", success);
        out.put("source", source);
        if (!StringUtils.isEmpty(message)) {
            out.put("message", message);
        }
        return out;
    }
}
