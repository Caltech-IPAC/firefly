package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_HIDDEN;
import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * Date: Nov 11, 2010
 *
 * @author loi
 * @version $Id: CatScanQuery.java,v 1.8 2012/10/08 22:51:25 tatianag Exp $
 */
@SearchProcessorImpl(id ="catScan")
public class CatScanQuery extends DynQueryProcessor {
    private static final String DEF_HOST    = AppProperties.getProperty("irsa.gator.hostname", "irsa.ipac.caltech.edu");
    private static final String URL_FORMAT  = "http://%s/cgi-bin/Gator/nph-scan?%smode=ascii";
    private static final String PROJ_PARAM = "projshort=%s&";

    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        String proj = req.getParam("projshort");
        String projParam = StringUtils.isEmpty(proj) ? "" : String.format(PROJ_PARAM, proj);
        URL url = new URL(String.format(URL_FORMAT, DEF_HOST, projParam));

        try {
            File outf = createFile(req);
            URLDownload.getDataToFile(url, outf);
            return outf;
        } catch (FailedRequestException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {

        if (results != null && results.getData() != null) {
            // prepend http://irsa to the description link
            DataGroup data = results.getData();
            for (int i = 0; i <data.size(); i++) {
                DataObject row = data.get(i);
                String desc = String.valueOf(row.getDataElement("description"));
                if (desc.indexOf("href='/data/") >= 0){
                    desc = desc.replaceAll("href='/data/", "href='http://irsa.ipac.caltech.edu/data/");
                    row.setDataElement(data.getDataDefintion("description"), desc);
                }
            }
        }

        super.onComplete(request, results);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        for(String s : new String[]{"identifier", "archive", "notes", "nrec"}) {
            meta.setAttribute(makeAttribKey(VISI_TAG, s), VISI_HIDDEN);
        }
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

