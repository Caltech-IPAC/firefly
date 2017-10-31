/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * ex. http://irsa.ipac.caltech.edu/cgi-bin/SSCDemo/nph-sscdemo?dataset=ivo%3A%2F%2Firsa.ipac%2Fspitzer.enhancedImages&region=cone+18.030000+12.240000+0.138889
 * @author tatianag
 */
@SearchProcessorImpl(id = "RTreeInventory", params=
        {@ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="dataset", desc="data set to search for images in.")
        })
public class QueryRTreeInventory extends IpacTablePartProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private static String DATASET_KEY = "dataset";
    private static String SERVICE_URL_KEY = "serviceurl";
    public static final String RADIUS_KEY = "radius";


    @Override
    protected String getFilePrefix(TableServerRequest request) {
        String ds;
        String dataset = request.getParam("dataset");
        if (!StringUtils.isEmpty(dataset)) {
            ds = dataset.replace("ivo://irsa.ipac/","");
            if (!StringUtils.isEmpty(ds)) { ds += "-"; }
        }
        return "rtree-"+"ds";
    }


    protected String getQueryString(TableServerRequest req) throws DataAccessException {
        String serviceurl = req.getParam(SERVICE_URL_KEY);
        if (StringUtils.isEmpty(serviceurl)) {
            throw new DataAccessException("could not find the parameter: " +SERVICE_URL_KEY);
        }


        WorldPt wpt = req.getWorldPtParam(ServerParams.USER_TARGET_WORLD_PT);
        if (wpt == null)
            throw new DataAccessException("could not find the parameter: " + ServerParams.USER_TARGET_WORLD_PT);
        wpt = Plot.convert(wpt, CoordinateSys.EQ_J2000);

        String dataset = req.getParam(DATASET_KEY);
        if (StringUtils.isEmpty(dataset)) {
            throw new DataAccessException("could not find the parameter: " + DATASET_KEY);
        }


        double radVal = req.getDoubleParam(RADIUS_KEY);
        double radDeg = MathUtil.convert(MathUtil.Units.parse(req.getParam(CatalogRequest.RAD_UNITS), MathUtil.Units.DEGREE), MathUtil.Units.DEGREE, radVal);

        return serviceurl + "?dataset=" + dataset + "&region=cone+" + wpt.getLon() + "+" + wpt.getLat() + "+" + radDeg;
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        return doQuery(request);  // all the work is done here
    }

    private File doQuery(TableServerRequest req) throws IOException, DataAccessException {


        URL url;
        String queryString = getQueryString(req);
        try {
            url = new URL(queryString);
        } catch (MalformedURLException e) {
            _log.error(e, e.toString());
            throw new DataAccessException("query failed - bad url: "+queryString, e);
        }
        StringKey cacheKey = new StringKey(url);
        File f = (File) getCache().get(cacheKey);
        if (f != null && f.canRead()) {
            return f;
        } else {


            File outFile = File.createTempFile(getFilePrefix(req), ".tbl", ServerContext.getPermWorkDir());
            try {
            downloadFile(url, outFile);
            } catch (EndUserException e) {
                throw new DataAccessException("download failed from "+queryString, e);
            }
            getCache().put(cacheKey, outFile, 60 * 60 * 24);    // 1 day
            return outFile;
        }
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        String [] colsToHide = {"cntr", "cra", "cdec", "ctype1", "ctype2",
                    "naxis1", "naxis2", "scale", "format",
                "crpix1", "crpix2", "crval1", "crval2", "cdelt1", "cdelt2", "crota2",
                "ra1", "dec1", "ra2", "dec2", "ra3", "dec3", "ra4", "dec4"
        };
        String dataset = request.getParam("RequestedDataSet");
        meta.setAttribute(MetaConst.DATASET_CONVERTER, dataset);

        // set columns to hide
        for (String c : colsToHide) {
            meta.setAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, c), DataSetParser.VISI_HIDE);
        }

    }

}
