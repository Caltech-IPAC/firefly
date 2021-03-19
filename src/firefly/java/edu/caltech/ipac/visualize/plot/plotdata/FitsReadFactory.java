/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;

import java.io.IOException;

/**
 * @author Trey Roby
 */
public class FitsReadFactory {

    public static final String NO_IMAGE_HDU_MSG= "FITS file does not contain image data";
    public static final String NO_IMAGE_HDU_ONLY_HEAD= "FITS file contains not image data only a 'Header Only' HDU";
    public static final String NO_IMAGE_HDU_MSG_ONLY_TABLE= "FITS file does not contain image data, only table data";
    public static final String BAD_FORMAT_MSG="Invalid FITS file format";

    /**
     * read a fits with extensions or cube data to create a list of the FistRead object
     *
     * @param fits
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray(Fits fits) throws FitsException {
        return createFitsReadArray(fits.read(),false);
    }

    /**
     *
     * @param HDUs
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray( BasicHDU[] HDUs, boolean clearHdu) throws FitsException {
        if (HDUs == null) throw new FitsException(BAD_FORMAT_MSG);

        BasicHDU [] imageHDUs= FitsReadUtil.getImageHDUArray(HDUs);
        Header zeroHeader= getZeroHeader(HDUs) ;
        confirmHasImageData(imageHDUs,HDUs);
        FitsRead[] fitsReadAry = new FitsRead[imageHDUs.length];
        for (int i = 0; i < imageHDUs.length; i++) fitsReadAry[i] =new FitsRead(imageHDUs[i], zeroHeader, clearHdu);
        return fitsReadAry;
    }

    private static Header getZeroHeader(BasicHDU[] HDUs) {
        if (HDUs.length>1 && (HDUs[0] instanceof ImageHDU) && HDUs[0].getHeader().getIntValue("NAXIS", -1)==0) {
            return HDUs[0].getHeader();
        }
        return null;
    }

    private static void confirmHasImageData(BasicHDU[] imageHDUs, BasicHDU[] HDUs) throws FitsException {
        if (imageHDUs.length == 0) { //The FITS file does not have any Image data
            if (HDUs.length>1) {
                throw new FitsException(NO_IMAGE_HDU_MSG_ONLY_TABLE);
            }
            else {
                boolean onlyZero= HDUs.length==1 && getZeroHeader(HDUs)!=null;
                throw new FitsException((onlyZero) ? NO_IMAGE_HDU_ONLY_HEAD : NO_IMAGE_HDU_MSG);
            }
        }
    }

    /**
     * Flip an image left to right so that pixels read backwards
     *
     * @param aFitsReader FitsRead object for the input image
     * @return FitsRead object for the new, flipped image
     */
    public static FitsRead createFitsReadFlipLR(FitsRead aFitsReader) throws FitsException{
        return new FlipXY(aFitsReader,"yAxis").doFlip();
    }

