package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/10/13
 * Time: 9:50 AM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class OptimizeForSpeedPurger implements MemoryPurger {

    public final static long MAX_AVAILABLE_K;
    static {
        long maxK= (Runtime.getRuntime().maxMemory()) / FileUtil.K;
        long maxMB= maxK/1024;
        if (maxMB< 2000) {
            MAX_AVAILABLE_K= (long)(maxK*.1);
        }
        else if (maxMB< 5000) {
            MAX_AVAILABLE_K= (long)(maxK*.2);
        }
        else {
            MAX_AVAILABLE_K= (long)(maxK*.21);
        }
    }



    public void purgeOtherPlots(PlotState excludeState) {
        PlotClientCtx excludeCtx= VisContext.getPlotCtx(excludeState.getContextString());
        String excludeKey= excludeCtx!=null ? excludeCtx.getKey() : null;
        synchronized (VisContext.class) {
            long totalInUseK= 0;
            long totalCnt= 0;
            long startTotal= 0;
            List<PlotClientCtx> allInUseCtx= new ArrayList<PlotClientCtx>(500);
            Cache cache= VisContext.getCache();
            List<String> keys= cache.getKeys();
            boolean freed;
            for(String key: keys) {
                Object o= cache.get(new StringKey(key));
                if (o instanceof VisContext.UserCtx) {
                    Map<String,PlotClientCtx> map= ((VisContext.UserCtx)o).getMap();
                    PlotClientCtx ctx;
                    for(Map.Entry<String,PlotClientCtx> entry : map.entrySet()) {
                        ctx= entry.getValue();
                        if (!ctx.getKey().equals(excludeKey)) {
                            if (ctx.getPlot()!=null) {  // if we are using memory
                                freed= entry.getValue().freeResources(PlotClientCtx.Free.OLD);
                                if (freed)  {
                                    totalCnt++;
                                }
                                else {
                                    totalInUseK+= ctx.getDataSizeK();
                                    allInUseCtx.add(ctx);
                                }
                                startTotal+= ctx.getDataSizeK();
                            }
                        }
                    }
                }
            }
            String aggressiveDesc= "";
            if (totalInUseK>MAX_AVAILABLE_K) {
                long purgeDownToK= (long)(MAX_AVAILABLE_K*.80);
                Collections.sort(allInUseCtx, new Comparator<PlotClientCtx>() {
                    public int compare(PlotClientCtx c1, PlotClientCtx c2) {
                        return -1 * ComparisonUtil.doCompare(c1.getAccessTime(), c2.getAccessTime());
                    }
                });

                for(PlotClientCtx ctx : allInUseCtx) {
                    if (!ctx.getKey().equals(excludeKey)) {
                        freed= ctx.freeResources(PlotClientCtx.Free.YOUNG);
                        if (freed) {
                            totalInUseK-= ctx.getDataSizeK();
                            totalCnt++;
                            if (totalInUseK<purgeDownToK) break;
                        }
                    }
                }

                if (totalInUseK>MAX_AVAILABLE_K*1.2) {
                    aggressiveDesc= ", aggressive";
                    for(PlotClientCtx ctx : allInUseCtx) {
                        if (!ctx.getKey().equals(excludeKey)) {
                            freed= ctx.freeResources(PlotClientCtx.Free.VERY_YOUNG);
                            if (freed) {
                                totalInUseK-= ctx.getDataSizeK();
                                totalCnt++;
                                if (totalInUseK<purgeDownToK) break;
                            }
                        }
                    }
                }
                if (totalInUseK>MAX_AVAILABLE_K*1.2) {
                    aggressiveDesc= ", very aggressive";
                    for(PlotClientCtx ctx : allInUseCtx) {
                        if (!ctx.getKey().equals(excludeKey)) {
                            freed= ctx.freeResources(PlotClientCtx.Free.INFANT);
                            if (freed) {
                                totalInUseK-= ctx.getDataSizeK();
                                totalCnt++;
                                if (totalInUseK<purgeDownToK) break;
                            }
                        }
                    }
                }


                if (totalCnt==0) {
                    Logger.debug("Free resources : no candidates to free, " +
                                         "current memory exceeds target, current/target (MB): " +
                                         totalInUseK / 1024 + " / " + MAX_AVAILABLE_K / 1024);

                }
            }
            if (totalCnt>0) {
                String exceeds= totalInUseK>MAX_AVAILABLE_K ? ", in use memory still exceeds target" : "";
                Logger.debug("Free resources : start/end/target (MB): "+ startTotal/1024 +
                                     " / "+ totalInUseK/1024+
                                     " / "+ MAX_AVAILABLE_K/1024+
                                     ", plots freed: "+totalCnt + exceeds+ aggressiveDesc );
            }
        }
        //To change body of implemented methods use File | Settings | File Templates.
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
