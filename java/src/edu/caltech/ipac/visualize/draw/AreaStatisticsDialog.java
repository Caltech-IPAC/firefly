package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.OSInfo;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.*;
import edu.caltech.ipac.visualize.plot.*;
import edu.caltech.ipac.data.DataConst;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;


/**
 * Dialog for Flux Statistics
 * @author Tatiana Goldina
 */
public class AreaStatisticsDialog {
     // three-color support
     public enum WhichReadout {LEFT, RIGHT }
     public enum WhichDir {LON, LAT }
     public enum ReadoutMode {HMS, DECIMAL }

     public enum Band {
        RED(ImagePlot.RED, "red"),
        GREEN(ImagePlot.GREEN, "green"),
        BLUE(ImagePlot.BLUE, "blue"),
        NO_BAND(ImagePlot.NO_BAND, "") ;

        int getBand() { return _band;}
        String getLabel() {return _label;}

        Band(int band, String label) {
            _band = band;
            _label = label;
        }
        int _band;
        String _label;
    }

    private final static ClassProperties _prop  = new ClassProperties(AreaStatisticsDialog.class);
    private final static String DIALOG_TITLE  = _prop.getTitle();
    private final static String COLOR         = _prop.getName("color");

    // degrees to readians conversion factor
    private static final double DtoR = Math.PI/180.0;

    // min, max, centroid, and flux weighted centroid
    // will be displayed as a number of JTextPanes
    // positioned using 4x3 GridLayout

    // define rows of GridLayout
    private static final int MAX = 0;
    private static final int MIN = 1;
    private static final int CENTROID = 2;
    private static final int FW_CENTROID = 3;
    private static final int [] rows = {MAX, MIN, CENTROID, FW_CENTROID};
    //private static final String [] names = {"MAX", "MIN", "FC", "FWC"};

    // define columns of GridLayout
    private static final int NAME = 0;
    private static final int WORLD_COORD = 1;
    private static final int IMAGE_COORD = 2;
    private static final int [] columns = {NAME, WORLD_COORD, IMAGE_COORD};

    // define mean, standard deviation, and integrated flux fields, displayed in the head
    private static final int MEAN = 0;
    private static final int STDEV = 1;
    private static final int INTEGRATED_FLUX = 2;
    private static final int [] hcolumns = {MEAN, STDEV, INTEGRATED_FLUX};

    // text fields


    private static Class controlClass;

    // marks the location of min/max/centrod/fw_centroid on the image
    private final static SkyShape MARKER_SKY_SHAPE = SkyShapeFactory.getInstance().getSkyShape("bigX");
    // private final static StringShape MARKER_STRING_SHAPE = new StringShape(8, StringShape.EAST, new Font("Arial", Font.PLAIN, 18));
    private FixedObjectGroup    _currentPts;
    private PlotView            _currentPV;
    private PlotGroup           _currentPlotGroup;
    private ShapeObject         _shapeObject;



    public static String formatPosHtml(WhichReadout which, Plot plot, ImageWorkSpacePt ip) {
        return "todo: fixe";
        //return DefaultMouseReadoutHandler.formatReadoutByImagePt(which, plot, ip, "<br>");
    }

    private static String formatPos(WhichReadout which, Plot plot, ImageWorkSpacePt ip) {
        return "todo: fixe";
        //return DefaultMouseReadoutHandler.formatReadoutByImagePt(which, plot, ip, ", ");
    }


    /**
     * The integrated flux (total flux from the area has different units from the flux
     * For example, if the flux unit (BUNIT in the header is "JY/SR" (Jansky per sterradian),
     * then the integrated flux is JY.
     * This method converts flux units into integrated flux units.
     * @param fluxUnits flux units
     * @return integrated flux units
     */
    private static String getIntegratedFluxUnits(String fluxUnits) {
        final String [] match = {"/STER", "-SR", "/SR", "/ST", "*SR-1", "*ST-1"};
        String fluxUnitsUpper = fluxUnits.toUpperCase();
        int idx = -1;
        if (fluxUnitsUpper.indexOf("S") > 0) {
            for (String s : match) {
                idx = fluxUnitsUpper.lastIndexOf(s);
                if (idx > 0)
                    break;
            }
        }
        if (idx > 0) {
            return fluxUnits.substring(0, idx);
        } else {
            return fluxUnits.trim()+"*STER";
        }
    }






