/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.ptf;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to get metadata from PTF pids using IBE API.
 * Created by ejoliet on 7/19/17.
 */
public class PtfIbeResolver {

    private static final Logger.LoggerImpl log = Logger.getLogger();

    public final static String PTF_IBE_HOST = AppProperties.getProperty("ptf.ibe.host", "https://irsa.ipac.caltech.edu/ibe");
    public static boolean isTestMode = false;

    public PtfIbeResolver() {
    }

    /**
     * Get list of filenames out of the table from PIDs
     *
     * @param pid pids
     * @return string array of file names, column pfilename
     * @throws IOException
     * @throws InterruptedException
     */
    public String[] getListPfilenames(long[] pid) throws IOException, InterruptedException {
        return getValuesFromColumn(pid, "pfilename");
    }


    /**
     * Gets value of the column from PTF PIDs
     *
     * @param pid     pids
     * @param colName string name
     * @return string array values of the column
     * @throws IOException
     * @throws InterruptedException
     */
    public String[] getValuesFromColumn(long pid[], String colName) throws IOException {
        File tempFile = getTempFile();
        try {
            URLConnection aconn = URLDownload.makeConnection(createURL(pid));
            aconn.setRequestProperty("Accept", "*/*");
            URLDownload.getDataToFile(aconn, tempFile);
        } catch (Exception e) {
            log.error(e);
        }


        DataGroup dataObjects = DataGroupReader.readAnyFormat(tempFile);
        Iterator<DataObject> iterator = dataObjects.iterator();
        int size = dataObjects.size();
        List<String> lstFiles = new ArrayList<String>();
        int total = 0;
        while (iterator.hasNext()) {
            DataObject next = iterator.next();
            Object pfilenam1 = next.getDataElement(colName);
            if (pfilenam1 != null) {
                lstFiles.add(pfilenam1.toString());
                total++;
            }
        }

        return lstFiles.toArray(new String[total]);
    }

    private static URL createURL(long[] pid) throws MalformedURLException {
        String url = QueryUtil.makeUrlBase(PTF_IBE_HOST) + "/search/ptf/images/level1?where=pid%20in%20(";
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        for (int i = 0; i < pid.length; i++) {
            sb.append(pid[i]);
            if (i < pid.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return new URL(sb.toString());
    }

    protected File getTempFile() throws IOException {
        File f = File.createTempFile("ptfFiles-", ".tbl", (isTestMode ? new File(".") : ServerContext.getTempWorkDir()));
        if (isTestMode) {
            f.deleteOnExit();
        }
        return f;
    }
}
