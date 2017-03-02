package edu.caltech.ipac.firefly.server.query.lsst;


import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created by zhang on 10/12/16.
 * This search processor is to get LSST images.
 */
@SearchProcessorImpl(id = "LSSTImageSearch")
public class LSSTImageSearch extends URLFileInfoProcessor {
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static String DAX_URL="http://lsst-qserv-dax01.ncsa.illinois.edu:5000/image/v0/";

    /**
     * Implement the abstract method, "getURL"
     * @param sr - ServerRequest
     * @return a URL object
     * @throws MalformedURLException
     */
    @Override
    public URL getURL(ServerRequest sr) throws MalformedURLException {
        String subsize =  sr.getParam("subsize");
        boolean isCutout = subsize != null;

        try {
            if (isCutout) {
                return getURLForCutout(sr);
            } else {
                if (sr.getParam("tract") != null) {
                    return getURLForDeepCoadd(sr);
                } else {
                    return getURLForCCDs(sr);
                }
            }
        }
        catch (Exception e){
            throw new MalformedURLException(e.getMessage());
        }
    }

    /**
     *
     * @param request - ServerRequest which contains imageType, imageId, ra, dec, widthDeg, heightDeg
     * @return URL
     * @throws IOException
     * @throws DataAccessException
     */
    private URL getURLForCutout(ServerRequest request) throws IOException, DataAccessException {
        // ra, dec, and subsize are in degrees
        validateRequiredParams(request, new String[]{"imageType", "imageId", "subsize"});

        String ra = request.getParam("ra");
        String dec = request.getParam("dec");
        if (ra == null || dec == null) {
            String userTargetWorldPt = request.getParam("UserTargetWorldPt");
            if (userTargetWorldPt == null) {
                throw new DataAccessException("No position is specified to get a cutout");
            }
            WorldPt pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = Plot.convert(pt, CoordinateSys.EQ_J2000);
                ra = "" + pt.getLon();
                dec = "" + pt.getLat();
            }
        }

        String imageType = request.getParam("imageType"); // calexp
        if (!imageType.equals("calexp")) {
            throw new DataAccessException("Only calexp cutouts are supported at the moment");
        }
        String imageId = request.getParam("imageId");

        double subsizeArcSec = MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, request.getDoubleParam("subsize"));
        return new URL(DAX_URL+imageType+"/"+imageId+"/cutout?ra="+ra+"&dec="+dec+"&widthAng="+subsizeArcSec+"&heightAng="+subsizeArcSec);
    }

    private void validateRequiredParams(ServerRequest request, String [] requiredParams) throws DataAccessException {
        ArrayList<String> missing = new ArrayList<>();
        for (String p : requiredParams) {
            if (request.getParam(p) == null)
                missing.add(p);
        }
        if (missing.size() > 0) {
            throw new DataAccessException("Required parameters missing: "+String.join(",", missing));
        }
    }

    /**
     * This method is using deepCoaddId to search the image.  It worked fine.  Comment it for now.  If it does not needed,
     * it will be deleted.
     * @param request - ServerRequest
     * @return URL Object
     * @throws IOException
     * @throws DataAccessException
     */
    private URL getURLForDeepCoadd(ServerRequest request) throws IOException, DataAccessException {
        String tract = request.getParam("tract");
        String patch = request.getParam("patch");
        String filterName = request.getParam("filterName");

        logger.info("create URL");
         return  new URL(createURLForDeepCoadd(tract, patch, filterName));
    }


    public static String createURLForDeepCoadd(String tract, String patch, String filterName) throws MalformedURLException {
        return  getBaseURL(true)+"tract="+tract+"&patch="+patch+"&filter="+filterName;
    }
    /**
     * This method uses a set of fields to search for image
     * @param request server request
     * @return URL
     * @throws IOException
     * @throws DataAccessException
     */
    private URL  getURLForCCDs(ServerRequest request)throws IOException, DataAccessException {
        logger.info("getting the parameters out from the request");
        String run = request.getParam("run");
        String camcol = request.getParam("camcol");
        String field = request.getParam("field");
        String filterName = request.getParam("filterName");
        logger.info("create URL");
        return new URL( createURLForScienceCCD(run, camcol,field, filterName) );

    }

    public static String  createURLForScienceCCD(String run, String camcol, String field, String filterName) throws MalformedURLException {
        return getBaseURL(false)+"run="+run+"&camcol="+camcol+"&field="+field+"&filter="+filterName;
    }

    private static  String getBaseURL(boolean isDeepCoadd){
        if (isDeepCoadd) {
            return DAX_URL+"deepCoadd/ids?";

        }
        else {
            return DAX_URL+"calexp/ids?";
        }
    }

}