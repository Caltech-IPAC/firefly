/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
/**
 * User: roby
 * Date: Mar 1, 2010
 * Time: 3:07:35 PM
 */


/**
 * @author Trey Roby
 */
public class FileAndHeaderInfo implements Serializable {

    private final static String SPLIT_TOKEN= "--FileAndHeaderInfo --";
    private String          _file;
    private String _headerSerialized;

    private FileAndHeaderInfo() {}

    /**
     * contains the file and the ClientFitsHeader
     * @param file the file to look at on the server
     * @param headerSerialized a string the contains the results of passing ClientFitsHeader
     * to ClientFitsHeader.toString()
     */
    FileAndHeaderInfo(String file, String headerSerialized) {
        _file= file;
        _headerSerialized = headerSerialized;
    }

    public String getfileName() { return _file; }
    public ClientFitsHeader getHeader() { return ClientFitsHeader.parse(_headerSerialized); }

    public String toString() {
        return _file+SPLIT_TOKEN+_headerSerialized;
    }


    public static FileAndHeaderInfo parse(String s) {

        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,3);
        FileAndHeaderInfo retval= null;
        if (sAry.length==2) {
            retval= new FileAndHeaderInfo(StringUtils.checkNull(sAry[0]),sAry[1]);
        }
        return retval;
    }
}

