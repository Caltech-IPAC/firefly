/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 12/4/14
 * Time: 3:55 PM
 */


/**
 * @author Trey Roby
 */
public class FileRetrieveException extends FailedRequestException {

    private final String retrieveServiceID;
    public FileRetrieveException(String err, String detailErr, String retrieveServiceID) {
        super(err, detailErr, true, null);
        this.retrieveServiceID= retrieveServiceID;
    }

    public FileRetrieveException(String err, String detailErr, Exception e, String retrieveServiceID) {
        super(err, detailErr, true, e);
        this.retrieveServiceID= retrieveServiceID;
    }

    public String getRetrieveServiceID() {
        return retrieveServiceID;
    }
}

