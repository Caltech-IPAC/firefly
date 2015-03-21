/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Query 2MASS Image Inventory
 * http://irsa.ipac.caltech.edu/applications/2MASS/IM/docs/siahelp.html
 */

@SearchProcessorImpl(id = "2MassQuerySIA", params=
        {@ParamDoc(name="UserTargetWorldPt",                  desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="type", desc="at (Atlas, default) or ql (Quicklook)."),
         @ParamDoc(name="ds", desc="The 2MASS data set to search for images in."),
         @ParamDoc(name="hem", desc="The hemisphere of the 2MASS observatory where the FITS images to return were taken: n or s."),
         @ParamDoc(name="band", desc="Limits 2MASS images returned to those with the given 2MASS Band: A (all), J, H, K."),
         @ParamDoc(name="scan", desc="The nightly scan number (a positive integer) of the FITS images to return.")

        })
public class Query2MassSIA extends QueryVOTABLE  {

    public static final String RADIUS_KEY = "radius";
    public static final String TYPE_KEY = "type";
    public static final String DS_KEY = "ds";
    public static final String HEM_KEY = "hem";
    public static final String BAND_KEY = "band";
    public static final String XDATE_KEY = "xdate";
    public static final String SCAN_KEY = "scan";

    private static final String TM_URL = AppProperties.getProperty("2mass.url.catquery",
                                                      "http://irsa.ipac.caltech.edu/cgi-bin/2MASS/IM/nph-im_sia?FORMAT=image/fits");
    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.IMAGESET;
    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return "2mass-";
    }


    protected String getQueryString(TableServerRequest req) throws DataAccessException {
        WorldPt wpt = req.getWorldPtParam(ServerParams.USER_TARGET_WORLD_PT);
        if (wpt == null)
            throw new DataAccessException("could not find the paramater: " + ServerParams.USER_TARGET_WORLD_PT);
        wpt = Plot.convert(wpt, CoordinateSys.EQ_J2000);

        String ds = req.getParam(DS_KEY);
        if (StringUtils.isEmpty(ds)) { ds = "asky"; }

        String type = req.getParam(TYPE_KEY);
        String hem = req.getParam(HEM_KEY);
        String band = req.getParam(BAND_KEY);
        Date xdate = req.getDateParam(XDATE_KEY);
        int scan = req.getIntParam(SCAN_KEY, -1);


        double radVal = req.getDoubleParam(RADIUS_KEY);
        double radDeg = MathUtil.convert(MathUtil.Units.parse(req.getParam(CatalogRequest.RAD_UNITS), MathUtil.Units.DEGREE), MathUtil.Units.DEGREE, radVal);

        return TM_URL + "&ds=" + ds
                + ((StringUtils.isEmpty(type) || type.equals("at")) ? "" : "&type=" + type)
                + "&SIZE=" + radDeg + "&POS=" + wpt.getLon() + "," + wpt.getLat()
                + ((StringUtils.isEmpty(hem) || hem.equals("a")) ? "" : "&hem=" + hem)
                + ((StringUtils.isEmpty(band) || band.equals("A")) ? "" : "&band=" + band)
                + (xdate == null ? "" : "&xdate="+(new SimpleDateFormat("yyMMdd")).format(xdate))
                + (scan == -1 ? "" : "&scan="+scan)
                ;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        String [] colsToHide = {"name", "download",
                    "naxes", "naxis", "scale", "format",
                "crpix", "crval", "crota2", "pers_art", "glint_art",
                "id", "scntr"
        };

        String relatedCols = "coadd_key";

        // set columns to hide
        for (String c : colsToHide) {
            meta.setAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, c), DataSetParser.VISI_HIDE);
        }

        // set related columns
        meta.setAttribute(MetaConst.DATASET_CONVERTER, "2MASS");
        meta.setAttribute("col.related", relatedCols);

    }

}
