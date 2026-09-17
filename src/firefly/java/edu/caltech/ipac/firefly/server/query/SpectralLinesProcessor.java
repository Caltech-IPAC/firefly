/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.obscorepackager.ObsCoreUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.serialization.Serializer;

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
            "SPHEREx line list", "/edu/caltech/ipac/firefly/resources/spherex_lines.tbl",
            "Spitzer PAHFIT line list",  "/edu/caltech/ipac/firefly/resources/pahfit_lines.csv",
            "Herschel HSPOT line list",  "/edu/caltech/ipac/firefly/resources/hspot_lines.csv"
            );
    private static final String WAVELENGTH_COL = "wavelength"; // must match SpectralLines.jsx's WAVELENGTH_COL
    private static final String LINE_LISTS_PROP = "charts.spectrum.linelists";

    public record LineListInfo(String listId, String listLabel, String src) {}

    public static final List<LineListInfo> LINE_LISTS = parseLineListsConfig();

    private static LineListInfo toLineListInfo(String label, String src) {
        String id = ObsCoreUtil.makeValidString(label).replace(".", "-");
        return new LineListInfo(id, label, src);
    }

    private static List<LineListInfo> parseLineListsConfig() {
        List<LineListInfo> lists = new ArrayList<>();
        try {
            String lineListsJson = AppProperties.getProperty(LINE_LISTS_PROP);
            if (StringUtils.isEmpty(lineListsJson)) {
                // default to every bundled line list when not defined or blank (different from explicit "[]")
                BUNDLED_RESOURCES.forEach((label, src) -> lists.add(toLineListInfo(label, src)));
                return lists;
            }
            var entries = Serializer.fromJson(lineListsJson, Map[].class);
            for (Map entry : entries) {
                String label = (String) entry.get("label");
                String src = (String) entry.get("src");
                if (src == null) src = BUNDLED_RESOURCES.get(label);
                if (src == null) {
                    LOGGER.error("%s: no bundled resource for label \"%s\" - dropping from spectral lines list".formatted(LINE_LISTS_PROP, label));
                    continue;
                }
                lists.add(toLineListInfo(label, src));
            }
        } catch (Exception e) {
            LOGGER.error(e, "%s: failed to parse config - no spectral line lists will be available".formatted(LINE_LISTS_PROP));
        }
        return lists;
    }

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        if (req.getBooleanParam("metaOnly")) return lineListsMetaDataGroup();

        String listId = req.getParam("listId");
        LineListInfo info = LINE_LISTS.stream().filter(l -> l.listId().equals(listId)).findFirst()
                .orElseThrow(() -> new DataAccessException("Unknown or missing spectral lines listId: %s".formatted(listId)));

        boolean isUrl = info.src().toLowerCase().startsWith("http");
        try {
            File tempFile = createTempFile(req, isUrl ? null : info.src().substring(info.src().lastIndexOf('.')));
            if (isUrl) {
                FileInfo fi = URLDownload.getDataToFile(new URI(info.src()).toURL(), tempFile);
                if (!fi.isOK()) throw new DataAccessException(fi.getResponseCodeMsg());
            } else {
                try (InputStream is = SpectralLinesProcessor.class.getResourceAsStream(info.src())) {
                    if (is == null) throw new IOException("Resource not found: %s".formatted(info.src()));
                    FileUtil.writeToFile(is, tempFile, null);
                }
            }
            DataGroup dg = TableUtil.readAnyFormat(tempFile, 0, req);
            DataType wlCol = dg.getDataDefintion(WAVELENGTH_COL);
            if (wlCol == null) {
                LOGGER.warn("Spectral line list \"%s\" from %s: \"%s\" column is missing - no lines will be loaded from this list."
                        .formatted(info.listLabel(), info.src(), WAVELENGTH_COL));
            } else if (StringUtils.isEmpty(wlCol.getUnits())) {
                LOGGER.warn("Spectral line list \"%s\" from %s: \"%s\" column has no units metadata - client will assume microns."
                        .formatted(info.listLabel(), info.src(), WAVELENGTH_COL));
            }
            return dg;
        } catch (Exception e) {
            String msg = "Unable to load spectral line list \"%s\" from %s".formatted(info.listLabel(), info.src());
            if (e instanceof FailedRequestException fre && fre.getResponseCode() > -1) {
                msg = "%s (response code: %d)".formatted(msg, fre.getResponseCode());
            }
            LOGGER.error(e, msg);
            throw new DataAccessException("Unable to read spectral lines resource for %s".formatted(info.listLabel()), e);
        }
    }

    private static DataGroup lineListsMetaDataGroup() {
        DataGroup dg = new DataGroup("Spectral Line Lists", new DataType[0]);
        // src deliberately excluded - it's admin-configured and may be an internal/credentialed URL, not for client eyes
        List<Map<String,String>> lineLists = LINE_LISTS.stream()
                .map(info -> Map.of("listId", info.listId(), "listLabel", info.listLabel()))
                .toList();
        String lineListsJson = Serializer.toJsonString(lineLists);
        dg.getTableMeta().setAttribute("lineLists", lineListsJson);
        return dg;
    }
}
