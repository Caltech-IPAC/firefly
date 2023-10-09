/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;


/**
 * Dialog for Flux Statistics
 * @author Tatiana Goldina
 * Moved from spot-common AreaStatisticsDialog
 */
public class AreaStatisticsUtil {
    private static final double DtoR = Math.PI/180.0; // degrees to radians conversion factor



    public static HashMap<Metrics, Metric> getAreaStatistics(ActiveFitsReadGroup frGroup, String areaShape, double rotateAngle, Band band,
                                                  ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4) {

        Shape shape;

        if (areaShape.equals("circle")) {
            double x_1 = Math.min(pt1.getX(), pt3.getX());
            double x_2 = Math.max(pt1.getX(), pt3.getX());
            double y_1 = Math.min(pt1.getY(), pt3.getY());
            double y_2 = Math.max(pt1.getY(), pt3.getY());
            if (rotateAngle != 0.0) {
                shape = makeRotatedEllipse(pt1, pt2, pt3);
            } else {
                shape = new Ellipse2D.Double(x_1, y_1, (x_2 - x_1), (y_2 - y_1));
            }

        } else { // rectangle
            GeneralPath genPath = new GeneralPath();
            genPath.moveTo((float) pt1.getX(), (float) pt1.getY());

            genPath.lineTo((float) pt4.getX(), (float) pt4.getY());
            genPath.lineTo((float) pt3.getX(), (float) pt3.getY());
            genPath.lineTo((float) pt2.getX(), (float) pt2.getY());
            genPath.lineTo((float) pt1.getX(), (float) pt1.getY());

            shape = genPath;
        }

        Rectangle2D boundingBox = shape.getBounds2D();

        double minX = boundingBox.getMinX();
        double maxX = boundingBox.getMaxX();
        double minY = boundingBox.getMinY();
        double maxY = boundingBox.getMaxY();

        FitsRead fr= frGroup.getFitsRead(band);
        minX = Math.max(0, minX);
        maxX = Math.min(fr.getNaxis1() - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(fr.getNaxis2() - 1, maxY);

        Rectangle2D.Double newBoundingBox = new Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY));
        //what to do about selected band?
        HashMap<Metrics, Metric> metrics = AreaStatisticsUtil.getStatisticMetrics(
                frGroup, band, shape, newBoundingBox, rotateAngle);
        return metrics;
    }


    // create an ellipse containing the information of ellipse center and length of major and minor axis
    private static Ellipse2D.Double makeRotatedEllipse(ImagePt pt1, ImagePt pt2, ImagePt pt3) {
        double a = Math.sqrt(Math.pow((pt1.getX() - pt2.getX()), 2) + Math.pow((pt1.getY() - pt2.getY()), 2));
        double b = Math.sqrt(Math.pow((pt2.getX() - pt3.getX()), 2) + Math.pow((pt2.getY() - pt3.getY()), 2));
        double x_c = (pt1.getX() + pt3.getX())/2;
        double y_c = (pt1.getY() + pt3.getY())/2;
        return new Ellipse2D.Double(x_c - a/2, y_c - b/2, a, b);
    }


    /**
     * The integrated flux (total flux from the area has different units from the flux
     * For example, if the flux unit (BUNIT in the header is "JY/SR" (Jansky per steradian),
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







    private static boolean rotatedEllipseContains(Ellipse2D.Double shape, double x, double y, double rAngle) {
        double nAngle = Math.PI*2 - rAngle;
        double x_c = shape.getX() + shape.getWidth()/2;
        double y_c = shape.getY() + shape.getHeight()/2;
        double a = shape.getWidth()/2;
        double b = shape.getHeight()/2;


        double xdist = Math.cos(nAngle) * (x - x_c) - Math.sin(nAngle) * (y - y_c);
        double ydist = Math.sin(nAngle) * (x - x_c) + Math.cos(nAngle) * (y - y_c);

        double r = ((xdist*xdist/(a*a)) + (ydist*ydist/(b*b)));

        return (r <= 1);
    }

    /**
     * Get hash map with Area Statistics Metrics
     * @param selectedBand selected color band, one of (ImagePlot.NO_BAND, ImagePlot.RED, ImagePlot.BLUE, ImagePlot.GREEN)
     * @param shape shape that contains region of calculation
     * @param boundingBox if not null, region of calculation will be intersection of shape and boundingBox
     * @return map of calculated metrics by metric id
     */
    public static HashMap<Metrics, Metric> getStatisticMetrics(ActiveFitsReadGroup frGroup,
                                                               Band selectedBand, Shape shape,
                                                               Rectangle2D boundingBox, double rAngle) {

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
        FitsRead fitsRead = frGroup.getFitsRead(selectedBand);
        int imageScaleFactor = fitsRead.getImageScaleFactor();
        if (imageScaleFactor < 1) imageScaleFactor = 1;
        // to calculate the number of pixels (and integratedFlux) correctly in overlay plots,



        /* adjust minX, minY, maxX, maxY to be centered on a pixel */
        ImagePt ippt =  new ImagePt(minX,minY);
        ippt = new ImagePt(
                Math.rint(ippt.getX() + 0.5) -0.5,
                Math.rint(ippt.getY() + 0.5) -0.5);
        minX = ippt.getX();
        minY = ippt.getY();

        ippt = new ImagePt(maxX,maxY);
        ippt = new ImagePt(
                Math.rint(ippt.getX() + 0.5) -0.5,
                Math.rint(ippt.getY() + 0.5) -0.5);
        maxX = ippt.getX();
        maxY = ippt.getY();

        // I need to take into account imageScaleFactor from FitsRead
        FitsRead fr= selectedBand==Band.NO_BAND ? frGroup.getFitsRead(Band.NO_BAND) : frGroup.getFitsRead(selectedBand);
        for (double y=minY; y<=maxY; y+=imageScaleFactor ) {
            for (double x=minX; x<=maxX; x+=imageScaleFactor) {
                if ((rAngle != 0.0 && !rotatedEllipseContains((Ellipse2D.Double)shape, x, y, rAngle)) ||
                        (rAngle == 0.0 && !shape.contains(x, y)))
                    continue;
                flux= fr.getFlux(new ImagePt(x,y));
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
            }
        }
        if (nPixels == 0) {
            HashMap<Metrics, Metric> metrics = new HashMap<>(1);
            metrics.put(Metrics.NUM_PIXELS, new Metric("Number Of Pixels", null, 0, ""));
            return metrics;
        }

        String fluxUnits = fr.getFluxUnits();

        // calculate the centroid position
        double xCentroid = xSum / nPixels;
        double yCentroid = ySum / nPixels;
        double xfCentroid = xfSum / fluxSum;
        double yfCentroid = yfSum / fluxSum;

        // calculate the mean and standard deviation
        double averageFlux = fluxSum/nPixels;
        double stdev = Math.sqrt(squareSum/nPixels - averageFlux*averageFlux);

	// calculate integrated flux
//	ImageHeader imageHdr = fitsRead.getImageHeader();

        ImageHeader imageHdr = new ImageHeader(fitsRead.getHeader());

        // pixel area in steradian
	double pixelArea =  Math.abs(imageHdr.cdelt1*DtoR*imageHdr.cdelt2*DtoR);
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

        HashMap<Metrics, Metric> metrics = new HashMap<>(Metrics.values().length);

        ImageWorkSpacePt maxIp = new ImageWorkSpacePt(maximumX, maximumY);
        metrics.put(Metrics.MAX, new Metric("Maximum Flux", maxIp, maximumFlux, fluxUnits));
        ImageWorkSpacePt minIp = new ImageWorkSpacePt(minimumX, minimumY);
        metrics.put(Metrics.MIN, new Metric("Minimum Flux", minIp, minimumFlux, fluxUnits));
        ImageWorkSpacePt centroidIp = new ImageWorkSpacePt(xCentroid, yCentroid);
        metrics.put(Metrics.CENTROID, new Metric("Aperture Centroid", centroidIp, Metric.NULL_DOUBLE, null));
        ImageWorkSpacePt fwCentroidIp = new ImageWorkSpacePt(xfCentroid, yfCentroid);
        metrics.put(Metrics.FW_CENTROID, new Metric("Flux Weighted Centroid", fwCentroidIp, Metric.NULL_DOUBLE, null));

        metrics.put(Metrics.MEAN, new Metric("Mean Flux", null, averageFlux, fluxUnits));
        metrics.put(Metrics.STDEV, new Metric("Standard Deviation", null, stdev, fluxUnits));
        metrics.put(Metrics.INTEGRATED_FLUX,  new Metric("Integrated Flux", null, integratedFlux, integratedFluxUnits));

        metrics.put(Metrics.NUM_PIXELS,  new Metric("Number of Pixels", null, nPixels, ""));
        metrics.put(Metrics.PIXEL_AREA, new Metric("Pixel Area", null, pixelArea, "STER"));

        return metrics;
    }

    public record Metric(String desc, ImageWorkSpacePt imageWorkSpacePt, double value, String units) {
        public static final double NULL_DOUBLE = Double.NaN;
    }

    public enum Metrics {MAX, MIN, CENTROID, FW_CENTROID, MEAN, STDEV, INTEGRATED_FLUX, NUM_PIXELS, PIXEL_AREA}
}
