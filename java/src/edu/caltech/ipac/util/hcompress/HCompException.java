/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.hcompress;

/**
 * TODO Write Javadocs
 *
 * @author <a href="mailto:jchavez@ipac.caltech.edu?subject=Java Docs">Joe Chavez</a>
 * @version $Id: HCompException.java,v 1.2 2005/12/01 00:56:21 roby Exp $
 *          <hr>
 *          Represents the data contained in an Observation Request. <BR>
 *          Copyright (C) 1999-2005 California Institute of Technology. All rights reserved.<BR>
 *          US Government Sponsorship under NASA contract NAS7-918 is acknowledged. <BR>
 */
public class HCompException extends Exception {

    public HCompException(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public HCompException(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
