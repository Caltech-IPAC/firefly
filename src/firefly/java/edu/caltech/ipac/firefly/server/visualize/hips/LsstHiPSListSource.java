package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.servlets.HiPSRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.download.URLDownload;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LsstHiPSListSource implements HiPSMasterListSourceType {

    private static final String lsstHipsListUrl = AppProperties.getProperty(
            "lsst.hips.masterUrl",
            "https://irsa.ipac.caltech.edu/data/hips/list");

    private static final String lsstMocListUrl = AppProperties.getProperty( "lsst.moc.masterUrl", null);
    static final Logger.LoggerImpl log = Logger.getLogger();


    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            if (!Arrays.asList(dataTypes).contains(ServerParams.IMAGE)) return null;
            return HiPSListUtil.createHiPSListFromUrl(lsstHipsListUrl, source, false, null);
        }
        catch (Exception e) {
            HiPSListUtil.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }
    }

    public List<HiPSMasterListEntry> getAdditionalMOCS(String source) {
        try {
            if (lsstMocListUrl==null) return null;
            URL url= URLDownload.makeURL(lsstMocListUrl);
            if  (url == null) return null;
            FileInfo listFileInfo = HiPSRetrieve.retrieveHiPSData(url.toString(), source, false);
            if (listFileInfo.getResponseCode()!=200 && listFileInfo.getResponseCode()!=304) return null;
            DataGroup dg= TableUtil.readAnyFormat(listFileInfo.getFile());
            if (dg == null || dg.isEmpty()) return null;
            var retList = new ArrayList<HiPSMasterListEntry>();
            String baseUrl= null;
            if (url.getQuery()==null) {
                String s= lsstMocListUrl.endsWith("/") ? lsstMocListUrl.substring(0,lsstMocListUrl.length()-1) : lsstMocListUrl;
                var endIdx= s.lastIndexOf("/");
                baseUrl= endIdx>-1  ? s.substring(0, s.lastIndexOf("/")) : s;
            }

            for(var i=0; i<dg.size();i++) {
                DataObject row= dg.get(i);
                HiPSMasterListEntry entry = new HiPSMasterListEntry();
                setV(entry,row, HiPSMasterListEntry.PARAMS.FRACTION, "coverage");
                setV(entry,row, HiPSMasterListEntry.PARAMS.WAVELENGTH, "waveband");
                setV(entry,row, HiPSMasterListEntry.PARAMS.TITLE, "title");
                setV(entry,row, HiPSMasterListEntry.PARAMS.ORDER, "order");
                setV(entry,row, HiPSMasterListEntry.PARAMS.RELEASEDATE, "release");
                setV(entry,row, HiPSMasterListEntry.PARAMS.PROPERTIES, "info_url");
                entry.set(HiPSMasterListEntry.PARAMS.SOURCE.getKey(),source);

                var mocUrlStrPath= (String)row.getDataElement("moc_url");
                if (mocUrlStrPath!=null) {
                    var mocUrlStr= (mocUrlStrPath.toLowerCase().startsWith("http") || baseUrl==null)
                            ? mocUrlStrPath
                            : baseUrl+"/"+mocUrlStrPath;
                    entry.set(HiPSMasterListEntry.PARAMS.URL.getKey(), mocUrlStr);
                    entry.set(HiPSMasterListEntry.PARAMS.IVOID.getKey(), mocUrlStr);
                }

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



    public String getUrl() { return lsstHipsListUrl; }
}
