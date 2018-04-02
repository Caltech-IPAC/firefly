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

    WebPlotInitializer _wpInit[];

    public CreatorResults(WebPlotInitializer wpInit[]) {
        _wpInit= wpInit;
    }

    public Iterator<WebPlotInitializer> iterator() {
        return Arrays.asList(_wpInit).iterator();
    }

    public WebPlotInitializer[] getInitializers() {
        return _wpInit;
    }

    public int size() { return _wpInit.length; }

    @Override
    public String toString() {
        StringBuilder sb= new StringBuilder(1000);
        for(int i= 0; (i<_wpInit.length);i++) {
            sb.append(_wpInit[i]);
            if (i<_wpInit.length-1) sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
    }

}

