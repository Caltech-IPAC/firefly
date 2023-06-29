/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.IpacTableReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static edu.caltech.ipac.firefly.server.query.IbeTemplateProcessor.PROC_ID;


/**
 * Creates table definition for IBE tables.  This extends SharedDbProcessor, meaning all of the tables will
 * be saved in one database.
 */
@SearchProcessorImpl(id = PROC_ID, params =
        {@ParamDoc(name = "url", desc = "the url for table definition")
        })
public class IbeTemplateProcessor extends SharedDbProcessor {
    public static final String PROC_ID = "IbeTemplate";

    public DataGroup fetchDataGroup(TableServerRequest treq) throws DataAccessException {
        try {
            String url = treq.getParam("url");
            ByteArrayOutputStream results = new ByteArrayOutputStream();
            HttpServices.getData(HttpServiceInput.createWithCredential(url), results);
            return IpacTableReader.read(new ByteArrayInputStream(results.toByteArray()));
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doLogging() {
        return false;
    }
}

