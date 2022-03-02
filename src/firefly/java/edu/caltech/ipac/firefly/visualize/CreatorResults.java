/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;


/**
 * @author Trey Roby
 */
public class CreatorResults implements Iterable<WebPlotInitializer>, Serializable {
    private final WebPlotHeaderInitializer wpHeader;
    private final WebPlotInitializer[] wpInit;

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

    @Override
    public String toString() {
        String SPLIT_TOKEN= "--CreatorResults--";
        StringBuilder sb= new StringBuilder(1000);
        for(int i = 0; (i< wpInit.length); i++) {
            sb.append(wpInit[i]);
            if (i< wpInit.length-1) sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
    }

}