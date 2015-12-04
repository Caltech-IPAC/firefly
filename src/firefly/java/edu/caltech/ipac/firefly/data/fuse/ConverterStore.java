/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.fuse;
/**
 * User: roby
 * Date: 8/26/14
 * Time: 2:03 PM
 */


import edu.caltech.ipac.firefly.data.fuse.provider.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ConverterStore {

    public static final String DYNAMIC= "DYNAMIC";

    private static Map<String,DatasetInfoConverter> converterMap= new HashMap<String, DatasetInfoConverter>(13);
    private static boolean init= false;

    public static void put(String id, DatasetInfoConverter c) {
        converterMap.put(id,c);
    }

    public static DatasetInfoConverter get(String id) {
        init();
        if (id==null) return null;
        return converterMap.get(id.toUpperCase());
    }

    private static void init() {
        if (!init) {
            put("TWOMASS", new TwoMassDataSetInfoConverter());
            put("WISE",    new WiseDataSetInfoConverter());
            put("SPITZER", new SpitzerDataSetConverter());
            put("DYNAMIC", new DynamicOnlyDataSetInfoConverter());
            put("FINDER_CHART", new FinderChartDataSetInfoConverter());
            put("2MASS",   new TwoMassSIADataSetInfoConverter()); // for old SIA search processor
            put("SIMPLE",  new SimpleDataSetInfoConverter()); // for old SIA search processor
            init= true;
        }
    }

    public static Collection<DatasetInfoConverter> getConverters() {
        init();
        return converterMap.values();
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
