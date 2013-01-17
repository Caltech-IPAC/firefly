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
