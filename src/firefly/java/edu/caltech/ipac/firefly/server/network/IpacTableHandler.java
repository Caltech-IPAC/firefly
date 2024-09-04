/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.IpacTableReader;
import org.apache.commons.httpclient.HttpMethod;
import edu.caltech.ipac.firefly.server.network.HttpServices.Status;

import static edu.caltech.ipac.firefly.server.network.HttpServices.getResponseBodyAsStream;

/**
 * Date: 7/7/22
 *
 * @author loi
 * @version : $
 */
public class IpacTableHandler implements HttpServices.Handler {
    public DataGroup results;

    public HttpServices.Status handleResponse(HttpMethod method) {
        if (HttpServices.isOk(method)) {
            try {
                results = IpacTableReader.read(getResponseBodyAsStream(method));
            } catch (Exception e) {
                return new Status(400, String.format("Error reading IPAC table: %s", e.getMessage()));
            }
        }
        return Status.getStatus(method);
    }

}