    /**
     * Rotate an image so that Equatorial North is up in the new image
     *
     * @param fitsReader FitsRead object for the input image
     * @return FitsRead object for the new, rotated image
     */
    public static FitsRead createFitsReadNorthUp(FitsRead fitsReader) throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(fitsReader, 0.0, CoordinateSys.EQ_J2000));
    }

    /**
     * Rotate an image so that Galactic North is up in the new image
     *
     * @param aFitsReader FitsRead object for the input image
     * @return FitsRead object for the new, rotated image
     */
    public static FitsRead createFitsReadNorthUpGalactic(FitsRead aFitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(aFitsReader, 0.0, CoordinateSys.GALACTIC));
    }

    /**
     * Rotate an image by a specified amount
     *
     * @param fitsReader    FitsRead object for the input image
     * @param rotationAngle number of degrees to rotate the image counter-clockwise
     * @param fromNorth if true that the rotation angle is from the north
     * @return FitsRead object for the new, rotated image
     */
    public static FitsRead createFitsReadRotated(FitsRead fitsReader, double rotationAngle, boolean fromNorth)
            throws FitsException, IOException, GeomException {

        ImageHeader imageHeader = new ImageHeader(fitsReader.getHeader());

        CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);
        Projection projection = imageHeader.createProjection(inCoordinateSys);

        double centerX = (imageHeader.naxis1 + 1.0) / 2.0;
        double centerY = (imageHeader.naxis2 + 1.0) / 2.0;

        try {
            WorldPt worldPt1 = projection.getWorldCoords(centerX, centerY - 1);
            WorldPt worldPt2 = projection.getWorldCoords(centerX, centerY);
            double positionAngle = VisUtil.getPositionAngle(worldPt1.getX(),
                    worldPt1.getY(), worldPt2.getX(), worldPt2.getY());
            if (fromNorth) {
                long angleToRotate= Math.round((180+ rotationAngle) % 360);
                if (angleToRotate==Math.round(positionAngle)) {
                    return fitsReader;
                }
                else {
                    return createFitsReadPositionAngle(fitsReader, -angleToRotate, inCoordinateSys);
                }
            }
            else {
                return createFitsReadPositionAngle(fitsReader, -positionAngle+ rotationAngle, inCoordinateSys);
            }
        } catch (ProjectionException pe) {
            if (SUTDebug.isDebug()) {
                System.out.println("got ProjectionException: " + pe.getMessage());
            }
            throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
        }
    }

    public static FitsRead createFitsReadWithGeom(FitsRead aFitsRead,
                                                  FitsRead aRefFitsRead,
                                                  boolean aDoscale) throws FitsException, IOException, GeomException {
        //update the input aFitsRead only if the aRefFitsRead is not null
        if (aRefFitsRead != null) {
            ImageHeader refHeader = new ImageHeader(aRefFitsRead.getHeader());
            Geom geom = new Geom();
            //geom.override_naxis1=0;
            geom.n_override_naxis1 = aDoscale;

            ImageHeader imageHeader = geom.open_in(aFitsRead);
            double primCdelt1 = Math.abs(imageHeader.cdelt1);
            double refCdelt1 = Math.abs(refHeader.cdelt1);
            int imageScaleFactor = 1;
            boolean shouldScale = 2 * refCdelt1 < primCdelt1;
            if (aDoscale && shouldScale) {
                imageScaleFactor = (int) (primCdelt1 / refCdelt1);
                geom.override_cdelt1 = refHeader.cdelt1 * imageScaleFactor;
                geom.n_override_cdelt1 = true;
                geom.override_cdelt2 = refHeader.cdelt2 * imageScaleFactor;
                geom.n_override_cdelt2 = true;
                if (refHeader.using_cd) {
                    geom.override_CD1_1 = refHeader.cd1_1 * imageScaleFactor;
                    geom.override_CD1_2 = refHeader.cd1_2 * imageScaleFactor;
                    geom.override_CD2_1 = refHeader.cd2_1 * imageScaleFactor;
                    geom.override_CD2_2 = refHeader.cd2_2 * imageScaleFactor;
                    geom.n_override_CDmatrix = true;
                }

                geom.crpix1_base = refHeader.crpix1;
                geom.crpix2_base = refHeader.crpix2;
                geom.imageScaleFactor = imageScaleFactor;
                geom.need_crpix_adjusted = true;
                if (SUTDebug.isDebug()) {
                    System.out.println(
                            "RBH ready for do_geom:  imageScaleFactor = "
                                    + imageScaleFactor + "  geom = " + geom);
                }
            }

            //make a copy of the reference  fits
            Fits modFits = geom.do_geom(aRefFitsRead);

            FitsRead[] fitsReadArray = createFitsReadArray(modFits);
            aFitsRead = fitsReadArray[0];

        }
        return aFitsRead;
    }

    /**
     * Rotate an image so that North is at the specified position angle in the new image
     *
     * @param fitsRead      FitsRead object for the input image
     * @param positionAngle desired position angle in degrees
     * @param coordinateSys desired coordinate system for output image
     * @return FitsRead object for the new, rotated image
     */
    public static FitsRead createFitsReadPositionAngle(FitsRead fitsRead, double positionAngle,
                                                       CoordinateSys coordinateSys)
            throws FitsException, IOException, GeomException {

        Geom geom = new Geom();
        Header refHeader = FitsReadUtil.getRefHeader(geom, fitsRead, positionAngle, coordinateSys);

        //create a ImageHDU with the null data
        ImageHDU refHDU = new ImageHDU(refHeader, null);
        Fits refFits = new Fits();
        refFits.addHDU(refHDU);

        refFits = geom.do_geom(refFits);  // throws GeomException
        FitsRead[] fitsReadArray = createFitsReadArray(refFits);
        fitsRead = fitsReadArray[0];
        return fitsRead;
    }
}