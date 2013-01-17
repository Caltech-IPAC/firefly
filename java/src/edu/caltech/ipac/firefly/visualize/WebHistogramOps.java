package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.visualize.task.VisTask;

/**
 * Date: May 27, 2005
 *
 * @author Trey Roby
 * @version $Id: WebHistogramOps.java,v 1.19 2012/03/12 18:04:41 roby Exp $
 */
public class WebHistogramOps {


    final Band _band;
    final WebPlot _plot;

    private int[] _dataHistogram= null;
    private int[] _colorHistogram= null;
    private double _meanDataAry[];

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public WebHistogramOps(WebPlot plot,
                           Band band) {
        _band= band;
        _plot= plot;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================



    public static void recomputeStretch(WebPlot plot,
                                        StretchData[] stretchData) {
        VisTask.getInstance().stretch(plot, stretchData);
    }

    public void changeColor(int colorIdx) { VisTask.getInstance().changeColor(_plot, colorIdx); }

    public void computeHistogramImage(int width, int height, AsyncCallback<WebPlotResult> imageUrl) {
        VisTask.getInstance().computeColorHistogram(this, _plot, width, height, imageUrl);
    }

    public Band getBand() { return _band; }

    public void setDataHistogram(int[] dataHistogram ) { _dataHistogram= dataHistogram; }
    public void setMeanDataAry(double[] meanDataAry)     { _meanDataAry= meanDataAry; }


//    public int getHistogramDataFromScreenIdx(int x) {
//        int retval= -1;
//        if (_lineDataSize!=null) {
//            Insets insets= getInsets();
//            Dimension dim= getSize();
//            if (x >= insets.left && x < dim.width-insets.right) {
//                retval= _lineDataSize[x-insets.left];
//            }
//        }
//
//        return retval;
//    }

    public int getColorHistogramIdxFromScreenIdx(int x, int width) {
        if (_colorHistogram==null) return -1;
        int idx= (int)(x *((float)_colorHistogram.length/(float)width));
        return idx;
    }

    public int getDataHistogramIdxFromScreenIdx(int x, int width) {
        if (_dataHistogram==null) return -1;
        int idx= (int)(x *((float)_dataHistogram.length/(float)width));
        return idx;
    }

    public double getDataHistogramMean(int idx) { return _meanDataAry[idx]; }

    public int getColorHistogramValue(int idx) { return _colorHistogram[idx]; }

    public int getDataHistogramValue(int idx) { return _dataHistogram[idx]; }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
