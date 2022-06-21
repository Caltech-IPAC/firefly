/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.IpacTableReader;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;

/**
 * Date: 7/7/22
 *
 * @author loi
 * @version : $
 */
public class DataGroupHandler implements HttpServices.Handler {
    public DataGroup results;
    public Exception exception;
    private TableServerRequest treq;

    public DataGroupHandler(TableServerRequest treq) {
        this.treq = treq;
    }

    public void handleResponse(HttpMethod method) {
        if (HttpServices.isOk(method)) {
            try {
                results = IpacTableReader.read(method.getResponseBodyAsStream());
            } catch (IOException e) {
                exception = new DataAccessException(e.getMessage(), e);
            }
        } else {
            exception = new IOException("Request failed with status=" + method.getStatusCode());
        }
    }

}
