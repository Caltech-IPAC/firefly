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

/**
 * Serves the recommended spectral line list (a fixed resource dataset) as a table.
 */
@SearchProcessorImpl(id = "spectralLines")
public class SpectralLinesProcessor extends EmbeddedDbProcessor {
    private static final String RESOURCE = "/edu/caltech/ipac/firefly/resources/linelist_combined.csv";

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        try (InputStream is = SpectralLinesProcessor.class.getResourceAsStream(RESOURCE)) {
            if (is == null) throw new IOException("Resource not found: " + RESOURCE);

            // readAnyFormat needs a File; copy the classpath resource to a temp file first
            String ext = RESOURCE.substring(RESOURCE.lastIndexOf('.'));
            File tempFile = createTempFile(req, ext);
            FileUtil.writeToFile(is, tempFile, null);

            return TableUtil.readAnyFormat(tempFile, 0, req);
        } catch (IOException e) {
            throw new DataAccessException("Unable to read spectral lines resource", e);
        }
    }
}