    class PlotListItem {
        ImagePlot _plot;
        Band _band;

        public PlotListItem(ImagePlot plot, Band band) {
            this._plot = plot;
            this._band = band;
        }

        ImagePlot getPlot() {return this._plot;}
        Band getBand() {return this._band; }

        boolean equals(PlotListItem pli) {
            return pli != null && pli.getPlot() == this._plot &&
                    pli.getBand().getBand() == this._band.getBand();
        }
    }


    /**
     * Get hash map with Area Statistics Metrics
     * @param plot  image plot
     * @param selectedBand selected color band, one of (ImagePlot.NO_BAND, ImagePlot.RED, ImagePlot.BLUE, ImagePlot.GREEN)
     * @param shape shape that contains region of calculation
     * @param boundingBox if not null, region of calculation will be intersection of shape and boundingBox
     * @return map of calculated metrics by metric id
     */
    public static HashMap<Metrics, Metric> getStatisticMetrics(ImagePlot plot, int selectedBand, Shape shape, Rectangle2D boundingBox) {

        //comment    
        if (boundingBox == null) boundingBox = shape.getBounds2D();
        double minX = boundingBox.getMinX();
        double maxX = boundingBox.getMaxX();
        double minY = boundingBox.getMinY();
        double maxY = boundingBox.getMaxY();

        double minimumX = 0d;
        double minimumY = 0d;
        double minimumFlux = Double.MAX_VALUE;
        double maximumX = 0d;
        double maximumY = 0d;
        double maximumFlux = Double.MIN_VALUE;
        double flux;
        double nPixels = 0d;
        double fluxSum = 0d;
        double squareSum = 0d;
        double xfSum = 0d;
        double yfSum = 0d;
        double xSum = 0d;
        double ySum = 0d;
        FitsRead fitsRead = (selectedBand == ImagePlot.NO_BAND) ? plot.getFitsRead() : plot.getImageData().getFitsRead(selectedBand);
        int imageScaleFactor = fitsRead.getImageScaleFactor();
        if (imageScaleFactor < 1) imageScaleFactor = 1;
        // to calculate the number of pixels (and integratedFlux) correctly in overlay plots,



        /* adjust minX, minY, maxX, maxY to be centered on a pixel */
        ImageWorkSpacePt iwwspt = new ImageWorkSpacePt(minX,minY);
        ImagePt ippt = plot.getImageCoords(iwwspt);
        ippt = new ImagePt(
                Math.rint(ippt.getX() + 0.5) -0.5,
                Math.rint(ippt.getY() + 0.5) -0.5);
        iwwspt = plot.getImageCoords(ippt);
        minX = iwwspt.getX();
        minY = iwwspt.getY();

        iwwspt = new ImageWorkSpacePt(maxX,maxY);
        ippt = plot.getImageCoords(iwwspt);
        ippt = new ImagePt(
                Math.rint(ippt.getX() + 0.5) -0.5,
                Math.rint(ippt.getY() + 0.5) -0.5);
        iwwspt = plot.getImageCoords(ippt);
        maxX = iwwspt.getX();
        maxY = iwwspt.getY();


        // I need to take into account imageScaleFactor from FitsRead
        for (double y=minY; y<=maxY; y+=imageScaleFactor ) {
            for (double x=minX; x<=maxX; x+=imageScaleFactor) {
                try {
                    if (!shape.contains(x, y)) continue;
                    if (selectedBand == ImagePlot.NO_BAND) {
                        flux = plot.getFlux(new ImageWorkSpacePt(x, y));
                    } else {
                        flux = plot.getFlux(selectedBand, new ImageWorkSpacePt(x, y));
                    }
                    //if (SUTDebug.isDebug()) System.out.println("x = "+x+";   y = "+y);
		    if (Double.isNaN(flux))
		    {
			continue;
		    }
                    nPixels++;
                    fluxSum += flux;
                    squareSum += flux*flux;
                    xfSum += x*flux;
                    yfSum += y*flux;
                    xSum += x;
                    ySum += y;

                    if (flux < minimumFlux) {
                        minimumFlux = flux;
                        minimumX = x;
                        minimumY = y;
                    }
                    if (flux > maximumFlux) {
                        maximumFlux = flux;
                        maximumX = x;
                        maximumY = y;
                    }
                } catch (PixelValueException pve) {
                    // do nothing
//                    System.out.println("Pixel Value Exception: ("+x+","+y+") "+pve.getMessage());
                }
            }
        }
        if (nPixels == 0) {
            HashMap<Metrics, Metric> metrics = new HashMap<Metrics, Metric>(1);
            metrics.put(Metrics.NUM_PIXELS, new Metric("Number Of Pixels", null, 0, ""));
            return metrics;
        }

        String fluxUnits = ((selectedBand == ImagePlot.NO_BAND) ? plot.getFluxUnits() : plot.getFluxUnits(selectedBand));


        // calculate the centroid position
        double xCentroid = xSum / nPixels;
        double yCentroid = ySum / nPixels;
        double xfCentroid = xfSum / fluxSum;
        double yfCentroid = yfSum / fluxSum;

        // calculate the mean and standard deviation
        double averageFlux = fluxSum/nPixels;
        double stdev = Math.sqrt(squareSum/nPixels - averageFlux*averageFlux);

	// calculate integrated flux
	ImageHeader imageHdr = fitsRead.getImageHeader();
	// pixel area in sterradian
	double pixelArea =  Math.abs(imageHdr.cdelt1*DtoR*imageHdr.cdelt2*DtoR);
	if (SUTDebug.isDebug()) System.out.println("pixelArea (STER)="+pixelArea);
	//double integratedFlux = nPixels*pixelArea*fluxSum;
	// Schuyler: avoid double counting the number of pixels, which is already implicitly included in fluxSum
	double integratedFlux = pixelArea*fluxSum;
	String integratedFluxUnits = getIntegratedFluxUnits(fluxUnits);

	if (fluxUnits.equals("mag"))
	{
	    /* exchange max and min */
	    double temp;
	    temp = minimumFlux;
	    minimumFlux = maximumFlux;
	    maximumFlux = temp;
	    temp = minimumX;
	    minimumX = maximumX;
	    maximumX = temp;
	    temp = minimumY;
	    minimumY = maximumY;
	    maximumY = temp;

	    // calculate integrated flux
	    integratedFlux = fluxSum;
	    integratedFluxUnits = "mag";
	}

        HashMap<Metrics, Metric> metrics = new HashMap<Metrics, Metric>(Metrics.values().length);

        ImageWorkSpacePt maxIp = new ImageWorkSpacePt(maximumX, maximumY);
        metrics.put(Metrics.MAX, new Metric("Maximum Flux", maxIp, maximumFlux, fluxUnits));
        ImageWorkSpacePt minIp = new ImageWorkSpacePt(minimumX, minimumY);
        metrics.put(Metrics.MIN, new Metric("Minimum Flux", minIp, minimumFlux, fluxUnits));
        ImageWorkSpacePt centroidIp = new ImageWorkSpacePt(xCentroid, yCentroid);
        metrics.put(Metrics.CENTROID, new Metric("Aperture Centroid", centroidIp, DataConst.NULL_DOUBLE, null));
        ImageWorkSpacePt fwCentroidIp = new ImageWorkSpacePt(xfCentroid, yfCentroid);
        metrics.put(Metrics.FW_CENTROID, new Metric("Flux Weighted Centroid", fwCentroidIp, DataConst.NULL_DOUBLE, null));

        metrics.put(Metrics.MEAN, new Metric("Mean Flux", null, averageFlux, fluxUnits));
        metrics.put(Metrics.STDEV, new Metric("Standard Deviation", null, stdev, fluxUnits));
        metrics.put(Metrics.INTEGRATED_FLUX,  new Metric("Integrated Flux", null, integratedFlux, integratedFluxUnits));

        metrics.put(Metrics.NUM_PIXELS,  new Metric("Number of Pixels", null, nPixels, ""));
        metrics.put(Metrics.PIXEL_AREA, new Metric("Pixel Area", null, pixelArea, "STER"));

        return metrics;
    }

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
