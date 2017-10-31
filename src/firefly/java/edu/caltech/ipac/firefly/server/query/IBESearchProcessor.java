/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class IBESearchProcessor extends DynQueryProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    abstract protected String getDDUrl(ServerRequest request);

    protected String parseMessageFromIBE(String response) {
        String msg = "";
        String BODY_BEGIN = "<body>";
        String BODY_END = "</body>";
        int a = response.indexOf(BODY_BEGIN);
        if (a > 0) {
            int b = response.indexOf(BODY_END);
            msg = response.substring(a + BODY_BEGIN.length(), b).toString();

            String HEADER_END = "</h1>";
            int c = msg.indexOf(HEADER_END);
            if (c > 0) {
                msg = msg.substring(c + HEADER_END.length());
            }

            // cleanup
            msg = msg.replaceAll("<br ?/>", " ").replaceAll("\n", " ").trim();

        } else {
            return response;
        }

        return msg;
    }

    protected String makeBaseSearchURL(String host, String schemaGroup, String schema, String table) {
        String urlString = QueryUtil.makeUrlBase(host) + "/search/" + schemaGroup + "/" + schema + "/" + table;
        return urlString;
    }

    protected String makeDDURL(String host, String schemaGroup, String schema, String table) {
        String urlString = QueryUtil.makeUrlBase(host) + "/search/" + schemaGroup + "/" + schema + "/" + table + "?FORMAT=METADATA";
        return urlString;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        setXmlParams(request);
        String url = getDDUrl(request);
        if (url != null) {
            try {
                TableServerRequest treg = new TableServerRequest(IbeTemplateProcessor.PROC_ID);
                treg.setParam("url", url);
                DataGroup template = new SearchManager().getDataGroup(treg).getData();
                for (DataObject row : template) {
                    String col = String.valueOf(row.getDataElement("name"));
                    if (exists(columns, col)) {
                        String desc = String.valueOf(row.getDataElement("description"));
                        meta.setAttribute(DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, col), desc);
                    }
                }
            } catch (DataAccessException e) {
                // cannot find template..  this is not normal
                LOGGER.error(e);
            }
        }
    }

    private boolean exists(List<DataType> cols, String name) {
        for (DataType dt : cols) {
            if (dt.getKeyName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    protected void requiredPostFileCacheParam(MultiPartPostBuilder builder, String name, String cacheID) throws EndUserException {
        boolean badParam = true;
        if (!StringUtils.isEmpty(cacheID)) {
            File uploadFile = ServerContext.convertToFile(cacheID);
            if (uploadFile.canRead()) {
                requiredPostParam(builder, name, uploadFile);
                badParam = false;
            }
        }

        if (badParam) {
            throw new EndUserException("Validation Error",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected void requiredPostParam(MultiPartPostBuilder builder, String name, File f) throws EndUserException {
        if (f.canRead()) {
            builder.addFile(name, f);

        } else {
            throw new EndUserException("WISE search failed, WISE Catalog is unavailable",
                    "Search Processor could not read file: " + f.getPath());
        }
    }

    protected void requiredPostParam(MultiPartPostBuilder builder, String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            builder.addParam(name, value);

        } else {
            throw new EndUserException("WISE search failed, WISE Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected void requiredPostParam(MultiPartPostBuilder builder, String name) throws EndUserException {
        builder.addParam(name, "");
    }

    protected void optionalPostParam(MultiPartPostBuilder builder, String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            builder.addParam(name, value);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            requiredParam(sb, name, value + "");

        } else {
            throw new EndUserException("WISE search failed, WISE Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));

        } else {
            throw new EndUserException("WISE search failed, WISE Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void optionalParam(StringBuffer sb, String name) {
        sb.append(param(name));
    }

    protected static void optionalParam(StringBuffer sb, String name, String value) {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));
        }
    }

    protected static void optionalParam(StringBuffer sb, String name, boolean value) {
        sb.append(param(name, value));
    }


    protected static String param(String name) {
        return "&" + name;
    }

    protected static String param(String name, String value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, int value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, double value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, boolean value) {
        return "&" + name + "=" + (value ? "1" : "0");
    }

    protected static IOException makeException(Exception e, String reason) {
        IOException eio = new IOException(reason);
        eio.initCause(e);
        return eio;
    }

}

