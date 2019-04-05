/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
/**
 * User: roby
 * Date: Apr 5, 2010
 * Time: 12:34:32 PM
 */


/**
 * @author Trey Roby
 */
public class CreatorResults implements Iterable<WebPlotInitializer>, Serializable {

    private final static String SPLIT_TOKEN= "--CreatorResults--";

    private WebPlotHeaderInitializer wpHeader;
    private WebPlotInitializer[] wpInit;

    public CreatorResults(WebPlotHeaderInitializer wpHeader, WebPlotInitializer[] wpInit) {
        this.wpInit = wpInit;
        this.wpHeader= wpHeader;
    }

    public Iterator<WebPlotInitializer> iterator() {
        return Arrays.asList(wpInit).iterator();
    }

    public WebPlotHeaderInitializer  getInitHeader() { return wpHeader; }

    public WebPlotInitializer[] getInitializers() {
        return wpInit;
    }

    public int size() { return wpInit.length; }

    @Override
    public String toString() {
        StringBuilder sb= new StringBuilder(1000);
        for(int i = 0; (i< wpInit.length); i++) {
            sb.append(wpInit[i]);
            if (i< wpInit.length-1) sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
    }

}

