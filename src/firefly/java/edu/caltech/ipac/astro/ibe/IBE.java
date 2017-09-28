/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import static edu.caltech.ipac.astro.ibe.BaseIbeDataSource.addUrlParam;

/**
 * Date: 4/17/14
 *
 * @author loi
 * @version $Id: $
 */
public class IBE {

    public static final String CUT_SIZE = "size";
    public static final String CUT_CENTER = "center";
    public static final String POS = "POS";

    private IbeDataSource ibeDataSource;
    private IbeFileUploader fileUploader;


    public IBE(IbeDataSource ibeDataSource) {
        this.ibeDataSource = ibeDataSource;
    }

    public IbeDataSource getIbeDataSource() {
        return ibeDataSource;
    }

    public void setFileUploader(IbeFileUploader fileUploader) {
        this.fileUploader = fileUploader;
    }

    public void getMetaData(File results) throws IOException {

        String url = ibeDataSource.getMetaDataUrl();
        downloadViaUrlToFile(new URL(url), results);
    }

    public void query(File results, IbeQueryParam param) throws IOException {
        String url = ibeDataSource.getQueryUrl(param);
        downloadViaUrlToFile(new URL(url), results);
    }

    public void multipleQueries(File results, File posFile, IbeQueryParam param) {
        if (fileUploader == null) {
            try {
                fileUploader = (IbeFileUploader) Class.forName("edu.caltech.ipac.firefly.server.query.ibe.IbeFileUploaderImpl").newInstance();
            } catch (Exception e) {
                throw new UnsupportedOperationException("You need an IbeFileUploader to do multiple queries search.");
            }
        }

        String url = ibeDataSource.getSearchUrl();

        Map<String, String> paramMap = ibeDataSource.getMulipleQueryParam(param);
        paramMap.remove(POS);
        try {
            fileUploader.post(results, POS, posFile, new URL(url), paramMap);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("IBE URL is bad.");
        }
    }


    /**
     * this is used to return fits images as well as artifacts.
     *
     * @param param param map
     * @return
     * @throws IOException
     */
    public FileInfo getData(IbeDataParam param, Map<String, String> sourceParams) throws IOException {
        return getData(param, sourceParams, null, null);
    }


    /**
     * this is used to return fits images as well as artifacts.
     *
     * @param param param map
     * @param dl    Download listener
     * @return
     * @throws IOException
     */
    public FileInfo getData(IbeDataParam param, Map<String, String> sourceParams, File dir, DownloadListener dl)
            throws IOException {

        if (ibeDataSource.useFileSystem() && param.getFilePath() == null) {
            throw new IOException("IbeDataParam does not contains the required filepath information.");
        }

        if (!param.isDoCutout() && ibeDataSource.useFileSystem()) {
            File f = createDataFilePath(param);
            if (f != null && f.exists()) {
                return new FileInfo(f);
            }
        }

        URL url = createDataUrl(param);

//        if (results == null) {
//            results = File.createTempFile(param.getFileName(), "", results);
//        } else if (results.isDirectory()) {
//            results = new File(results, param.getFileName());
//        }
        return downloadViaUrl(url, sourceParams, dir, dl);
    }

    public File createDataFilePath(IbeDataParam param) throws IOException {
        File f = new File(ibeDataSource.getBaseFilesystemPath() + "/" + param.getFilePath());
        return f;
    }

    public URL createDataUrl(IbeDataParam param) throws IOException {

        String url = ibeDataSource.getDataUrl(param);

        String qstr = "";
        if (param.isDoCutout()) {
            qstr = addUrlParam(qstr, CUT_CENTER, param.getCenter());
            qstr = addUrlParam(qstr, CUT_SIZE, param.getSize());
            qstr = addUrlParam(qstr, "gzip", param.isDoZip());
        }

        url += (qstr.length() > 0 ? "?" + qstr : "");

        return new URL(url);
    }


    private FileInfo downloadViaUrl(URL url, Map<String, String> sourceParams, File dir, DownloadListener dl)
            throws IOException {
        String progressKey = null;
        String plotId = null;
        try {
            if (sourceParams != null) {
                progressKey = sourceParams.get("ProgressKey");
                plotId = sourceParams.get("plotId");
            }
            Map<String, String> identityCookies = ServerContext.getRequestOwner().getIdentityCookies();

            return URLFileInfoProcessor.retrieveViaURL(url, dir, progressKey, plotId, null, identityCookies);
        } catch (DataAccessException e) {
            throw new IOException(e.getMessage(), e);
        }
    }


    private void downloadViaUrlToFile(URL url, File results) throws IOException {
        downloadViaUrlToFile(url, results, null);
    }

