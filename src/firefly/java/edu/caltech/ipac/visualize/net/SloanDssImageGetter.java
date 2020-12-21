/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * @author Trey Roby
 * @version $Id: SloanDssImageGetter.java,v 1.4 2012/08/21 21:30:41 roby Exp $
 */
public class SloanDssImageGetter {

    private static final String server = AppProperties.getProperty("sdss.host", "https://cas.sdss.org");
    private static final String cgiapp = "/vo/dr7siap/siap.asmx/getSiapInfo";

    public static FileInfo get(SloanDssImageParams params, File outFile) throws FailedRequestException, IOException {
        try {
            String req = makeSDssRequest(params);
            SloanDssImageParams qParam= params.makeQueryKey();
            File  f= CacheHelper.makeFile(qParam.getUniqueString() + ".xml");
            FileInfo fi= URLDownload.getDataToFile(new URL(req), f);
            if (fi.getResponseCode()!=200) {
                String htmlErr= f.canRead() ? FileUtil.readFile(f) : "Sloan DSS Image Error";
                throw new FailedRequestException( htmlErr, "The SDss server is reporting an error", null);
            }
            DataGroup[] dgAry= VoTableReader.voToDataGroups(f.getAbsolutePath());
            DataGroup dataGroup= dgAry[0];
            if (dataGroup.size() >0) {
                URLDownload.getDataToFile(new URL((String)dataGroup.get(0).getDataElement("url")), outFile);
                return new FileInfo(outFile);
            }
            else {
                throw new FailedRequestException("SDSS: Area not covered",
                        "votable returned not results, probably area is not covered: ");
            }
        } catch (SocketTimeoutException timeOutE) {
            if (outFile.exists() && outFile.canWrite()) outFile.delete();
            throw timeOutE;
        } catch (MalformedURLException me) {
            throw new FailedRequestException( "Invalid URL", "Details in exception", me );
        }

    }

    private static String makeSDssRequest(SloanDssImageParams params) {
        return server + cgiapp +
                "?POS=" + params.getRaJ2000String() + "," + params.getDecJ2000String() +
                "&size=" + params.getSizeInDeg() +
                "&bandpass=" + params.getBand().toString().toLowerCase() +
                "&format=image/fits";
    }


    public static void main(String args[]) {
        SloanDssImageParams params = new SloanDssImageParams();
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
