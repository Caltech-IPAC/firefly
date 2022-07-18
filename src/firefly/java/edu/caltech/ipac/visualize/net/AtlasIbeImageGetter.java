/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class AtlasIbeImageGetter {

    public static FileInfo get(AtlasImageParams params) throws FailedRequestException {

        try {
            Map<String, String> queryMap = new HashMap<>();
            Map<String, String> m = new HashMap<>();
            m.put(AtlasIbeDataSource.BAND_KEY, params.getBand());
            m.put(AtlasIbeDataSource.INSTRUMENT_KEY, params.getInstrument());
            if(params.getDs()!=null){ // using AtlasIbeSource defined datasets
                m.put(AtlasIbeDataSource.DS_KEY, params.getDs());
            }
            else{
                m.put(AtlasIbeDataSource.DATASET_KEY, params.getSchema());
                m.put(AtlasIbeDataSource.TABLE_KEY, params.getTable());
                queryMap.put(AtlasIbeDataSource.XTRA_KEY, params.getXtraFilter());
                queryMap.put(AtlasIbeDataSource.BAND_KEY, params.getBand());
                queryMap.put(AtlasIbeDataSource.INSTRUMENT_KEY, params.getInstrument());
            }

            boolean isCube = params.getDataType() != null && params.getDataType().equalsIgnoreCase("cube"); //TODO Can be also from metadata 'hdu' is it's consistent
            IbeDataSource ibeSource = new AtlasIbeDataSource();
            ibeSource.initialize(m);

            IBE ibe = new IBE(ibeSource);
            IbeQueryParam queryParam = ibeSource.makeQueryParam(queryMap);
            queryParam.setPos(params.getRaJ2000String() + "," + params.getDecJ2000String());
            queryParam.setMcen(true);
            File queryTbl = File.createTempFile("Ibe2Query-", ".tbl", CacheHelper.getDir());
            ibe.query(queryTbl, queryParam);
            DataGroup data = IpacTableReader.read(queryTbl);

            if (data.values().size() == 1) {
                DataObject row = data.get(0);
                Map<String, String> dataMap = IpacTableUtil.asMap(row);
                IbeDataParam dataParam = ibeSource.makeDataParam(dataMap);
                String sizeStr = !Float.isNaN(params.getSize()) ? params.getSize()+"" : null;
                if(sizeStr!=null && !isCube){
                    dataParam.setCutout(true, params.getRaJ2000String() + "," + params.getDecJ2000String(), sizeStr);
                }
                dataParam.setDoZip(true);
                return ibe.getData(dataParam, null);
            } else {
                throw new FailedRequestException("Area not covered "+
                        params.getSchema()+"/"+params.getTable()+"/"+params.getBand());
            }
        } catch (IOException me) {
            throw new FailedRequestException("Could not parse results", "Details in exception", me);
        }
    }
}
