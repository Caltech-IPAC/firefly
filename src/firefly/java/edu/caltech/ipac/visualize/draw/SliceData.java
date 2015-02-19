/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.visualize.plot.*;

import java.awt.geom.Point2D;
import java.awt.*;
import java.util.*;

/**
 * Data for Slice
 * @author Tatiana Goldina
 */
public class SliceData  implements Iterable<SliceData.Series> {

    public static Color [] SLICE_COLORS = {
            new Color(102, 0, 0, 200),
            new Color(51, 0, 102, 200),
            new Color(0, 102, 102, 200),
            new Color(102, 0, 102, 200) };

    public enum Band {
        RED(ImagePlot.RED, "Red", new Color(204, 0, 0, 200)),
        GREEN(ImagePlot.GREEN, "Green", new Color(0, 204, 0, 200)),
        BLUE(ImagePlot.BLUE, "Blue", new Color(0, 0, 204, 200));

        int getBand() { return _band;}
        String getLabel() {return _label;}
        Color getColor() {return _color;}

        Band(int band, String label, Color color) {
            _band = band;
            _label = label;
            _color = color;
        }
        int _band;
        String _label;
        Color _color;
    }

    private final int MAX_SERIES = 4;
    private final int MAX_DIST = 8;

    private ImagePlot _plot;
    private WorldPt [] _pts;
    private ImageWorkSpacePt [] _ipts;
    private int _size;
    private CoordinateSys  _coordSys;

    private int       _nSeries;
    private Series [] _series;
    private String _error;

    private Point2D [] _spts;
    private double _scale; // to cache screen points

    // instance vars that define the quation of the line and step
    private boolean _xStep;
    private double  _slope;
    private double  _yIntercept;
    private double  _islope;
    private double  _xIntercept;



