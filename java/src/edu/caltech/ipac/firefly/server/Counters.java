/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;
/**
 * User: roby
 * Date: 7/26/13
 * Time: 9:57 AM
 */


import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;

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

    public enum Category {Visualization, Search, Browser, OS, Pages, Packaging, Upload, Download, Unknown}
    public enum Unit {CNT, KB}
    public enum Action {INC, DEC, SET}

    private final Map<String,AtomicLong> cntMap= new LinkedHashMap<String,AtomicLong>(303);
    private static final String KEY_SEP= "----";
    private static final String CAT_START= "";
    private static final String KEY_START= "  - ";
    private static final String UNIT_CNT= "CNT";
    private static final String UNIT_KB= "KB";
    private final CatComparator catComparator= new CatComparator();
    private final KeyComparator keyComparator= new KeyComparator();
    private final SizeComparator sizeComparator= new SizeComparator();
    private static final long startTime= System.currentTimeMillis();
    private static final String HOST_NAME= FileUtil.getHostname();



    public static final Counters instance= new Counters();

    public static Counters getInstance() { return instance; }

    private Counters() {
    }

    public void incrementKB(Category cat, String key, long kbSize) {
        updateMap(cat.toString(), key, Unit.KB, kbSize, Action.INC);
    }


    public void increment(Category cat, String key) { increment(cat, key, 1); }

    public void increment(Category cat, String key, int incSize) {
        updateMap(cat.toString(), key, Unit.CNT, incSize, Action.INC);
    }

    public void updateValue(Category cat, String key, long value) {
        updateMap(cat.toString(),key,Unit.CNT,value,Action.SET);
    }

    public void updateValue(String cat, String key, long value) {
        updateMap(cat,key,Unit.CNT,value,Action.SET);
    }

    /**
     * increment the counter.  To minimize the synchronization required, do a unsynchronized get first. If the value is found
     * do a synchronized increment on just that value.  if the value is not found then we might have to create it.
     * At that point do a synchronized lock on the map, do another get (a safe one this time) and if the value still have not
     * be created then create it. This way most of the time we will not have to lock the whole map which might affect
     * several server threads.
     * @param cat the category of the counter
     * @param key the counter key
     * @param unit the unit
     * @param action the action
     */
    private void updateMap(String cat, String key, Unit unit, long inc, Action action) {
        String mapKey= makeMapKey(cat,key,unit);
        AtomicLong v= cntMap.get(mapKey);
        if (v==null) {
            synchronized (cntMap)  {
                v= cntMap.get(mapKey);
                if (v==null) {
                    v= new AtomicLong(0);
                    cntMap.put(mapKey, v);
                }
                modifyValue(v,inc,action);
            }
        }
        else {
            modifyValue(v,inc,action);
        }
    }



    private void modifyValue(AtomicLong value, long modValue, Action action) {
        switch (action) {
            case INC: value.getAndAdd(modValue); break;
            case DEC: value.getAndAdd(-modValue); break;
            case SET: value.set(modValue); break;
        }
    }



    public void initKey(Category cat, String key) { initKey(cat,key,0); }
    public void initKey(String cat, String key) { initKey(cat,key, 0); }
    public void initKey(Category cat, String key, long value) { initKey(cat,key,Unit.CNT,value); }
    public void initKey(String cat, String key, long value) { initKey(cat,key,Unit.CNT,value); }
    public void initKey(Category cat, String key, Unit unit, long value) { initKey(cat.toString(),key,unit,value); }

    public void initKey(String cat, String key, Unit unit, long value) {
        String mapKey= makeMapKey(cat,key,unit);
        synchronized (cntMap)  {
            if (!cntMap.containsKey(mapKey)) cntMap.put(mapKey, new AtomicLong(value));
        }
    }


    public void resetKey(Category cat, String key, long value) { resetKey(cat.toString(),key,value); }

    public void resetKey(String cat, String key, long value) {
        String mapKey= makeMapKey(cat,key,Unit.CNT);
        synchronized (cntMap)  {
            cntMap.put(mapKey, new AtomicLong(value));
        }
    }

    public List<String> reportStatus() {
        List<String> outList= new ArrayList<String>(cntMap.keySet());
        List<String> retList= new ArrayList<String>(cntMap.size()+40);
        Collections.sort(outList,catComparator);
        String lastCat= "START";


        String elapse= UTCTimeUtil.getDHMS((System.currentTimeMillis()-startTime)/1000);
        retList.add("Overview");
        addToList(retList,"Hostname", HOST_NAME);
        addToList(retList, "Up time", elapse);
        retList.add("");
        addMemoryStatus(retList);

        String pagesCat= Category.Pages.toString();
        List<String> pagesList= new ArrayList<String>(300);

        for(String mapKey : outList) {
            KeyParts kp= getKeyParts(mapKey);
            if (kp!=null) {
                if (kp.getCat().equals(pagesCat)) {
                    pagesList.add(mapKey);
                }
                else {
                    if (!kp.getCat().equals(lastCat)) {
                        retList.add("");
                        retList.add(kp.getCat());
                        lastCat= kp.getCat();
                    }
                    switch (kp.getUnit()) {
                        case CNT:
                            addToList(retList,kp.getKey(),cntMap.get(mapKey).get());
                            break;
                        case KB:
                            String sizeStr= StringUtils.getKBSizeAsString(cntMap.get(mapKey).get());
                            addToList(retList,kp.getKey(),sizeStr);
                            break;
                        default:
                            // do nothing
                            break;
                    }

                }
            }
        }
        reportPages(pagesList, retList);

        return retList;
    }



    public void reportPages(List<String> pagesKeys, List<String> retList) {
        if (pagesKeys.size()>0) {
            Collections.sort(pagesKeys,keyComparator);
            Collections.sort(pagesKeys,sizeComparator);
            retList.add("");
            retList.add(Category.Pages.toString()  + " (showing 2 or more hits, 30 max)");
            int cnt=0;
            long hitCnt;
            for(String key : pagesKeys) {
                KeyParts kp= getKeyParts(key);
                hitCnt= cntMap.get(key).get();
                if (hitCnt>1) {
                    addToList(retList,kp.getKey(),hitCnt);
                    cnt++;
                    if (cnt>30) break;
                }
            }
        }
    }



    public void addMemoryStatus(List<String> retList) {
        Runtime rt= Runtime.getRuntime();
        long totMem= rt.totalMemory();
        long freeMem= rt.freeMemory();
        retList.add("Memory");
        long maxMem= rt.maxMemory();
        addMemStrToList(retList,"Used", FileUtil.getSizeAsString(totMem-freeMem));
        addMemStrToList(retList,"Max", FileUtil.getSizeAsString(maxMem));
        addMemStrToList(retList,"Max Free", FileUtil.getSizeAsString(maxMem-(totMem-freeMem)));
        addMemStrToList(retList,"Free Active", FileUtil.getSizeAsString(freeMem));
        addMemStrToList(retList,"Total Active", FileUtil.getSizeAsString(totMem));
        retList.add("");
    }


    private void addToList(List<String>l, String desc, String value) {
        l.add(formatOutStr(desc,value));
    }

    private void addToList(List<String>l, String desc, long value) {
        String s= String.format("%s%-25s : %d",KEY_START, desc, value);
        l.add(s);
    }

    private static String formatOutStr(String desc, String value) {
        return String.format("%s%-25s : %s",KEY_START, desc, value);
    }



    private static void addMemStrToList(List<String>l, String desc, String memStr) {
        String s= String.format("%s%-25s : %9s",KEY_START, desc,memStr);
        l.add(s);
    }

    private static String makeMapKey(String cat, String key, Unit unit) {
        return new KeyParts(cat,key,unit).makeKey();
    }


    private static KeyParts getKeyParts(String mapKey) {
        KeyParts retval= null;
        String sAry[]= mapKey.split(KEY_SEP);
        if (sAry.length==3) {
            try {
                Unit unit= Unit.valueOf(sAry[2]);
                retval= new KeyParts(sAry[0], sAry[1], unit);
            } catch (IllegalArgumentException e) {
                retval= null;
            }
        }
        return retval;
    }



    private static String getCat(String mapKey) {
        String retval= null;
        String sAry[]= mapKey.split(KEY_SEP);
        if (sAry.length==3) {
            retval= sAry[0];
        }
        return retval;
    }


    public void incrementVis(String key){ increment(Category.Visualization,key);}
    public void incrementSearch(String key){ increment(Category.Search,key);}


    public static class CatComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            return ComparisonUtil.doCompare(getCat(s1), getCat(s2));
        }
    }

    public static class KeyComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            return ComparisonUtil.doCompare(getKeyParts(s1).getKey(), getKeyParts(s2).getKey());
        }
    }

    public class SizeComparator implements Comparator<String> {
        public int compare(String key1, String key2) {
            long v1= 0;
            long v2= 0;
            AtomicLong obj1= cntMap.get(key1);
            AtomicLong obj2= cntMap.get(key2);
            if (obj1!=null) v1= obj1.get();
            if (obj2!=null) v2= obj2.get();
            return -1*ComparisonUtil.doCompare(v1,v2);
        }
    }

    private static class KeyParts {
        final private String cat;
        final private String key;
        final private Unit unit;

        private KeyParts(String cat, String key, Unit unit) {
            this.cat = (cat==null) ? Category.Unknown.toString() : cat;
            this.key = key;
            this.unit = unit;
        }

        String getCat() { return cat; }
        String getKey() { return key; }
        Unit getUnit() { return unit; }

        private String makeKey() {
            return cat+KEY_SEP+key+KEY_SEP+unit;
        }
    }
}

