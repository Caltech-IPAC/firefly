package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 7/26/13
 * Time: 9:57 AM
 */


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Trey Roby
 */
public class VisStat {

    private final AtomicLong zoom= new AtomicLong(0);
    private final AtomicLong crop= new AtomicLong(0);
    private final AtomicLong rotate= new AtomicLong(0);
    private final AtomicLong color= new AtomicLong(0);
    private final AtomicLong newPlot= new AtomicLong(0);
    private final AtomicLong new3Plot= new AtomicLong(0);
    private final AtomicLong stretch= new AtomicLong(0);
    private final AtomicLong fitsHeader= new AtomicLong(0);
    private final AtomicLong regionRead= new AtomicLong(0);
    private final AtomicLong regionSave= new AtomicLong(0);
    private final AtomicLong flip= new AtomicLong(0);
    private final AtomicLong plotRevalidate= new AtomicLong(0);
    private final AtomicLong areaStat= new AtomicLong(0);
    private final AtomicLong add3ColorBand= new AtomicLong(0);
//    private final AtomicLong flux= new AtomicLong(0) ;
    private final AtomicLong colorHistogram= new AtomicLong(0);
//    private final AtomicLong activeUsers= new AtomicLong(0);


    public static final VisStat instance= new VisStat();

    public static final VisStat getInstance() { return instance; }

    private VisStat() {}

    public void incrementZoom() { zoom.addAndGet(1); }
    public void incrementRotate() { rotate.addAndGet(1); }
    public void incrementColor() { color.addAndGet(1); }
    public void incrementNewPlot() { newPlot.addAndGet(1); }
    public void incrementNew3Plot() { new3Plot.addAndGet(1); }
    public void incrementStretch() { stretch.addAndGet(1); }
    public void incrementFitsHeader() { fitsHeader.addAndGet(1); }
    public void incrementRegionRead() { regionRead.addAndGet(1); }
    public void incrementRegionSave() { regionSave.addAndGet(1); }
    public void incrementFlip() { flip.addAndGet(1); }
    public void incrementPlotRevalidate() { plotRevalidate.addAndGet(1); }
    public void incrementAreaStat() { areaStat.addAndGet(1); }
    public void incrementAdd3ColorBand() { add3ColorBand.addAndGet(1); }
    public void incrementColorHistogram() { colorHistogram.addAndGet(1); }
    public void incrementCrop() { crop.addAndGet(1); }

//    public void incrementFlux() { flux.addAndGet(1); }

    public List<String> getStatus() {
        String start= "- ";
        ArrayList<String> s = new ArrayList<String>();
        s.add(start+"New Plots:         "+newPlot.get());
        s.add(start+"New 3 Color Plots: "+new3Plot.get());
        s.add(start+"Revalidate:        "+plotRevalidate.get());
        s.add(start+"Zooms:             "+zoom.get());
        s.add(start+"Crop:              "+crop.get());
        s.add(start+"Rotates:           "+rotate.get());
        s.add(start+"Color change:      "+color.get());
        s.add(start+"Stretch change:    "+stretch.get());
        s.add(start+"Fits header:       "+fitsHeader.get());
        s.add(start+"Region read:       "+regionRead.get());
        s.add(start + "Region save:       " + regionSave.get());
        s.add(start+"Flip:              "+flip.get());
        s.add(start+"Area State:        "+areaStat.get());
        s.add(start + "3 Color Band:      " + add3ColorBand.get());
        return s;
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
