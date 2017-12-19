/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.util.BufferedDataOutputStream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class ProjectionTest {

    public static void main(String[] args) throws FitsException, IOException {

        try {
            String inFitsName = "/Users/ejoliet/tools/samples/TPV/PTF-image.fits";//"/Users/ejoliet/tools/samples/TPV/chip.fits";// "/Users/ejoliet/tools/samples/Tickets/1009-ztf-sample.fits";///args[0];
            Fits fits = new Fits(inFitsName);
            FitsRead[] fitsReadArray = FitsRead.createFitsReadArray(fits);

            System.out.println(fitsReadArray.length);

            FitsRead reader = fitsReadArray[0];
            CoordinateSys cs = CoordinateSys.EQ_J2000;
            ImageHeader imageHeader = reader.getImageHeader();

            WorldPt worldPt = getCenterWorld(imageHeader, cs);

            System.out.println(worldPt.toString());

            Projection proj = imageHeader.createProjection(cs);
            double centerX = (imageHeader.naxis1 + 1.0) / 2.0 - 1;
            double centerY = (imageHeader.naxis2 + 1.0) / 2.0 - 1;

            ProjectionPt imageCoords = proj.getImageCoords(worldPt.getLon(), worldPt.getLat());

            Assert.argTst(Math.abs(imageCoords.getX() - centerX) < 1, "wrong");

            Assert.argTst(Math.abs(imageCoords.getY() - centerY) < 1, "wrong");

            WorldPt worldCoords = proj.getWorldCoords(imageCoords.getX(), imageCoords.getY());

            Assert.argTst(worldPt.getLon() == worldCoords.getLon(), "wrong");

            Assert.argTst(worldPt.getLat() == worldCoords.getLat(), "wrong");
        } catch (ProjectionException e) {
            e.printStackTrace();
        }

    }

    private static WorldPt getCenterWorld(ImageHeader imageHeader, CoordinateSys cs) throws FitsException {
        Projection proj = imageHeader.createProjection(cs);


        double centerX = (imageHeader.naxis1 + 1.0) / 2.0;
        double centerY = (imageHeader.naxis2 + 1.0) / 2.0;

        WorldPt worldPt;
        try {
            worldPt = proj.getWorldCoords(centerX - 1, centerY - 1);

        } catch (ProjectionException pe) {
                System.out.println("got ProjectionException: " + pe.getMessage());
            throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
        }
        return worldPt;
    }
}
