/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.hcompress;

/**
 * TODO Write Javadocs
 *
 * @version $Id: HDecompressException.java,v 1.2 2005/12/01 00:56:21 roby Exp $
 *          <hr>
 *          Represents the data contained in an Observation Request. <BR>
 *          Copyright (C) 1999-2005 California Institute of Technology. All rights reserved.<BR>
 *          US Government Sponsorship under NASA contract NAS7-918 is acknowledged. <BR>
 */
public class HDecompressException extends Exception {

    public HDecompressException(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public HDecompressException(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
