package edu.caltech.ipac.firefly.server.query.inventory;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.GROUPBY_COLS_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.LABEL_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_INVISIBLE;
import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * Date: Nov 11, 2010
 *
 * @author loi
 * @version $Id: InventorySummary.java,v 1.5 2012/08/21 21:31:12 roby Exp $
 */
@SearchProcessorImpl(id ="inventorySummary")
public class InventorySummary extends DynQueryProcessor {
    private static final String DEF_HOST    = AppProperties.getProperty("irsa.inventory.hostname","irsa.ipac.caltech.edu" );
    private static final String URL_FORMAT  = "http://%s/cgi-bin/SSCDemo/nph-sscdemo?action=list&region=cone+%+.6f+%+.6f+%+.6f";

    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        float radius = req.getFloatParam("InventorySearch.radius");
        WorldPt pt= req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        pt= VisUtil.convertToJ2000(pt);
        if (radius == Float.NaN || pt==null) {
            throw new DataAccessException(
                    new EndUserException("IRSA search failed, inventory search is unavailable",
                               "Search Processor did not find the required parameter(s)"));
        }

        URL url = new URL(String.format(URL_FORMAT, DEF_HOST, pt.getLon(), pt.getLat(), radius) );

        try {
            File outf = createFile(req);
            URLDownload.getDataToFile(url, outf, false);

            DataGroup dg = DataGroupReader.read(outf);
            DataType sn = new DataType("sname", String.class);
            dg.addDataDefinition(sn);
            for (Iterator<DataObject> itr = dg.iterator(); itr.hasNext(); ) {
                DataObject row = itr.next();
                String[] ids = String.valueOf(row.getDataElement("identifier")).split("\\.");
                row.setDataElement(sn, ids[ids.length-1]);
            }
            SortInfo si = new SortInfo("datatype");
            QueryUtil.doSort(dg, si);
            DataGroupWriter.write(outf, dg, req.getPageSize());

            return outf;
        } catch (FailedRequestException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        for(String s : new String[]{"identifier", "archive", "notes", "nrec", "sname"}) {
            meta.setAttribute(makeAttribKey(VISI_TAG, s), VISI_INVISIBLE);
        }

        meta.setAttribute(makeAttribKey(LABEL_TAG, "datatype"), "Type");
        meta.setAttribute(makeAttribKey(LABEL_TAG, "set"), "Dataset");
        meta.setAttribute(makeAttribKey(LABEL_TAG, "description"), "Description");
        meta.setAttribute(makeAttribKey(LABEL_TAG, "count"), "Count");

        meta.setAttribute(GROUPBY_COLS_TAG, "datatype, set");
        super.prepareTableMeta(meta, columns, request);
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
