/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 * @version $Id: DssImageGetter.java,v 1.9 2012/08/21 21:30:41 roby Exp $
 */
public class AtlasImageGetter {


    public static File get(ImageServiceParams params) throws FailedRequestException, IOException {

        try {
            String sizeStr = null;
            boolean isCube = false; // Default is 'image', see IrsaMasterDataSource.java, if cube, cutout doesn't make sense here! See IRSA-2154
            IbeDataSource ibeSource = null;
            Map<String, String> queryMap = new HashMap<String, String>(11);
            String errorMsg ="";
            if (params instanceof AtlasImageParams) {
                AtlasImageParams atlasParams = (AtlasImageParams) params;
                if(!Float.isNaN(atlasParams.getSize())){
                    sizeStr= atlasParams.getSize()+"";
                }
                ibeSource = new AtlasIbeDataSource();

                Map<String, String> m = new HashMap<String, String>(1);
                m.put(AtlasIbeDataSource.BAND_KEY, atlasParams.getBand());
                m.put(AtlasIbeDataSource.INSTRUMENT_KEY, atlasParams.getInstrument());
                if(atlasParams.getDs()!=null){ // using AtlasIbeSource defined datasets
                    m.put(AtlasIbeDataSource.DS_KEY, atlasParams.getDs());
                }else{
                    m.put(AtlasIbeDataSource.DATASET_KEY, atlasParams.getSchema());
                    m.put(AtlasIbeDataSource.TABLE_KEY, atlasParams.getTable());
                    //Extra filter for querying images for now - see IRSA-
                    queryMap.put(AtlasIbeDataSource.XTRA_KEY, atlasParams.getXtraFilter());
                    queryMap.put(AtlasIbeDataSource.BAND_KEY, atlasParams.getBand());
                    queryMap.put(AtlasIbeDataSource.INSTRUMENT_KEY, atlasParams.getInstrument());
                }
                errorMsg = atlasParams.getSchema()+"/"+atlasParams.getTable()+"/"+atlasParams.getBand();

                isCube = atlasParams.getDataType() != null && atlasParams.getDataType().equalsIgnoreCase("cube"); //TODO Can be also from metadata 'hdu' is it's consistent
                ibeSource.initialize(m);

            } else {
                Assert.argTst(false, "unknown request type");
            }

            IBE ibe = new IBE(ibeSource);


            File queryTbl = File.createTempFile("Ibe2Query-", ".tbl", CacheHelper.getDir());

            IbeQueryParam queryParam = ibeSource.makeQueryParam(queryMap);
            queryParam.setPos(params.getRaJ2000String() + "," + params.getDecJ2000String());
            queryParam.setMcen(true);
            //queryParam.setIntersect(IbeQueryParam.Intersect.CENTER);
            ibe.query(queryTbl, queryParam);

            DataGroup data = IpacTableReader.read(queryTbl);


            if (data.values().size() == 1) {
                DataObject row = data.get(0);
                Map<String, String> dataMap = IpacTableUtil.asMap(row);
                IbeDataParam dataParam = ibeSource.makeDataParam(dataMap);
                if(sizeStr!=null && !isCube){
                    dataParam.setCutout(true, params.getRaJ2000String() + "," + params.getDecJ2000String(), sizeStr);
                }
                dataParam.setDoZip(true);
                FileInfo result = ibe.getData(dataParam, null);
                return new File(result.getInternalFilename());
            } else {
                throw new FailedRequestException("Area not covered "+errorMsg);
            }

        } catch (IOException me) {
            throw new FailedRequestException("Could not parse results", "Details in exception", me);
        }

    }
}
