/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.io.Serializable;


public class PixelValueException extends Exception implements Serializable {

    public PixelValueException () { super(); }

    public PixelValueException (String msg) { super(msg); }

}



