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
