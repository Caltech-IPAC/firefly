/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;


public class PixelValueException extends Exception {

    public PixelValueException () {
        super();
    }

    public PixelValueException (String msg) {
        super(msg);
    }

    public PixelValueException (String msg, Throwable t) {
        super(msg,t);
    }
}