    private void downloadViaUrlToFile(URL url, File results, DownloadListener dl) throws IOException {
        try {
            URLConnection uc = URLDownload.makeConnection(url);
            uc.setRequestProperty("Accept", "text/plain");
            URLDownload.getDataToFile(uc, results, dl);
        } catch (FailedRequestException e) {
            throw new IOException(e.getUserMessage(), e);
        }
    }


    public static String convertUnixToMJD(String unix) {
        if (!StringUtils.isEmpty(unix)) {
            long unixL = Long.parseLong(unix);

            // derived from http://en.wikipedia.org/wiki/Julian_day
            long mdjL = (unixL / 86400000) + 40587;

            return Long.toString(mdjL);

        } else {
            return null;
        }
    }

    public static void main(String[] args) {


        if (true) {
            AtlasIbeDataSource atlas = new AtlasIbeDataSource(AtlasIbeDataSource.DS.SEIP);
            IBE ibe = new IBE(atlas);
            try {
                String basedir = "/hydra/ibetest/atlas";
                // query the metadata
                File results = new File(basedir, "meta.tbl");
                ibe.getMetaData(results);

                // query via position
                results = new File(basedir, "ibe.tbl");
                IbeQueryParam param = new IbeQueryParam("280,-8", ".45");    // m31
//                atlas.makeQueryParam()
                ibe.query(results, param);

                // retrieve all data types with cutout options
                DataGroup data = IpacTableReader.readIpacTable(results, "results");
                for (int idx = 0; idx < data.size(); idx++) {
                    DataObject row = data.get(idx);
                    Map<String, String> dinfo = IpacTableUtil.asMap(row);
                    IbeDataParam dparam = ibe.getIbeDataSource().makeDataParam(dinfo);
                   // dparam.setCutout(true, "10.768479,41.26906", ".1");
                    try {
                        FileInfo fileInfo = ibe.getData(dparam, null, new File(basedir), null);
                        assert fileInfo.getSizeInBytes()>0;
                    } catch (IOException ex) {
                        throw new IOException("Line " + idx + ": Unable to retrieve " + dinfo.get("file_type") + " data.",ex);
                    }
                }

            } catch (IOException | IpacTableException e) {
                e.printStackTrace();
            }
        }

        /*
        // test WISE
        if (false) {
            IBE ibe = new IBE(new WiseIbeDataSource(WiseIbeDataSource.DataProduct.ALLSKY_4BAND_1B));
            try {
                String basedir = "/hydra/ibetest/wise";
                // query the metadata
                File results = new File(basedir, "meta.tbl");
                ibe.getMetaData(results);

                // query via position
                results = new File(basedir, "ibe.tbl");
                IbeQueryParam param = new IbeQueryParam("10.768479,41.26906", ".5");    // m31
                ibe.query(results, param);

                // retrieve all data types with cutout options
                DataGroup data = IpacTableReader.readIpacTable(results, "results");
                for (WiseIbeDataSource.DATA_TYPE dtype : WiseIbeDataSource.DATA_TYPE.values()) {
                    for (int idx = 0; idx < data.size(); idx++) {
                        DataObject row = data.get(idx);
                        Map<String, String> dinfo = IpacTableUtil.asMap(row);
                        dinfo.put(WiseIbeDataSource.FTYPE, dtype.name());
                        IbeDataParam dparam = ibe.getIbeDataSource().makeDataParam(dinfo);
                        dparam.setCutout(true, "10.768479,41.26906", ".1");
                        try {
                            ibe.getData(dparam, null, new File(basedir), null);
                        } catch (IOException ex) {
                            System.out.println("Line " + idx + ": Unable to retrieve " + dtype + " data.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IpacTableException e) {
                e.printStackTrace();
            }
        }

        // test ptf
        if (false) {
            IBE ibe = new IBE(new PtfIbeDataSource(PtfIbeDataSource.DataProduct.LEVEL1));
            try {
                String basedir = "/hydra/ibetest/ptf";
                // query the metadata
                File results = new File(basedir, "meta.tbl");
                ibe.getMetaData(results);

                // query via position
                results = new File(basedir, "ibe.tbl");
                IbeQueryParam param = new IbeQueryParam("148.88822,69.06529", ".5");    // m81
                ibe.query(results, param);

                // retrieve all data types with cutout options
                DataGroup data = IpacTableReader.readIpacTable(results, "ptf");
                for (int idx = 0; idx < data.size(); idx++) {
                    DataObject row = data.get(idx);
                    Map<String, String> dinfo = IpacTableUtil.asMap(row);
                    IbeDataParam dparam = ibe.getIbeDataSource().makeDataParam(dinfo);
                    dparam.setCutout(true, "10.768479,41.26906", ".1");
                    try {
                        ibe.getData(dparam, null, new File(basedir), null);
                    } catch (IOException ex) {
                        System.out.println("Line " + idx + ": Unable to retrieve data.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IpacTableException e) {
                e.printStackTrace();
            }
        }
        */

    }
}
