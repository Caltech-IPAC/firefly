package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 8/26/14
 * Time: 2:03 PM
 */


import edu.caltech.ipac.firefly.fuse.data.provider.*;

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
            init= true;
        }
    }

    public static Collection<DatasetInfoConverter> getConverters() {
        init();
        return converterMap.values();
    }

}

