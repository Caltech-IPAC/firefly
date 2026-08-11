/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Serves one of the recommended spectral line lists (fixed resource datasets) as a table,
 * selected by the request's "listId" param.
 */
@SearchProcessorImpl(id = "spectralLines")
public class SpectralLinesProcessor extends EmbeddedDbProcessor {
    private static final Map<String, String> RESOURCES = Map.of(
            "luisa", "/edu/caltech/ipac/firefly/resources/luisa_linelist.csv",
            "jwst",  "/edu/caltech/ipac/firefly/resources/jwst_linelist.tbl"
    );

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        String listId = req.getParam("listId");
        String resource = RESOURCES.get(listId);
        if (resource == null) throw new DataAccessException("Unknown or missing spectral lines listId: " + listId);

        try (InputStream is = SpectralLinesProcessor.class.getResourceAsStream(resource)) {
            if (is == null) throw new IOException("Resource not found: " + resource);

            // readAnyFormat needs a File; copy the classpath resource to a temp file first
            String ext = resource.substring(resource.lastIndexOf('.'));
            File tempFile = createTempFile(req, ext);
            FileUtil.writeToFile(is, tempFile, null);

            return TableUtil.readAnyFormat(tempFile, 0, req);
        } catch (IOException e) {
            throw new DataAccessException("Unable to read spectral lines resource", e);
        }
    }
}
