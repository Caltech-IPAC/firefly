package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.datasource.PtfIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 4/17/14
 *
 * @author loi
 * @version $Id: $
 */
public class IBE {
    public static final String USER_TARGET_WORLD_PT = "UserTargetWorldPt";
    public static final String POS = "POS";
    public static final String REF_BY = "refby";
    public static final String INTERSECT = "INTERSECT";
    public static final String SIZE = "SIZE";
    public static final String MCEN = "mcen";
    public static final String COLUMNS = "columns";
    public static final String WHERE = "where";
    public static final String CUT_SIZE = "size";
    public static final String CUT_CENTER = "center";

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

        String url = ibeDataSource.getIbeHost() + "/search/" +
                ibeDataSource.getMission() + "/" + ibeDataSource.getDataset() +
                "/" + ibeDataSource.getTableName()+ "?FORMAT=METADATA";
        downloadViaUrl(new URL(url), results);
    }

    public void query(File results, IbeQueryParam param) throws IOException {
        String url = ibeDataSource.getIbeHost() + "/search/" +
                ibeDataSource.getMission() + "/" + ibeDataSource.getDataset() +
                "/" + ibeDataSource.getTableName() + "?" + convertToUrl(param);
        downloadViaUrl(new URL(url), results);
    }

    public void multipleQueries(File results, File posFile, IbeQueryParam param) {
        if (fileUploader == null) {
            try {
                fileUploader = (IbeFileUploader) Class.forName("edu.caltech.ipac.firefly.server.query.ibe.IbeFileUploaderImpl").newInstance();
            } catch (Exception e) {
                throw new UnsupportedOperationException("You need an IbeFileUploader to do multiple queries search.");
            }
        }

        String url = ibeDataSource.getIbeHost() + "/search/" +
                ibeDataSource.getMission() + "/" + ibeDataSource.getDataset() +
                "/" + ibeDataSource.getTableName();

        Map<String, String> paramMap = asMap(param);
        paramMap.remove(POS);
        try {
            fileUploader.post(results, POS, posFile, new URL(url), paramMap);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("IBE URL is bad.");
        }
    }


    /**
     * this is used to return fits images as well as artifacts.
     * @param results The file to send the results to.  If it's a directory, a new file
     *                containing the results will be created there.  If null, the results
     *                will be created in the temp directory.
     * @param param param map
     * @return
     * @throws IOException
     */
    public File getData(File results, IbeDataParam param) throws IOException {
        return getData(results, param, null);
    }


    /**
     * this is used to return fits images as well as artifacts.
     * @param results The file to send the results to.  If it's a directory, a new file
     *                containing the results will be created there.  If null, the results
     *                will be created in the temp directory.
     * @param param param map
     * @param dl Download listener
     * @return
     * @throws IOException
     */
    public File getData(File results, IbeDataParam param, DownloadListener dl) throws IOException {

        if (param.getFilePath() == null) {
            throw new IOException("IbeDataParam does not contains the required filepath information.");
        }

        if (!param.isDoCutout() && ibeDataSource.useFileSystem()) {
            File f = createDataFilePath(param);
            if (f != null && f.exists()) {
                return f;
            }
        }

        URL url = createDataUrl(param);

        if (results == null) {
            results = File.createTempFile(param.getFileName(), "", results);
        } else if (results.isDirectory()) {
            results = new File(results, param.getFileName());
        }
        downloadViaUrl(url, results,dl);
        return results;
    }

    public File createDataFilePath(IbeDataParam param) throws IOException {
        File f = new File(ibeDataSource.getBaseFilesystemPath() + "/" + param.getFilePath());
        return f;
    }

    public URL createDataUrl(IbeDataParam param) throws IOException {

        String fpath = param.getFilePath();

        String url = ibeDataSource.getIbeHost() + "/data/" +
                ibeDataSource.getMission() + "/" + ibeDataSource.getDataset() +
                "/" + ibeDataSource.getTableName() + "/" + fpath;
        String qstr = "";
        if (param.isDoCutout()) {
            qstr= addUrlParam(qstr, CUT_CENTER, param.getCenter());
            qstr= addUrlParam(qstr, CUT_SIZE, param.getSize());
            qstr= addUrlParam(qstr, "gzip", param.isDoZip());
        }

        url = url + (qstr.length() > 0 ? "?" + qstr : "");
        return new URL(url);
    }

    private void downloadViaUrl(URL url, File results) throws IOException {
        downloadViaUrl(url,results,null);
    }

    private void downloadViaUrl(URL url, File results, DownloadListener dl) throws IOException {
        try {
            URLConnection uc = URLDownload.makeConnection(url);
            uc.setRequestProperty("Accept", "text/plain");
            URLDownload.getDataToFile(uc, results, dl);
        } catch (FailedRequestException e) {
            throw new IOException("Request Failed", e);
        }
    }

    private Map<String, String> asMap(IbeQueryParam param) {
        HashMap<String, String> params = new HashMap<String, String>();
        String s = convertToUrl(param);
        String[] pp = s.split("&");
        for (String keyval : pp) {
            if (!StringUtils.isEmpty(keyval)) {
                String[] parts = keyval.split("=", 2);
                if (!StringUtils.isEmpty(parts[0])) {
                    String v = parts.length > 1 && !StringUtils.isEmpty(parts[1]) ? parts[1].trim() : "";
                    params.put(parts[0], v);
                }
            }
        }
        return params;
    }


    private String convertToUrl(IbeQueryParam param) {
        String s = "";
        if (param == null) return "";

        if (!StringUtils.isEmpty(param.getRefBy())) {
            s =addUrlParam(s, REF_BY, param.getRefBy());
        } else if (!StringUtils.isEmpty(param.getPos())) {
            s = addUrlParam(s, POS, param.getPos());
            s = addUrlParam(s, INTERSECT, param.getIntersect());
            if (param.isMcen()) {
                s = addUrlParam(s, null, MCEN);
            } else {
                s = addUrlParam(s, SIZE, param.getSize());
            }
        }

        s = addUrlParam(s, COLUMNS, param.getColumns());
        s = addUrlParam(s, WHERE, param.getWhere(), true);
        return s;
    }

    public static String addUrlParam(String url, String key, Object value) {
        return addUrlParam(url, key, value, false);
    }

    public static String addUrlParam(String url, String key, Object value, boolean doEncode) {
        try {
            if (!StringUtils.isEmpty(value)) {
                if (!StringUtils.isEmpty(url)) {
                    url = url + "&";
                }
                value = doEncode ? URLEncoder.encode(value.toString(), "UTF-8") : value;
                key = StringUtils.isEmpty(key) ? "" : key + "=";
                url = url + key + value;
            }
        } catch (UnsupportedEncodingException e) {
        }
        return url;
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
                            ibe.getData(new File(basedir), dparam);
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
        if (true) {
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
                        ibe.getData(new File(basedir), dparam);
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

    }
}
