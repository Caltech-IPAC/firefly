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

