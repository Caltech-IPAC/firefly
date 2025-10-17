/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileCacheHelper;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;

public class SloanDssImageGetter {

    private static final String server = AppProperties.getProperty("sdss.host", "https://skyserver.sdss.org");
    private static final String RELEASE= "17";
    private static final String path = "/vo/dr"+RELEASE+"siap/siap.asmx/getSiapInfo";
    private static final String NOT_COVERED=  String.format("SDSS (dr%s): Area not covered", RELEASE);
    private static final String NOT_COVERED_DETAIL =  "votable returned not results, probably area is not covered: ";
    private static final String SDSS_SRV_ERR= "The SDSS server is reporting a severe error: %s (%d)";

    public static FileInfo get(SloanDssImageParams params, File outFile) throws FailedRequestException, IOException {
        try {
            URL url= new URI(makeSDssRequest(params)).toURL();
            SloanDssImageParams qParam= params.makeQueryKey();
            FileInfo fi= URLDownload.getDataToFile(url, FileCacheHelper.makeFile(qParam.getUniqueString() +"."+FileUtil.XML));
            if (fi.getResponseCode()!=200) {
                var detail= String.format(SDSS_SRV_ERR, fi.getResponseCodeMsg(), fi.getResponseCode());
                throw new FailedRequestException( getHtmlErr(fi.getFile(), detail), detail);
            }
            DataGroup dataGroup= VoTableReader.voToDataGroups(fi.getFile().getPath())[0]; // should only be one but get first
            if (dataGroup.isEmpty()) throw new FailedRequestException(NOT_COVERED, NOT_COVERED_DETAIL);
            URL imageUrl= new URI(dataGroup.get(0).getStringData("url")).toURL();
            return URLDownload.getDataToFile(imageUrl, outFile);
        } catch (SocketTimeoutException timeOutE) {
            if (outFile.canWrite()) outFile.delete();
            throw timeOutE;
        } catch (Exception me) {
            throw new FailedRequestException( "Invalid URL", "Details in exception", me );
        }
    }

    private static String makeSDssRequest(SloanDssImageParams params) {
        return server + path +
                "?POS=" + params.getRaJ2000String() + "," + params.getDecJ2000String() +
                "&size=" + params.getSizeInDeg() +
                "&bandpass=" + params.getBand().toString().toLowerCase() +
                "&format=image/fits";
    }

    private static String getHtmlErr(File f, String detail) throws IOException {
        return f.canRead() ? FileUtil.readFile(f) : detail;
    }

    public static void main(String[] args) {
        SloanDssImageParams params = new SloanDssImageParams("test", "test");
        params.setSizeInDeg(0.1F);
        params.setBand(SloanDssImageParams.SDSSBand.r);
        params.setWorldPt(new WorldPt(10.672, 41.259));
        try {
            get(params, new File("./a.fits.gz"));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}