/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

/**
 * Holds the flag which determines whether debug messages should be emitted 
 * 
 * @author Booth Hartley
 */
public final class SUTDebug
{

    /**
     * flag which determines whether debug messages should be emitted
     */
    private static boolean DEBUG = false;
    

    /**
     * check flag which determines whether debug messages should be emitted
     */
    public static boolean isDebug() {
	return DEBUG;
    }
    

    /**
     * set flag which determines whether debug messages should be emitted
     */
    public static void setDebug(boolean b) {
	DEBUG = b;
    }
    


}
