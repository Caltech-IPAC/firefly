/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Serves one of the recommended spectral line lists (fixed resource datasets) as a table,
 * selected by the request's "listId" param. When "metaOnly" is true, returns an empty table
 * whose tableMeta.lineLists carries the available {listId, listLabel} pairs as a JSON array,
 * so the client can discover what's available.
 */
@SearchProcessorImpl(id = "spectralLines")
public class SpectralLinesProcessor extends EmbeddedDbProcessor {

    public record LineListInfo(String listId, String listLabel, String resource) {}

    public static final List<LineListInfo> LINE_LISTS = List.of(
            new LineListInfo("luisa", "Luisa", "/edu/caltech/ipac/firefly/resources/luisa_linelist.csv"),
            new LineListInfo("jwst",  "JWST",  "/edu/caltech/ipac/firefly/resources/jwst_linelist.tbl")
    );

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        if (req.getBooleanParam("metaOnly")) return lineListsMetaDataGroup();

        String listId = req.getParam("listId");
        LineListInfo info = LINE_LISTS.stream().filter(l -> l.listId().equals(listId)).findFirst()
                .orElseThrow(() -> new DataAccessException("Unknown or missing spectral lines listId: " + listId));

        try (InputStream is = SpectralLinesProcessor.class.getResourceAsStream(info.resource())) {
            if (is == null) throw new IOException("Resource not found: " + info.resource());

            // readAnyFormat needs a File; copy the classpath resource to a temp file first
            String ext = info.resource().substring(info.resource().lastIndexOf('.'));
            File tempFile = createTempFile(req, ext);
            FileUtil.writeToFile(is, tempFile, null);

            return TableUtil.readAnyFormat(tempFile, 0, req);
        } catch (IOException e) {
            throw new DataAccessException("Unable to read spectral lines resource", e);
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