    public SliceData(ImagePlot plot, VectorObject vectObject) {

        _plot = plot;
        _spts = null;
        _scale = -10000.0;

        try {
            PlotGroup plotGroup = plot.getPlotGroup();
            WorldPt wp1 = vectObject.getWorldPt(0);
            WorldPt wp2 = vectObject.getWorldPt(1);
            _coordSys = wp1.getCoordSys();

            ImageWorkSpacePt ip1 = plot.getImageCoords(wp1);
            ImageWorkSpacePt ip2 = plot.getImageCoords(wp2);

            //System.out.println("Image point 1: "+ip1);
            //System.out.println("Image point 2: "+ip2);

            double x1 = ip1.getX();
            double y1 = ip1.getY();

            double x2 = ip2.getX();
            double y2 = ip2.getY();

            // delta X and Y in image pixels
            double deltaX = Math.abs(ip2.getX() - ip1.getX());
            double deltaY = Math.abs(ip2.getY() - ip1.getY());

            double x, y;
            if (deltaX > deltaY) {
                _xStep = true;
                _slope = (y2-y1)/(x2-x1);
                _yIntercept = y1-_slope*x1;

                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2) ;
                // smallest x within the plot is 0
                // biggest x within the plot is plot.getImageDataWidth()-1
                if (minX >= plotGroup.getGroupImageXMax() ||
                    maxX <= plotGroup.getGroupImageXMin()) {
                    initOnError("Slice segment is outside the image");
                    return;
                }
                minX = Math.max(minX, plotGroup.getGroupImageXMin());
                maxX = Math.min(maxX, plotGroup.getGroupImageXMax());

                int n = (int)Math.rint(Math.ceil(maxX-minX))+1;
                _pts = new WorldPt[n];
                _ipts = new ImageWorkSpacePt[n];

                int idx = 0;
                ImageWorkSpacePt ipt;
                for (x=minX; x<=maxX; x+=1) {
                    y = _slope*x + _yIntercept;
                    ipt= new ImageWorkSpacePt(x, y);
                    if (plot.pointInPlot(ipt)) {
                        _ipts[idx]= ipt;
                        _pts[idx] = plot.getWorldCoords(_ipts[idx], _coordSys);
                        idx++;
                    }
                }
                if (idx == 0) {
                    initOnError("Slice segment is outside the image.");
                    return;
                }
                _size = idx;

            } else if (y1 != y2) {
                _xStep = false;
                _islope = (x2-x1)/(y2-y1);
                _xIntercept = x1-_islope*y1;

                double minY = Math.min(y1, y2);
                double maxY = Math.max(y1, y2);
                // smallest y within the plot is 0
                // biggest y within the plot is plot.getImageDataHeight()-1
                if (minY >= plotGroup.getGroupImageYMax() ||
                    maxY <= plotGroup.getGroupImageYMin()) {
                    initOnError("Slice segment is outside the image");
                    return;
                }
                minY = Math.max(minY, plotGroup.getGroupImageYMin());
                maxY = Math.min(maxY, plotGroup.getGroupImageYMax());

                int n = (int)Math.rint(Math.ceil(maxY - minY))+1;
                _pts = new WorldPt[n];
                _ipts = new ImageWorkSpacePt[n];

                int idx = 0;
                ImageWorkSpacePt ipt;
                for (y=minY; y<=maxY; y+=1) {
                    x = _islope*y + _xIntercept;
                    ipt= new ImageWorkSpacePt(x, y);
                    if (plot.pointInPlot(ipt)) {
                        _ipts[idx] = ipt;
                        _pts[idx] = plot.getWorldCoords(_ipts[idx], _coordSys);
                        idx++;
                    }
                }
                if (idx == 0) {
                    initOnError("Slice segment is outside the image.");
                    return;
                }
                _size = idx;

            } else {
                initOnError("Please, draw a vector of non-zero length.");
            }

            Plot p;
            ImagePlot imagePlot;
            _series = new Series[MAX_SERIES];
            _nSeries = 0;
            ArrayList <Color> cols = new ArrayList<Color>(Arrays.asList(SLICE_COLORS));
            for(Iterator<Plot> iter = plot.getPlotGroup().iterator(); (iter.hasNext() && _nSeries<MAX_SERIES); ) {
                p = iter.next();
                if (p instanceof ImagePlot && p.isShowing()) {
                    imagePlot = (ImagePlot)p;
                    if (imagePlot.isThreeColor()) {
                        // process three-color
                        for (Band b : Band.values() ) {
                            int band = b.getBand();
                            if (imagePlot.isColorBandVisible(band)) {
                                // set flux for the band
                                boolean skipSeries = false;
                                double flux[] = new double[_size];
                                for (int i=0; i<_size; i++) {
                                    try {
                                        flux[i] = imagePlot.getFlux(band, _ipts[i]);
                                    } catch (PixelValueException pve) {
                                        //initOnError("Color band flux is not available.");
                                        //return;
                                        skipSeries = true;
                                        break;
                                    }
                                }
                                if (!skipSeries) {
                                    _series[_nSeries] = new Series(flux, imagePlot, b);
                                    _nSeries++;
                                }
                                if (_nSeries >= MAX_SERIES) { break;}
                            }
                        }
                    } else {
                        // not three-color
                        boolean skipSeries = false;
                        double flux[] = new double[_size];
                        for (int i=0; i<_size; i++) {
                            try {
                                flux[i] = imagePlot.getFlux(_ipts[i]);
                            } catch (PixelValueException pve) {
                                //initOnError("Overlay flux is not available.");
                                //return;
                                skipSeries = true;
                                break;
                            }
                        }
                        if (!skipSeries) {
                            Object sliceColor = imagePlot.getAttribute("sliceColor");
                            if (sliceColor == null || (!cols.remove(sliceColor))) {
                                Color color = cols.remove(0);
                                imagePlot.setAttribute("sliceColor", color);
                            }

                            _series[_nSeries] = new Series(flux, imagePlot);
                            _nSeries++;
                        }
                    }
                }
            }
            if (_nSeries == 0) {
                initOnError("Make sure the slice segment is completely inside an image.");
            }

        } catch (Exception e) {
            initOnError(e.getMessage());
        }
    }


    private void initOnError(String error) {
        _pts = null;
        _ipts = null;
        _spts = null;
        _scale = -10000.0;
        _series = null;
        _nSeries = 0;
        _size = 0;
        _coordSys = CoordinateSys.UNDEFINED;
        _error = "Can not construct slice. "+error;
    }

    public int getNSearies() {
        return _nSeries;
    }

    public Series getSeries(int idx) {
        return _series[idx];
    }

    public ImagePlot getPlot() {
        return _plot;
    }

    public int getSize() {
        return _size;
    }

    public WorldPt getWorldPt(int i) throws ArrayIndexOutOfBoundsException {
        if (_size == 0 || i < 0 || i >= _size) {
            throw new ArrayIndexOutOfBoundsException("No slice data with index " + i);
        } else {
            return _pts[i];
        }
    }

    public ImageWorkSpacePt getImagePt(int i) throws ArrayIndexOutOfBoundsException {
        if (_size == 0 || i < 0 || i >= _size) {
            if (i >= _size && _size > 2) {
                // approximate
                ImageWorkSpacePt last = _ipts[_size-1];
                double x, y;
                if (_xStep) {
                    x = last.getX()+(i-_size+1);
                    y = _slope*x + _yIntercept;
                } else {
                    y = last.getY()+(i-_size+1);
                    x = _islope*y + _xIntercept;
                }
                return new ImageWorkSpacePt(x, y);
            } else {
                throw new ArrayIndexOutOfBoundsException("No slice data with index " + i);
            }
        } else {
            return _ipts[i];
        }
    }

    public int getNSeries() {
        return _nSeries;
    }

    /**
     * Non-null errors means slice data were not constructed properly
     * @return error
     */
    public String getError() {
        return _error;
    }

    /**
     * Get index of a position, that is close to a given point in screen
     * coordinates, If none of the positions are close to the given, -1
     * is returned,
     * @return closest sample position index
     * @param sPt point
     */
    public int getIdxCloseToScreenPt(Point2D sPt) {
        if (_ipts == null) return -1;
        if (_spts == null || _scale != _plot.getScale()) {
            _spts = new Point2D[_size];
            for (int i=0; i<_size; i++) {
                _spts[i] = _plot.getScreenCoords(_ipts[i]);
            }
            _scale = _plot.getScale();
        }
        double dist;
        double minDist = MAX_DIST;
        int closest = -1; // -1 means none is close enough
        for (int i=0; i<_size; i++) {
            dist = sPt.distance(_spts[i]);
            if (dist < MAX_DIST && dist < minDist) {
                 closest = i;
                 minDist = dist;
            }
        }
        return closest;
    }

    public Iterator<Series> iterator() {
        return new Iterator<Series>() {
            int ii = 0;
            public boolean hasNext() {
                return (ii<_nSeries);
            }

            public Series next() {
                if (ii >= _nSeries) {
                    throw new NoSuchElementException("Series does not exist - "+ii);
                } else {
                    ii++;
                    return _series[ii-1];
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    class Series {

        private ImagePlot _imagePlot;
        private boolean   _isThreeColor;
        private Band      _band;
        private double    _flux[];

        Series(double[] flux, ImagePlot imagePlot) {
            this(flux, imagePlot, null);

        }

        Series(double[] flux, ImagePlot imagePlot, Band band) {
            _flux = flux;
            _imagePlot = imagePlot;
            _band = band;
            _isThreeColor = band != null;
        }

        ImagePlot getImagePlot() { return _imagePlot;}
        boolean isThreeColor() { return _isThreeColor;}
        Band getBand() { return _band;}
        double getFlux(int sampleIdx) { return _flux[sampleIdx]; }

    }
}





