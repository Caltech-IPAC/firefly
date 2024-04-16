/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static edu.caltech.ipac.table.TableUtil.Format.VO_TABLE;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * Utility functions for DAL services; https://www.ivoa.net/documents/DALI/
 *
 * @author loi
 */
public class DaliUtil {
    public static final String UPLOAD = "UPLOAD";
    public static final String UPLOAD_DESC = "external resources (typically files)";
    public static final String MAXREC = "MAXREC";
    public static final String MAXREC_DESC = "maximum number of records to be returned";

    // less common, but should follow if used
    public static final String REQUEST = "REQUEST";
    public static final String REQUEST_DESC = "for service capabilities that have multiple modes or operations";
    public static final String RUNID = "RUNID";
    public static final String RUNID_DESC = "tag requests with the identifier of a larger job";
    public static final String RESPONSEFORMAT = "RESPONSEFORMAT";
    public static final String RESPONSEFORMAT_DESC = "sync=content-type of the response; async=content-type of the result resource(s)";

    // constant used by Firefly
    public static final String UPLOAD_COLUMNS = "UPLOAD_COLUMNS";
    public static final String UPLOAD_COLUMNS_DESC = "Use to limit the upload to only these columns";

    private static final int MAXREC_HARD = AppProperties.getIntProperty("tap.maxrec.hardlimit", 10_000_000);
    private static final Pattern uriPattern = Pattern.compile("^(http:|https:|vos:).*", Pattern.CASE_INSENSITIVE);

    /**
     * Populate the input with the known parameters from the given request.
     * @param input
     * @param request
     */
    public static void populateKnownInputs(HttpServiceInput input, ServerRequest request) throws DataAccessException {
        applyIfNotEmpty(request.getParam(REQUEST), (v) -> input.setParam(REQUEST, v));
        applyIfNotEmpty(request.getParam(RUNID), (v) -> input.setParam(RUNID, v));
        applyIfNotEmpty(request.getParam(RESPONSEFORMAT), (v) -> input.setParam(RESPONSEFORMAT, v));
        handleMaxrec(input, request);
        handleUpload(input, request, null);
    }

    public static void handleMaxrec(HttpServiceInput input, ServerRequest request) {

        applyIfNotEmpty(request.getParam(MAXREC), (v) -> {
            int maxrec = StringUtils.getInt(v, -1);
            if (maxrec < 0 || maxrec > MAXREC_HARD) {
                throw new IllegalArgumentException(String.format("MAXREC value %d is not in (0,%d) range.", maxrec, MAXREC_HARD));
            } else {
                input.setParam(MAXREC, v);
            }
        });
    }

    public static void handleUpload(HttpServiceInput input, ServerRequest request, String namedAs) throws DataAccessException {

        String upload = request.getParam(UPLOAD);           // impl for single upload; although multi upload is supported by the DALI
        if (isEmpty(upload)) return;

        namedAs = isEmpty(namedAs) ? "table1" : namedAs;

        if ( uriPattern.matcher(upload).matches()) {             // upload a resource from URI
            input.setParam(UPLOAD, namedAs + "," + upload);
        } else {
            // upload a resourse inline
            String uploadCols = request.getParam(UPLOAD_COLUMNS);

            File ufile = ServerContext.convertToFile(upload);
            try {
                input.setParam(UPLOAD, namedAs + ",param:ufile1");
                DataGroup dg= TableUtil.readAnyFormat(ufile);
                if (!isEmpty(uploadCols)) {
                    List<String> cols = Arrays.stream(TableUtil.splitCols(uploadCols))      // use this to allow commas in double-quotes
                            .map(cname -> cname.trim().replaceAll("^\"|\"$", ""))  // remove double-quotes if exists
                            .toList();

                    Arrays.asList(dg.getDataDefinitions()).forEach(c -> {
                        if (!cols.contains(c.getKeyName())) dg.removeDataDefinition(c.getKeyName());
                    });
                }
                File ufile1 = File.createTempFile("dali-upload",".xml", ServerContext.getTempWorkDir());
                VoTableWriter.save(ufile1,dg, VO_TABLE);
                input.setFile("ufile1",ufile1);
            } catch (IOException e) {
                throw new DataAccessException.FileNotFound("Unable to read uploaded file", ufile);
            }
        }
    }

    public static DataAccessException createDax(String title, String url) {
        return createDax(title, url, null);
    }

    public static DataAccessException createDax(String title, String url, Exception cause) {
        String msg = String.format("%s from the URL: [%s]", title, url);
        return new DataAccessException(msg, cause);
    }

    public static String parseError(HttpMethod method, String errorUrl) {

        String errMsg;
        if (HttpServices.isOk(method)) {
            String contentType = HttpServices.getResHeader(method, "Content-Type", "");
            boolean isText = contentType.startsWith("text/plain");
            try {
                if (isText) {
                    // error is text doc
                    errMsg = HttpServices.getResponseBodyAsString(method);
                } else {
                    // error is VOTable doc
                    InputStream is = HttpServices.getResponseBodyAsStream(method);
                    try {
                        String voError = VoTableReader.getError(is, errorUrl);
                        if (voError == null) {
                            voError = "Non-compliant error doc " + errorUrl;
                        }
                        errMsg = voError;
                    } finally {
                        FileUtil.silentClose(is);
                    }
                }
            } catch (Exception e) {
                errMsg = String.format("Error retrieving error document from %s: %s", errorUrl, e.getMessage());
            }
        } else {
            errMsg = String.format("Error retrieving error document from %s: %s", errorUrl, method.getStatusText());
        }
        return errMsg;
    }

}


