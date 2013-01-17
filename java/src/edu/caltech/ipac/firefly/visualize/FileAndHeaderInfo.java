package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.MiniFitsHeader;

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
     * contains the file and the MiniFitsHeader
     * @param file the file to look at on the server
     * @param headerSerialized a string the contains the results of passing MiniFitsHeader
     * to MiniFitsHeader.toString()
     */
    FileAndHeaderInfo(String file, String headerSerialized) {
        _file= file;
        _headerSerialized = headerSerialized;
    }

    public String getfileName() { return _file; }
    public MiniFitsHeader getHeader() { return MiniFitsHeader.parse(_headerSerialized); }

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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
