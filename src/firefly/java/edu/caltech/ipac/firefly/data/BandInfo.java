/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Apr 9, 2010
 * Time: 10:13:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class BandInfo implements Serializable, DataEntry, HandSerialize {

    private final static String SPLIT_TOKEN= "--BandInfo--";
    private final static String ELEMENT_TOKEN_LEVEL1= "--BIElement--";
    private final static String ELEMENT_TOKEN_LEVEL2= "--BI2Element--";
    private HashMap<Band, RawDataSet> rawDataMap;
    private HashMap<Band, String> stringMap;
    private HashMap<Band, HashMap<Metrics, Metric>> metricsMap;

    public BandInfo(){}

    public BandInfo(HashMap<Band, RawDataSet> rdMap, HashMap<Band, String> strMap, HashMap<Band, HashMap<Metrics, Metric>> mMap){
        this.rawDataMap = rdMap;
        this.stringMap = strMap;
        this.metricsMap = mMap;

    }


    public HashMap<Band, RawDataSet> getRawDataMap() {
        return rawDataMap;
    }

    public HashMap<Band, String> getStringMap() {
        return stringMap;
    }


    public HashMap<Band, HashMap<Metrics, Metric>> getMetricsMap() {
        return metricsMap;
    }

    public String serialize() {
        StringBuffer sb= new StringBuffer(2000);

        // rawDataMap
        sb.append('[');
        if (rawDataMap!=null) {
            for(Map.Entry<Band,RawDataSet> entry: rawDataMap.entrySet()) {
                sb.append(entry.getKey()).append(ELEMENT_TOKEN_LEVEL1);
                sb.append(entry.getValue().serialize()).append(ELEMENT_TOKEN_LEVEL1);
            }
        }
        sb.append(']').append(SPLIT_TOKEN);

        // StringMap
        sb.append('[');
        if (stringMap!=null) {
            for(Map.Entry<Band,String> entry: stringMap.entrySet()) {
                sb.append(entry.getKey()).append(ELEMENT_TOKEN_LEVEL1);
                sb.append(entry.getValue()).append(ELEMENT_TOKEN_LEVEL1);
            }
        }
        sb.append(']').append(SPLIT_TOKEN);



        // metricMap
        sb.append('[');
        if (metricsMap!=null) {
            for(Map.Entry<Band,HashMap<Metrics,Metric>> entry: metricsMap.entrySet()) {
                sb.append(entry.getKey()).append(ELEMENT_TOKEN_LEVEL1);
                sb.append('[');
                for(Map.Entry<Metrics,Metric> me : entry.getValue().entrySet()) {
                    sb.append(me.getKey()).append(ELEMENT_TOKEN_LEVEL2);
                    sb.append(me.getValue().serialize()).append(ELEMENT_TOKEN_LEVEL2);
                }
                sb.append(']');
                sb.append(ELEMENT_TOKEN_LEVEL1);
            }
        }
        sb.append(']');


        return sb.toString();


    }

    public static BandInfo parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,4);
        if (sAry.length==3) {
            BandInfo retval= new BandInfo();
            int i= 0;

            Map<String,String> sMap;

            sMap= StringUtils.parseStringMap(sAry[i++], ELEMENT_TOKEN_LEVEL1);
            if (sMap.size()>0) {
                retval.rawDataMap= new HashMap<Band, RawDataSet>(sMap.size()+17);
                for(Map.Entry<String,String> e : sMap.entrySet()) {
                    Band b= Band.parse(e.getKey());
                    RawDataSet rds= RawDataSet.parse(e.getValue());
                    if (b!=null && rds!=null) retval.rawDataMap.put(b,rds);
                }
            }


            sMap= StringUtils.parseStringMap(sAry[i++], ELEMENT_TOKEN_LEVEL1);
            if (sMap.size()>0) {
                retval.stringMap= new HashMap<Band, String>(sMap.size()+17);
                for(Map.Entry<String,String> e : sMap.entrySet()) {
                    Band b= Band.parse(e.getKey());
                    if (b!=null) retval.stringMap.put(b,e.getValue());
                }
            }


            sMap= StringUtils.parseStringMap(sAry[i++], ELEMENT_TOKEN_LEVEL1);
            if (sMap.size()>0) {
                retval.metricsMap= new HashMap<Band, HashMap<Metrics, Metric>>(sMap.size()+17);
                for(Map.Entry<String,String> e : sMap.entrySet()) {
                    Band b= Band.parse(e.getKey());
                    HashMap<Metrics, Metric> mmMap= new HashMap<Metrics, Metric>(23);
                    Map<String,String> mmStringMap= StringUtils.parseStringMap(e.getValue(), ELEMENT_TOKEN_LEVEL2);
                    if (mmStringMap.size()>0)  {
                        for(Map.Entry<String,String> e2 : mmStringMap.entrySet()) {
                            Metrics metrics= Enum.valueOf(Metrics.class,e2.getKey());
                            Metric metric= Metric.parse(e2.getValue());
                            if (metrics!=null && metric!=null) {
                                mmMap.put(metrics,metric);
                            }
                        }
                    }
                    if (b!=null && mmMap.size()>0) retval.metricsMap.put(b,mmMap);
                }
            }
            return  retval;


        }
        return null;
    }
}
