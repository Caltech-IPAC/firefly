/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.CropAndCenter;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import edu.caltech.ipac.visualize.plot.plotdata.FlipXY;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.IOException;

/**
 * @author Trey Roby
 * Date: 7/12/18
 */
public class WebPlotPipeline {


    static FitsRead applyPipeline(WebPlotRequest req, FitsRead fr)
                                        throws IOException, FitsException, FailedRequestException, GeomException {
        FitsRead retFr= fr;
        for(WebPlotRequest.Order order : req.getPipelineOrder()) {
            retFr = switch (order) {
                case FLIP_Y -> applyFlipYAxis(req, retFr);
                case FLIP_X -> applyFlipXAxis(req, retFr);
                case ROTATE -> applyRotation(req, retFr);
                case POST_CROP -> applyCrop(req, retFr);
                case POST_CROP_AND_CENTER -> applyCropAndCenter(req, retFr);
            };
        }
        return retFr;
    }

    private static FitsRead applyRotation(WebPlotRequest req, FitsRead fr)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        FitsRead retval= fr;
        if (isRotation(req) && canRotate(fr)) {
            if (req.getRotateNorth()) {
                if (req.getRotateNorthType().equals(CoordinateSys.EQ_J2000)) {
                    retval = FitsReadFactory.createFitsReadNorthUp(fr);
                } else if (req.getRotateNorthType().equals(CoordinateSys.GALACTIC)) {
                    retval = FitsReadFactory.createFitsReadNorthUpGalactic(fr);
                } else {
                    throw new FailedRequestException("Rotation Failed",
                                                     "Fits read failed, rotation type not supported: " +
                                                           req.getRotateNorthType().toString());
                }
            } else if (req.getRotate()) {
                retval = FitsReadFactory.createFitsReadRotated(fr, req.getRotationAngle(), true);
            }
        }
        return retval;

    }

    private static FitsRead applyCrop(WebPlotRequest req, FitsRead fr) throws FitsException, IOException {
        FitsRead retval= fr;
        if (req.getPostCrop()) {
            Pt pt1;
            Pt pt2;
            if (getCropPt1(req) instanceof WorldPt && getCropPt2(req) instanceof WorldPt) {
                ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
                frGroup.setFitsRead(Band.NO_BAND,fr);
                ImagePlot tmpIM = new ImagePlot(frGroup, 0);
                try {
                    pt1 = tmpIM.getImageCoords((WorldPt) getCropPt1(req));
                    pt2 = tmpIM.getImageCoords((WorldPt) getCropPt2(req));
                } catch (ProjectionException e) {
                    pt1 = null;
                    pt2 = null;
                }
            } else {
                pt1 = getCropPt1(req);
                pt2 = getCropPt2(req);
            }
            if (pt1 != null && pt2 != null) {
                Fits inFits = fr.createNewFits();
                Fits cropFits = CropAndCenter.do_crop(inFits, (int) pt1.getX(), (int) pt1.getY(),
                        (int) pt2.getX(), (int) pt2.getY());
                FitsRead[] tmpFr = FitsReadFactory.createFitsReadArray(cropFits);
                retval = tmpFr[0];
            }
        }
        return retval;
    }

    private static FitsRead applyCropAndCenter(WebPlotRequest req, FitsRead fr) throws FitsException {
        FitsRead retval= fr;
        if (req.getPostCropAndCenter()) {
            WorldPt wpt = VisUtil.convert(getCropCenter(req), req.getPostCropAndCenterType());
            double size = getImageSize(req) / 2.;
            retval= CropAndCenter.do_crop(fr, wpt.getLon(), wpt.getLat(), size);
        }
        return retval;
    }

    private static FitsRead applyFlipYAxis(WebPlotRequest req, FitsRead fr) throws  FitsException {
        FitsRead retval= fr;
        if (req.isFlipY()) {
            retval= FitsReadFactory.createFitsReadFlipLR(fr);
        }
        return retval;
    }

    private static FitsRead applyFlipXAxis(WebPlotRequest req, FitsRead fr) throws  FitsException {
        FitsRead retval= fr;
        if (req.isFlipX()) {
            retval= new FlipXY(fr,"xAxis").doFlip();
        }
        return retval;
    }

    private static boolean canRotate(FitsRead fr) {
        int projType = fr.getProjectionType();
        return (projType != Projection.AITOFF &&
                projType != Projection.UNRECOGNIZED &&
                projType != Projection.UNSPECIFIED);
    }

    static boolean isRotation(WebPlotRequest r) {
        return r!=null && ((r.getRotate() && !Double.isNaN(r.getRotationAngle())) || r.getRotateNorth());
    }

    private static Pt getCropPt1(WebPlotRequest r) {
        return r.getCropImagePt1() != null ? r.getCropImagePt1() : r.getCropWorldPt1();
    }

    private static Pt getCropPt2(WebPlotRequest r) {
        return r.getCropImagePt2() != null ? r.getCropImagePt2() : r.getCropWorldPt2();
    }

    private static WorldPt getCropCenter(WebPlotRequest r) {
        Circle c=PlotServUtils.getRequestArea(r);
        return (c!=null) ? c.center() : null;
    }

    private static double getImageSize(WebPlotRequest r) {
        Circle c=PlotServUtils.getRequestArea(r);
        return (c!=null) ? c.radius() : 0.0;
    }
}
