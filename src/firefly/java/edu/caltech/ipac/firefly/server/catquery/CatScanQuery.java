/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static edu.caltech.ipac.table.TableMeta.VISI_TAG;
import static edu.caltech.ipac.table.TableMeta.makeAttribKey;

/**
 * Date: Nov 11, 2010
 *
 * @author loi
 * @version $Id: CatScanQuery.java,v 1.8 2012/10/08 22:51:25 tatianag Exp $
 */
@SearchProcessorImpl(id ="catScan")
public class CatScanQuery extends IpacTablePartProcessor {
    private static final String DEF_HOST    = AppProperties.getProperty("irsa.gator.hostname", "https://irsa.ipac.caltech.edu");
    private static final String URL_FORMAT  = "%s/cgi-bin/Gator/nph-scan?%smode=ascii";
    private static final String PROJ_PARAM = "projshort=%s&";

    protected File loadDataFile(TableServerRequest req) throws IOException, DataAccessException {

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
            // prepend https://irsa to the description link
            DataGroup data = results.getData();
            for (int i = 0; i <data.size(); i++) {
                DataObject row = data.get(i);
                String desc = String.valueOf(row.getDataElement("description"));
                if (desc.indexOf("href='/data/") >= 0){
                    desc = desc.replaceAll("href='/data/", "href='https://irsa.ipac.caltech.edu/data/");
                    row.setDataElement(data.getDataDefintion("description"), desc);
                }
            }
        }

        super.onComplete(request, results);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        for(String s : new String[]{"identifier", "archive", "notes", "nrec"}) {
            meta.setAttribute(makeAttribKey(VISI_TAG, s), DataType.Visibility.hidden.name());
        }
        super.prepareTableMeta(meta, columns, request);
    }
}
