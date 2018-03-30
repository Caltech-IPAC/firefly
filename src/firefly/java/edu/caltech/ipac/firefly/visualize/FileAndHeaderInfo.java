/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

/**
 * User: roby
 * Date: Mar 1, 2010
 * Time: 3:07:35 PM
 */


/**
 * @author Trey Roby
 */
public class FileAndHeaderInfo {

    private final static String SPLIT_TOKEN= "--FileAndHeaderInfo --";
    private String          _file;
    private ClientFitsHeader _header;

    /**
     * contains the file and the ClientFitsHeader
     * @param file the file to look at on the server
     * @param header the ClientFitsHeader
     */
    FileAndHeaderInfo(String file, ClientFitsHeader header) {
        _file= file;
        _header= header;
    }

    public String getfileName() { return _file; }
    public ClientFitsHeader getHeader() { return _header; }

    public String toString() {
        return _file+SPLIT_TOKEN+_header.toString();
    }
}

