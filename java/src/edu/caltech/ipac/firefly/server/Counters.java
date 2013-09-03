package edu.caltech.ipac.firefly.server;
/**
 * User: roby
 * Date: 7/26/13
 * Time: 9:57 AM
 */


import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Trey Roby
 */
public class Counters {

    public enum Category {Visualization, Search, Browser, Packaging, Unknown}

    private final Map<String,AtomicLong> cntMap= new LinkedHashMap<String,AtomicLong>(303);
    private static final String KEY_SEP= "----";
    private static final String CAT_START= "";
    private static final String KEY_START= "  - ";
    private final CatComparator catComparator= new CatComparator();



    public static final Counters instance= new Counters();

    public static Counters getInstance() { return instance; }

    private Counters() {
       testInit();
    }



    public void increment(Category cat, String key) {
        increment(cat.toString(),key);
    }

    /**
     * increment the counter.  To minimize the synchronization required, do a unsynchronized get first. If the value is found
     * do a synchronized increment on just that value.  if the value is not found then we might have to create it.
     * At that point do a synchronized lock on the map, do another get (a safe one this time) and if the value still have not
     * be created then create it. This way most of the time we will not have to lock the whole map which might affect
     * several server threads.
     * @param cat the category of the counter
     * @param key the counter key
     */
    private void increment(String cat, String key) {
        String mapKey= makeMapKey(cat,key);
        AtomicLong v= cntMap.get(mapKey);
        if (v==null) {
            synchronized (cntMap)  {
                v= cntMap.get(mapKey);
                if (v==null) cntMap.put(mapKey, new AtomicLong(1));
                else v.getAndAdd(1);
            }
        }
        else {
            v.getAndAdd(1);
        }
    }

    public void initKey(Category cat, String key) {
        initKey(cat.toString(),key);
    }

    private void initKey(String cat, String key) {
        String mapKey= makeMapKey(cat,key);
        synchronized (cntMap)  {
            if (!cntMap.containsKey(mapKey)) cntMap.put(mapKey, new AtomicLong(0));
        }
    }

    public List<String> reportStatus() {
        List<String> outList= new ArrayList<String>(cntMap.keySet());
        List<String> retList= new ArrayList<String>(cntMap.size()+40);
        Collections.sort(outList,catComparator);
        String lastCat= "START";
        String cat;
        String key;
        for(String mapKey : outList) {
            cat= getCat(mapKey);
            if (cat!=null) {
                if (!cat.equals(lastCat)) {
                    retList.add("");
                    retList.add(cat);
                    lastCat= cat;
                }
                key= getKey(mapKey);
                if (key!=null) {
                    retList.add(String.format("%s%-25s : %d",KEY_START, key,cntMap.get(mapKey).get()));
                }
            }
        }
        return retList;
    }

    private static String makeMapKey(String cat, String key) {
        if (cat==null) cat= Category.Unknown.toString();
        return cat+KEY_SEP+key;
    }

    private static String getCat(String mapKey) {
        String retval= null;
        String sAry[]= mapKey.split(KEY_SEP);
        if (sAry.length==2) {
            retval= sAry[0];
        }
        return retval;
    }

    private static String getKey(String mapKey) {
        String retval= null;
        String sAry[]= mapKey.split(KEY_SEP);
        if (sAry.length==2) {
            retval= sAry[1];
        }
        return retval;
    }


    public void incrementVis(String key){ increment(Category.Visualization,key);}
    public void incrementSearch(String key){ increment(Category.Search,key);}


    private void testInit() {
        incrementSearch("test");
        increment(Category.Search, "test");
        increment(Category.Search, "xxx");
    }


    public static class CatComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            return ComparisonUtil.doCompare(getCat(s1), getCat(s2));
        }
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
