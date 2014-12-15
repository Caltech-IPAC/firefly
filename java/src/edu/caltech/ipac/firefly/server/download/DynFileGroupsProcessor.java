package edu.caltech.ipac.firefly.server.download;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SearchProcessorImpl(id = "defaultDownload")
public class DynFileGroupsProcessor extends FileGroupsProcessor {

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }

    private String getUrlParamValue(String url, String param) {
        if (param == null || param.length() == 0)
            return null;

        // remove any '#' and the '?' to prevent interference when getting values
        String[] params = url.split("#")[0].split("\\?")[1].split("&");

        // search through the params array until we reach the end or we find a match
        for (int i = 0; i < params.length; i++) {
            String[] keyValArr = params[i].split("=");
            if (keyValArr[0].equals(param))
                return keyValArr[1];
        }

        return null;
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        String urlDownloadColumn = request.getParam("URL_DOWNLOAD_COLUMN");
        if (urlDownloadColumn == null) {
            urlDownloadColumn = "download";
        }

        String urlParamFilename = request.getParam("URL_PARAM_FILENAME");
        if (urlParamFilename == null) {
            urlParamFilename = "name";
        }

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, urlDownloadColumn);

        Logger.LoggerImpl logger = Logger.getLogger();

        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        long fgSize = 0;

        for (int rowIdx : selectedRows) {
            String dataURL = (String) dgData.get(rowIdx, urlDownloadColumn);
            String filename = getUrlParamValue(dataURL, urlParamFilename);
            File outFile = new File(ServerContext.getTempWorkDir() + "/" + filename);

            // check for cached version
            if (!outFile.exists()) {
                logger.info("File URL: " + dataURL);

                URL url = new URL(dataURL);

                try {
                    URLDownload.getDataToFile(url, outFile);
                } catch (Exception e) {
                    logger.error("ERROR: " + e.getMessage());
                }
            }

            FileInfo fi = new FileInfo(filename, filename, outFile.length());
            fiArr.add(fi);
            fgSize += outFile.length();
        }

        FileGroup fg = new FileGroup(fiArr, ServerContext.getTempWorkDir(), fgSize, "Hydra Download Files");
        fgArr.add(fg);

        return fgArr;
    }
}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
