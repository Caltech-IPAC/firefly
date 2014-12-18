package edu.caltech.ipac.firefly.server.query.inventory;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.net.URLParms;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.DESC_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.LABEL_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * Date: Nov 11, 2010
 *
 * @author loi
 * @version $Id: InventorySearch.java,v 1.10 2012/08/21 21:31:12 roby Exp $
 */
@SearchProcessorImpl(id ="inventorySearch")
public class InventorySearch extends DynQueryProcessor {
    private static final String DEF_HOST    = AppProperties.getProperty("irsa.inventory.hostname","irsa.ipac.caltech.edu" );
    private static final String URL_FORMAT  = "http://%s/cgi-bin/SSCDemo/nph-sscdemo?dataset=%s&region=cone+%+.6f+%+.6f+%+.6f";
    private static final ClassProperties COL_META = new ClassProperties("IS", InventorySearch.class);

    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        String dataset = req.getParam("identifier");
        float radius = req.getFloatParam("InventorySearch.radius");
        WorldPt pt= req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        pt= VisUtil.convertToJ2000(pt);
        return queryInventoryService(DEF_HOST, dataset, pt, radius, createFile(req));
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);

        String sname = request.getParam("sname");
        String datatype = request.getParam("datatype");

        if (sname != null) {
            String base = datatype.equals("images") ? "IMAGES" : sname;
            if (COL_META.getItems(base) != null) {
                for(String s : COL_META.getItems(base)) {
                    String prop = base+"."+s;
                    String label = COL_META.getTitle(prop);
                    String tips = COL_META.getTip(prop);
                    String visi = COL_META.getDefault(prop);

                    label = label.startsWith(prop) ? s : label;
                    tips = tips.startsWith(prop) ? s : tips;
                    visi = visi.startsWith(prop) ? "show" : visi;

                    meta.setAttribute(makeAttribKey(LABEL_TAG, s), label);
                    meta.setAttribute(makeAttribKey(DESC_TAG, s), tips);
                    meta.setAttribute(makeAttribKey(VISI_TAG, s), visi);
                }
            }
        }

        if (datatype == null) return;

        if (datatype.equals("spectra")) {
            TableMeta.LonLatColumns col= new TableMeta.LonLatColumns("ra", "dec", CoordinateSys.EQ_J2000);
            meta.setCenterCoordColumns(col);

        } else if (datatype.equals("images")) {

            TableMeta.LonLatColumns col= new TableMeta.LonLatColumns("ra", "dec", CoordinateSys.EQ_J2000);
            meta.setCenterCoordColumns(col);

            TableMeta.LonLatColumns c1= new TableMeta.LonLatColumns("ra1", "dec1", CoordinateSys.EQ_J2000);
            TableMeta.LonLatColumns c2= new TableMeta.LonLatColumns("ra2", "dec2", CoordinateSys.EQ_J2000);
            TableMeta.LonLatColumns c3= new TableMeta.LonLatColumns("ra3", "dec3", CoordinateSys.EQ_J2000);
            TableMeta.LonLatColumns c4= new TableMeta.LonLatColumns("ra4", "dec4", CoordinateSys.EQ_J2000);
            meta.setCorners(c1, c2, c3, c4);
        }
    }

    public static File queryInventoryService(String host, String dataset, WorldPt pos, float radius, File outf) throws IOException, DataAccessException {

        pos =  Plot.convert(pos, CoordinateSys.EQ_J2000);
        if (pos == null || dataset == null || host == null) {
            throw new DataAccessException(
                    new EndUserException("IRSA search failed, inventory is unavailable",
                               "Search Processor did not find the required parameter(s)"));
        }

        URL url = new URL( String.format(URL_FORMAT, host, URLParms.encode(dataset), pos.getLon(), pos.getLat(), radius));
        try {
            if (outf == null) {
                outf =  File.createTempFile("inventorySearch", ".tbl", ServerContext.getPermWorkDir());
            }
            URLDownload.getDataToFile(url, outf, false);
            return outf;
        } catch (FailedRequestException e) {
            throw new IOException(e);
        }

    }


}

