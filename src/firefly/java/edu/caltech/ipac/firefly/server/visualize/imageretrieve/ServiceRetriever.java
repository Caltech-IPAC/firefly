/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.query.ibe.IbeQueryArtifact;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.DssImageParams;
import edu.caltech.ipac.visualize.net.IrsaImageParams;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;
import edu.caltech.ipac.visualize.net.WiseImageParams;
import edu.caltech.ipac.visualize.plot.Circle;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static edu.caltech.ipac.visualize.net.ImageServiceParams.ImageSourceTypes;


public class ServiceRetriever implements FileRetriever {

    public static final String WISE_3A = "3a";


    public FileInfo getFile(WebPlotRequest r) throws FailedRequestException {
        switch (r.getServiceType()) {
            case ISSA: return getIrsaPlot(r, ImageSourceTypes.ISSA, ServiceDesc.get(r));
            case IRIS: return getIrsaPlot(r, ImageSourceTypes.IRIS, ServiceDesc.get(r));
            case TWOMASS: return get2MassPlot(r);
            case MSX: return getIrsaPlot(r, ImageSourceTypes.MSX, ServiceDesc.get(r));
            case DSS: return getDssPlot(r);
            case SDSS: return getSloanDSSPlot(r);
            case WISE: return getWisePlot(r);
            case DSS_OR_IRIS: return getDSSorIris(r);
            default: throw new FailedRequestException("Unsupported Service");
        }
    }

    private FileInfo getSloanDSSPlot(WebPlotRequest request) throws FailedRequestException {
        String bandStr = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        SloanDssImageParams.SDSSBand band;
        try {
            band = Enum.valueOf(SloanDssImageParams.SDSSBand.class,bandStr);
        } catch (Exception e) {
            band= SloanDssImageParams.SDSSBand.r;
        }
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInDegrees = (float)circle.getRadius();
        if (sizeInDegrees > 1) sizeInDegrees = 1F;
        if (sizeInDegrees < .02) sizeInDegrees = .02F;

        SloanDssImageParams params = new SloanDssImageParams();
        params.setBand(band);
        params.setSizeInDeg(sizeInDegrees);
        params.setWorldPt(circle.getCenter());
        FileInfo fi = LockingVisNetwork.retrieve(params);
        fi.setDesc(ServiceDesc.get(request));
        return fi;
    }

    private FileInfo getDssPlot(WebPlotRequest request) throws FailedRequestException {
        String surveyKey = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        return getDssPlot(surveyKey, ServiceDesc.get(request), circle, 15000);
    }


    private FileInfo getDssPlot(String surveyKey, String desc, Circle circle, int timeoutMills) throws FailedRequestException {
        DssImageParams params = new DssImageParams();
        params.setTimeout(timeoutMills); // time out - 15 sec
        params.setWorldPt(circle.getCenter());
        float arcMin = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCMIN, circle.getRadius());
        params.setWidth(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setHeight(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setSurvey(surveyKey);
        FileInfo fi= LockingVisNetwork.retrieve(params);
        fi.setDesc(desc);
        return fi;
    }



    private FileInfo get2MassPlot(WebPlotRequest request) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(request);
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInArcSec = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, circle.getRadius());
        if (sizeInArcSec > 500) sizeInArcSec = 500;
        if (sizeInArcSec < 50) sizeInArcSec = 50;
        circle = new Circle(circle.getCenter(), sizeInArcSec);
        List<RelatedData> rdList= IbeQueryArtifact.get2MassRelatedData(circle.getCenter(), circle.getRadius()+"");
        FileInfo fi = getIrsaPlot(request.getSurveyKey(), circle, ImageSourceTypes.TWOMASS, ServiceDesc.get(request));
        for(RelatedData rd : rdList) fi.addRelatedData(rd);
        return fi;
    }

    private FileInfo getIrsaPlot(WebPlotRequest request,
                                 ImageSourceTypes plotType,
                                 String desc) throws FailedRequestException {
        Circle surveyArea = PlotServUtils.getRequestArea(request);
        return getIrsaPlot(request.getSurveyKey(), surveyArea, plotType, desc);
    }

    private FileInfo getIrsaPlot(String surveyKey,
                                 Circle surveyArea,
                                 ImageSourceTypes plotType,
                                 String desc) throws FailedRequestException {
        IrsaImageParams params = new IrsaImageParams(plotType);
        params.setWorldPt(surveyArea.getCenter());
        params.setBand(surveyKey);
        params.setSize((float) surveyArea.getRadius()); // this is really size not radius, i am just using Circle to hold the params
        FileInfo fi = LockingVisNetwork.retrieve(params);
        fi.setDesc(desc);
        return fi;
    }

    private FileInfo getWisePlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        WiseImageParams params = new WiseImageParams();
        params.setWorldPt(circle.getCenter());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.getRadius());
        FileInfo fi= LockingVisNetwork.retrieve(params);
        List<RelatedData> rdList= IbeQueryArtifact.getWiseRelatedData(circle.getCenter(), circle.getRadius()+"", r.getSurveyBand());
        fi.setDesc(ServiceDesc.get(r));
        fi.addRelatedDataList(rdList);
        return fi;
    }

    private FileInfo getDSSorIris(WebPlotRequest request) throws FailedRequestException {
        FileInfo retval;
        Circle circle = PlotServUtils.getRequestArea(request);

        GetDSSInBackground dss = new GetDSSInBackground(circle, request);
        GetIrisInBackground iris = new GetIrisInBackground( circle, request);

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
        private final AtomicReference<FileInfo> _retFile = new AtomicReference<>(null);
        private final Circle circle;
        private final WebPlotRequest request;

        GetDSSInBackground(Circle circle, WebPlotRequest request) {
            this.circle = circle;
            this.request= request;
        }

        public void run() {
            try {
                _retFile.getAndSet(getDssPlot(request.getSurveyKey(), ServiceDesc.get(request), circle, 3000));
            } catch (Exception e) {
                Logger.warn(e, "Dss background retrieve failed");
            }
        }

        public FileInfo getFile() {
            return _retFile.get();
        }
    }

    private class GetIrisInBackground implements Runnable {
        private final AtomicReference<FileInfo> _retFile = new AtomicReference<>(null);
        private final Circle circle;
        private final WebPlotRequest request;

        GetIrisInBackground(Circle circle, WebPlotRequest request) {
            this.circle = circle;
            this.request= request;
        }

        public void run() {
            try {
                String desc = ServiceDesc.get(request);
                FileInfo fi = getIrsaPlot(request.getSurveyKey(), circle, ImageSourceTypes.IRIS, desc);
                _retFile.getAndSet(fi);
            } catch (Exception e) {
                Logger.warn(e, "IRIS background retrieve failed");
            }
        }

        public FileInfo getFile() {
            return _retFile.get();
        }
    }

}
