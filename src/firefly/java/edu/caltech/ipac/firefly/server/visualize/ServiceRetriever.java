/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.DssImageParams;
import edu.caltech.ipac.visualize.net.IrsaImageParams;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;
import edu.caltech.ipac.visualize.net.WiseImageParams;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
public class ServiceRetriever implements FileRetriever {



    public static final String WISE_3A = "3a";


    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        FileData retval;

        switch (request.getServiceType()) {
            case ISSA:
                retval = getIssaPlot(request);
                break;
            case IRIS:
                retval = getIrisPlot(request);
                break;
            case TWOMASS:
                retval = get2MassPlot(request);
                break;
            case DSS_OR_IRIS:
                retval = getDSSorIris(request);
                break;
            case MSX:
                retval = getMsxPlot(request);
                break;
            case DSS:
                retval = getDssPlot(request);
                break;
            case SDSS:
                retval = getSloanDSSPlot(request);
                break;
            case WISE:
                retval = getWisePlotNEW(request);
                break;
            default:
                retval = null;
                Assert.argTst(false, "unsupported service type");
                break;
        }
        return retval;
    }


    private FileData getSloanDSSPlot(WebPlotRequest request) throws FailedRequestException, GeomException {
        String bandStr = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        SloanDssImageParams.SDSSBand band;
        try {
            band = Enum.valueOf(SloanDssImageParams.SDSSBand.class,bandStr);
        } catch (Exception e) {
            band= SloanDssImageParams.SDSSBand.r;
        }
        return getSloanDSSPlot(band, circle);
    }

    private FileData getSloanDSSPlot(SloanDssImageParams.SDSSBand band, Circle circle) throws FailedRequestException, GeomException {
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInDegrees = (float)circle.getRadius();
        if (sizeInDegrees > 1) sizeInDegrees = 1F;
        if (sizeInDegrees < .02) sizeInDegrees = .02F;
        WorldPt wp = Plot.convert(circle.getCenter(), CoordinateSys.EQ_J2000);

        SloanDssImageParams params = new SloanDssImageParams();
        params.setBand(band);
        params.setSizeInDeg(sizeInDegrees);
        params.setRaJ2000(wp.getLon());
        params.setDecJ2000(wp.getLat());
        File f = getNetworkPlot(params);
        String desc = getSloanDssDesc(band);
        return new FileData(f, desc);
    }

    private FileData getDssPlot(WebPlotRequest request) throws FailedRequestException, GeomException {
        String surveyKey = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        return getDssPlot(surveyKey, circle, 15000);
    }


    private FileData getDssPlot(String surveyKey, Circle circle, int timeoutMills) throws FailedRequestException, GeomException {
        DssImageParams params = new DssImageParams();
        WorldPt wp = Plot.convert(circle.getCenter(), CoordinateSys.EQ_J2000);
        params.setTimeout(timeoutMills); // time out - 15 sec
        params.setRaJ2000(wp.getLon());
        params.setDecJ2000(wp.getLat());
        float arcMin = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCMIN, circle.getRadius());
        params.setWidth(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setHeight(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setSurvey(surveyKey);
        return new FileData(getNetworkPlot(params), getDssDesc(surveyKey));
    }





    private FileData get2MassPlot(WebPlotRequest request) throws FailedRequestException, GeomException {
        Circle circle = PlotServUtils.getRequestArea(request);
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInArcSec = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, circle.getRadius());
        if (sizeInArcSec > 500) sizeInArcSec = 500;
        if (sizeInArcSec < 50) sizeInArcSec = 50;
        circle = new Circle(circle.getCenter(), sizeInArcSec);
        String survey = request.getSurveyKey();
        File f = getIrsaPlot(survey, circle, IrsaImageParams.IrsaTypes.TWOMASS);
        String desc = get2MassDesc(survey);
        return new FileData(f, desc);
    }


    private FileData getIrisPlot(WebPlotRequest request) throws FailedRequestException, GeomException {
        String desc = getIrisDesc(request.getSurveyKey());
        File f = getIrsaPlot(request, IrsaImageParams.IrsaTypes.IRIS);
        return new FileData(f, desc);
    }

    private FileData getIssaPlot(WebPlotRequest request) throws FailedRequestException, GeomException {
        String desc = getIssaDesc(request.getSurveyKey());
        File f = getIrsaPlot(request, IrsaImageParams.IrsaTypes.ISSA);
        return new FileData(f, desc);
    }

    private FileData getMsxPlot(WebPlotRequest request) throws FailedRequestException, GeomException {
        try {
            return new FileData(getIrsaPlot(request, IrsaImageParams.IrsaTypes.MSX), "MSX Image");
        } catch (FailedRequestException e) {
            if (e.getUserMessage().contains("does not lie on an image")) {
                throw new FailedRequestException("Location not covered by MSX",
                                                 e.getUserMessage() + " --- " + e.getDetailMessage());
            } else {
                throw e;
            }
        }
    }

    private File getIrsaPlot(WebPlotRequest request,
                             IrsaImageParams.IrsaTypes plotType) throws FailedRequestException,
                                                                        GeomException {
        Circle surveyArea = PlotServUtils.getRequestArea(request);
        return getIrsaPlot(request.getSurveyKey(), surveyArea, plotType);
    }

    private File getIrsaPlot(String surveyKey,
                             Circle surveyArea,
                             IrsaImageParams.IrsaTypes plotType) throws FailedRequestException,
                                                                        GeomException {
        IrsaImageParams params = new IrsaImageParams();
        WorldPt wp = surveyArea.getCenter();
        wp = Plot.convert(wp, CoordinateSys.EQ_J2000);
        params.setRaJ2000(wp.getLon());
        params.setDecJ2000(wp.getLat());
        params.setBand(surveyKey);
        params.setSize((float) surveyArea.getRadius()); // this is really size not radius, i am just using Circle to hold the params
        params.setType(plotType);
        return getNetworkPlot(params);
    }




    private FileData getWisePlotNEW(WebPlotRequest r) throws FailedRequestException, GeomException {
        Circle circle = PlotServUtils.getRequestArea(r);
        WorldPt wp = circle.getCenter();
        wp = Plot.convert(wp, CoordinateSys.EQ_J2000);
        WiseImageParams params = new WiseImageParams();
        params.setRaJ2000(wp.getLon());
        params.setDecJ2000(wp.getLat());
        params.setType(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.getRadius());
        File f= getNetworkPlot(params);
        return new FileData(f,getWiseDesc(r.getSurveyKey(),r.getSurveyBand()));
    }

    /**
     * @param params the network params
     * @return a fits file
     * @throws FailedRequestException if anything goes wrong
     */
    private File getNetworkPlot(BaseNetParams params) throws FailedRequestException {
        try {
            return LockingVisNetwork.getImage(params);
        } catch (SecurityException e) {
            throw new FailedRequestException("Error cause by failed reprojection", "Geom failed", e);
        }
    }

    private FileData getDSSorIris(WebPlotRequest request) throws FailedRequestException {
        FileData retval;
        String dssSurveyKey = request.getSurveyKey();
        String irisSurveyKey = request.getSurveyKeyAlt();
        Circle circle = PlotServUtils.getRequestArea(request);

        GetDSSInBackground dss = new GetDSSInBackground(dssSurveyKey, circle);
        GetIrisInBackground iris = new GetIrisInBackground(irisSurveyKey, circle);

        Thread dssThread = new Thread(dss);
        Thread irisThread = new Thread(iris);
        dssThread.setDaemon(true);
        irisThread.setDaemon(true);
        dssThread.start();
        irisThread.start();
        try {
            dssThread.join(7000);
        } catch (InterruptedException e) {
            // do nothing
        }
        if (dss.getFile() == null) {
            if (iris.getFile() == null) {
                try {
                    irisThread.join(500);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            retval = iris.getFile();
        } else {
            retval = dss.getFile();
        }


        if (retval == null) {
            throw new FailedRequestException("Could not retrieve dss or iris image");
        }
        return retval;
    }


    private class GetDSSInBackground implements Runnable {
        private final AtomicReference<FileData> _retFile = new AtomicReference<FileData>(null);
        private final String _surveyKey;
        private final Circle _circle;

        public GetDSSInBackground(String surveyKey, Circle circle) {
            _surveyKey = surveyKey;
            _circle = circle;
        }

        public void run() {
            try {
                _retFile.getAndSet(getDssPlot(_surveyKey, _circle, 3000));
            } catch (Exception e) {
                Logger.warn(e, "Dss background retrieve failed");
            }
        }

        public FileData getFile() {
            return _retFile.get();
        }
    }

    private class GetIrisInBackground implements Runnable {
        private final AtomicReference<FileData> _retFile = new AtomicReference<FileData>(null);
        private final String _surveyKey;
        private final Circle _circle;

        public GetIrisInBackground(String surveyKey, Circle circle) {
            _surveyKey = surveyKey;
            _circle = circle;
        }

        public void run() {
            try {
                String desc = getIrisDesc(_surveyKey);
                File f = getIrsaPlot(_surveyKey, _circle, IrsaImageParams.IrsaTypes.IRIS);
                _retFile.getAndSet(new FileData(f, desc));
            } catch (Exception e) {
                Logger.warn(e, "IRIS background retrieve failed");

            }
        }

        public FileData getFile() {
            return _retFile.get();
        }
    }

    private static String getDssDesc(String survey) {

        String retval = "DSS ";

        if (survey.equalsIgnoreCase("poss2ukstu_red")) retval += "POSS2/UKSTU Red";
        else if (survey.equalsIgnoreCase("poss2ukstu_ir")) retval += "POSS2/UKSTU Infrared";
        else if (survey.equalsIgnoreCase("poss2ukstu_blue")) retval += "POSS2/UKSTU Blue";
        else if (survey.equalsIgnoreCase("poss1_red")) retval += "POSS1 Red";
        else if (survey.equalsIgnoreCase("poss1_blue")) retval += "POSS1 Blue";
        else if (survey.equalsIgnoreCase("quickv")) retval += "Quick-V Survey";
        else if (survey.equalsIgnoreCase("phase2_gsc2")) retval += "HST Phase 2 Target Positioning(GSC 2)";
        else if (survey.equalsIgnoreCase("phase2_gsc1")) retval += "HST Phase 1 Target Positioning(GSC 1)";

        return retval;
    }


    private static String getSloanDssDesc(SloanDssImageParams.SDSSBand band) {
        return "Band: " + band;

    }



    private static String getWiseDesc(String survey, String band) {
        return "WISE "+survey+ " " + band;
    }

    private static String get2MassDesc(String survey) {

        String retval = "2MASS ";
        if (survey.equalsIgnoreCase("j")) retval += "J";
        else if (survey.equalsIgnoreCase("h")) retval += "H";
        else if (survey.equalsIgnoreCase("k")) retval += "K";

        return retval;
    }

    private static String getIrisDesc(String survey) {

        String retval = "IRAS: ";

        if (survey.equals("12")) retval += "IRIS 12 micron";
        else if (survey.equals("25")) retval += "IRIS 25 micron";
        else if (survey.equals("60")) retval += "IRIS 60 micron";
        else if (survey.equals("100")) retval += "IRIS 100 micron";

        return retval;
    }

    private static String getIssaDesc(String survey) {

        String retval = "ISSA ";

        if (survey.equals("12")) retval += "12 micron";
        else if (survey.equals("25")) retval += "25 micron";
        else if (survey.equals("60")) retval += "60 micron";
        else if (survey.equals("100")) retval += "100 micron";

        return retval;
    }
}
