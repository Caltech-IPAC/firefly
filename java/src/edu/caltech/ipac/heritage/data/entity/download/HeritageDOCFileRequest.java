package edu.caltech.ipac.heritage.data.entity.download;

import edu.caltech.ipac.firefly.data.ServerRequest;

import java.io.Serializable;


/**
 * @author trey
 *         $Id: HeritageDOCFileRequest.java,v 1.1 2010/05/06 00:57:01 roby Exp $
 */
public class HeritageDOCFileRequest extends ServerRequest implements Serializable {
    private static final String REQKEY = "REQKEY";

    public HeritageDOCFileRequest() {}

    public HeritageDOCFileRequest(String reqkey) {
        super("heritageDOCFileRequest");
        setParam(REQKEY, reqkey);
    }

    public String getReqkey() { return getParam(REQKEY); }
}