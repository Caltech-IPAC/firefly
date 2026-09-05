/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.obscorepackager.ObsCoreUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serves one of the recommended spectral line lists as a table, selected by the request's "listId" param.
 * When "metaOnly" is true, returns an empty table whose tableMeta.lineLists carries the available
 * {listId, listLabel} pairs as a JSON array, so the client can discover what's available.
 * <p>
 * The active set and ordering is driven by the "charts.spectrum.linelists" app config property, a JSON
 * array of {label, src?} - src omitted falls back to BUNDLED_RESOURCES. The resolved src is fetched as a
 * URL if it starts with http/https, otherwise as a classpath resource - so BUNDLED_RESOURCES can be a URL too.
 */
@SearchProcessorImpl(id = "spectralLines")
public class SpectralLinesProcessor extends EmbeddedDbProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final Map<String, String> BUNDLED_RESOURCES = Map.of(
            "Luisa", "/edu/caltech/ipac/firefly/resources/luisa_linelist.csv",
            "JWST",  "/edu/caltech/ipac/firefly/resources/jwst_linelist.tbl"
    );

    public record LineListInfo(String listId, String listLabel, String src) {}

    public static final List<LineListInfo> LINE_LISTS = parseLineListsConfig();

    private static List<LineListInfo> parseLineListsConfig() {
        List<LineListInfo> lists = new ArrayList<>();
        try {
            JSONArray entries = (JSONArray) new JSONParser().parse(AppProperties.getProperty("charts.spectrum.linelists", "[]"));
            for (Object o : entries) {
                JSONObject entry = (JSONObject) o;
                String label = (String) entry.get("label");
                String src = (String) entry.get("src");
                if (src == null) src = BUNDLED_RESOURCES.get(label);
                if (src == null) {
                    LOGGER.error("charts.spectrum.linelists: no bundled resource for label \"" + label + "\" - dropping from spectral lines list");
                    continue;
                }
                String id = ObsCoreUtil.makeValidString(label).replace(".", "-");
                lists.add(new LineListInfo(id, label, src));
            }
        } catch (Exception e) {
            LOGGER.error(e, "charts.spectrum.linelists: failed to parse config - no spectral line lists will be available");
        }
        return lists;
    }

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        if (req.getBooleanParam("metaOnly")) return lineListsMetaDataGroup();

        String listId = req.getParam("listId");
        LineListInfo info = LINE_LISTS.stream().filter(l -> l.listId().equals(listId)).findFirst()
                .orElseThrow(() -> new DataAccessException("Unknown or missing spectral lines listId: " + listId));

        boolean isUrl = info.src().toLowerCase().startsWith("http");
        try {
            File tempFile = createTempFile(req, isUrl ? null : info.src().substring(info.src().lastIndexOf('.')));
            if (isUrl) {
                URLDownload.getDataToFile(new URI(info.src()).toURL(), tempFile);
            } else {
                try (InputStream is = SpectralLinesProcessor.class.getResourceAsStream(info.src())) {
                    if (is == null) throw new IOException("Resource not found: " + info.src());
                    FileUtil.writeToFile(is, tempFile, null);
                }
            }
            return TableUtil.readAnyFormat(tempFile, 0, req);
        } catch (Exception e) {
            LOGGER.error(e, "Unable to load spectral line list \"" + info.listLabel() + "\" from " + info.src());
            throw new DataAccessException("Unable to read spectral lines resource for " + info.listLabel(), e);
        }
    }

    private static DataGroup lineListsMetaDataGroup() {
        DataGroup dg = new DataGroup("Spectral Line Lists", new DataType[0]);
        JSONArray lists = new JSONArray();
        LINE_LISTS.forEach(info -> {
            JSONObject o = new JSONObject();
            o.put("listId", info.listId());
            o.put("listLabel", info.listLabel());
            lists.add(o);
        });
        dg.getTableMeta().setAttribute("lineLists", lists.toJSONString());
        return dg;
    }
}
