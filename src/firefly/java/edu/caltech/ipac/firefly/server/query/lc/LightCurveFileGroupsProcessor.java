package edu.caltech.ipac.firefly.server.query.lc;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.DataGroup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;


@SearchProcessorImpl(id = "LightCurveFileGroupsProcessor")
public class LightCurveFileGroupsProcessor extends FileGroupsProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        try {
            DownloadRequest dlReq = (DownloadRequest) request;
            TableServerRequest searchReq = dlReq.getSearchRequest();
            Collection<Integer> selectedRows = dlReq.getSelectedRows();

            // temporary mock data instead of using searchReq
            File voRes = new File("/Users/loi/data/p3am_cdd.vot");
            int statusCode = HttpServices.getDataViaUrl(new URL("http://irsa.ipac.caltech.edu/ibe/sia/wise/allwise/p3am_cdd?POS=10.6,40.2&SIZE=10"), voRes);
            if (statusCode != 500) {
                // should be 248 images found.
                DataGroup dataWithUrl = DataGroupReader.readAnyFormat(voRes);
                return computeFileGroup(dataWithUrl, selectedRows);
            }
            return null;
        } catch (Exception e) {
            LOGGER.error(e);
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * givent the results from a search, compute the files to be packaged from the selectedRows.
     * @param data          the tabular data from a search
     * @param selectedRows  selected rows from the data above.  index starts from zero.
     * @return
     * @throws Exception
     */
    private List<FileGroup> computeFileGroup(DataGroup data, Collection<Integer> selectedRows) throws Exception {
        ArrayList<FileInfo> images = new ArrayList<>();
        for (int idx : selectedRows) {
            String url = String.valueOf(data.get(idx).getDataElement("sia_url"));
            images.add(new FileInfo(url, "/3a/img"+idx, 67*1024*1024));
        }
        FileGroup fgroup = new FileGroup(images, new File("/lightcurve_data"), selectedRows.size() * 640 * 1024, "Test lightcurve data");
        return Arrays.asList(fgroup);
    }

}

