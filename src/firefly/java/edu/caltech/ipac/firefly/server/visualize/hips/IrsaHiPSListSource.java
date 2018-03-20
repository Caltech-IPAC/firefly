package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import org.apache.commons.csv.CSVFormat;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cwang on 2/27/18.
 */
public class IrsaHiPSListSource implements HiPSMasterListSourceType {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private final static String irsaHipsTable = "/edu/caltech/ipac/firefly/resources/irsa-hips-master-table.csv";
    private static final String IRSA_HIPS_TABLE = AppProperties.getProperty("irsa.hipsmastertable.location",irsaHipsTable);


    private static Map<String, String> paramsMap = new HashMap<>();
    static {
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TITLE, "label");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.URL, "url");
    }

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            return createHiPSList(IRSA_HIPS_TABLE, dataTypes, source);
        }
        catch (FailedRequestException | IOException e) {
            _log.warn("get Irsa HiPS failed");
            return null;
        } catch (Exception e) {
            _log.warn(e.getMessage());
            return null;
        }

    }

    // a csv file is created to contain HiPS from IRSA
    private List<HiPSMasterListEntry> createHiPSList(String hipsMaser,
                                                     String[] dataTypes,
                                                     String source) throws IOException, FailedRequestException {

        InputStream inf= IrsaHiPSListSource.class.getResourceAsStream(hipsMaser);
        DataGroup dg = DsvToDataGroup.parse(inf, CSVFormat.DEFAULT);

        return getListData(dg, paramsMap, source);
    }

    private List<HiPSMasterListEntry> getListData(DataGroup hipsDg,
                                                  Map<String, String> keyMap, String source) {

        List<DataObject> dataRows = hipsDg.values();
        DataType[]   dataCols = hipsDg.getDataDefinitions();
        String[]     cols = new String[dataCols.length];

        for (int i = 0; i < dataCols.length; i++) {
            String colName = dataCols[i].getKeyName();
            for (Map.Entry<String, String> entry: keyMap.entrySet()) {
                if (colName.equals(entry.getValue())) {
                    cols[i] = entry.getKey();
                    break;
                }
            }
        }

        List<HiPSMasterListEntry> lists = new ArrayList<>();
        HiPSMasterListEntry oneList;

        for (DataObject row : dataRows) {
            oneList = new HiPSMasterListEntry();
            lists.add(oneList);
            oneList.set(PARAMS.TYPE.getKey(), ServerParams.IMAGE);
            oneList.set(PARAMS.SOURCE.getKey(), source);
            for (int i = 0; i < dataCols.length; i++) {
                Object obj = row.getDataElement(dataCols[i].getKeyName());
                String val = obj != null ? obj.toString() : null;

                oneList.set(cols[i], val);
            }
        }
        
        return lists;
    }
}

