package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.servlets.HiPSRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.AppProperties;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;

/**
 * Created by cwang on 2/27/18.
 */
public class IrsaHiPSListSource implements HiPSMasterListSourceType {

    private static final String irsaBaseUrl= AppProperties.getProperty("irsa.base.url","https://irsa.ipac.caltech.edu");
    private static final String irsaAddMocPath = AppProperties.getProperty("irsa.moc.list.path","/irsadata/hips/irsa_moc.list");
    static final Logger.LoggerImpl log = Logger.getLogger();

    private static final String irsaHipsListUrl = AppProperties.getProperty(
            "irsa.hips.masterUrl",
            "https://irsa.ipac.caltech.edu/data/hips/list");

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            if (!Arrays.asList(dataTypes).contains(ServerParams.IMAGE)) return null;
            return HiPSListUtil.createHiPSListFromUrl(irsaHipsListUrl, source, false, null);
        }
        catch (Exception e) {
            HiPSListUtil.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }
    }

    public List<HiPSMasterListEntry> getAdditionalMOCS(String source) {
        try {
            URL url= new URI(getMocUrl()).toURL();
            FileInfo listFileInfo = HiPSRetrieve.retrieveHiPSData(url.toString(), source, false);
            if (listFileInfo.getResponseCode()!=200 && listFileInfo.getResponseCode()!=304) return null;
            DataGroup dg= TableUtil.readAnyFormat(listFileInfo.getFile());
            var retList = new ArrayList<HiPSMasterListEntry>();

            for(var i=0; i<dg.size();i++) {
                DataObject row= dg.get(i);
                HiPSMasterListEntry entry = new HiPSMasterListEntry();
                setV(entry,row, PARAMS.FRACTION, "coverage");
                setV(entry,row, PARAMS.WAVELENGTH, "waveband");
                setV(entry,row, PARAMS.TITLE, "title");
                setV(entry,row, PARAMS.URL, "moc_url");
                setV(entry,row, PARAMS.IVOID, "moc_url");
                setV(entry,row, PARAMS.ORDER, "order");
                setV(entry,row, PARAMS.RELEASEDATE, "release");
                setV(entry,row, PARAMS.PROPERTIES, "info_url");
                entry.set(PARAMS.SOURCE.getKey(),source);

                retList.add(entry);
            }
            return retList;
        } catch (Exception e) {
            log.error(e, "cannot find additional MOCS for " + source);
            return null;
        }
    }

    public void setV(HiPSMasterListEntry entry, DataObject row, HiPSMasterListEntry.PARAMS key, String col) {
        Object obj= row.getDataElement(col);
        if (obj!=null) entry.set(key.getKey(), obj.toString());
    }

    public String getUrl() { return irsaHipsListUrl; }
    public String getMocUrl() {
        if (irsaAddMocPath.startsWith("https")) return irsaAddMocPath;
        else return irsaBaseUrl+irsaAddMocPath;
    }
}
