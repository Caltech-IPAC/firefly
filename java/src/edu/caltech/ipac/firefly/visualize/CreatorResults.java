package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.DataEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Apr 5, 2010
 * Time: 12:34:32 PM
 */


/**
 * @author Trey Roby
 */
public class CreatorResults implements Iterable<WebPlotInitializer>, Serializable, DataEntry {

    private final static String SPLIT_TOKEN= "--CreatorResults--";

    WebPlotInitializer _wpInit[];

    private CreatorResults() {}

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

    public static CreatorResults parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,10);
        List<WebPlotInitializer> list= new ArrayList<WebPlotInitializer>(5);
        for(String wpStr : sAry) {
            list.add(WebPlotInitializer.parse(wpStr));
        }
        CreatorResults retval= new CreatorResults(list.toArray(new WebPlotInitializer[list.size()]));
        return retval;

    }
}

