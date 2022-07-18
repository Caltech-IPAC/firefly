/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.TwoMassIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.ZtfIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.PtfIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.util.*;
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
public class IbeImageGetter {

    static final String NaN = "NaN";
    static final String NULL = "null";
    public static FileInfo get(ImageServiceParams params) throws FailedRequestException {
        boolean isWise= false;


      try  {
          String sizeStr= null;
          Map<String,String> queryMap= new HashMap<>(11);
          IbeDataSource ibeSource= null;

          if (params instanceof WiseImageParams) {
              WiseImageParams wiseParams= (WiseImageParams)params;

              WiseIbeDataSource.DataProduct product= wiseParams.getProductLevel().equals(WiseImageParams.WISE_3A) ?
                                                     WiseIbeDataSource.DataProduct.ALLWISE_MULTIBAND_3A :
                                                     WiseIbeDataSource.DataProduct.ALLSKY_4BAND_3A;

              ibeSource= new WiseIbeDataSource(product);

              queryMap.put("band", wiseParams.getBand());
              if(!Float.isNaN(wiseParams.getSize())){
                  sizeStr= wiseParams.getSize()+"";
              }
              isWise= true;
          }
          else if (params instanceof TwoMassImageParams) {
              TwoMassImageParams irsaParams= (TwoMassImageParams) params;
              if (params.getType()== ImageServiceParams.ImageSourceTypes.TWOMASS || params.getType()== ImageServiceParams.ImageSourceTypes.TWOMASS6) {

                  ibeSource= new TwoMassIbeDataSource();


                  Map<String,String> m= new HashMap<>();
                  m.put(TwoMassIbeDataSource.DS_KEY, irsaParams.getDataset());
                  ibeSource.initialize(m);


                  queryMap.put("band", irsaParams.getBand());
                  // Add extra filtering of filename 'fname' in case of mosaic (IRSA-2742)
                  if(irsaParams.getDataset().equalsIgnoreCase(TwoMassIbeDataSource.DS.MOSAIC.toString())){
                      queryMap.put(TwoMassIbeDataSource.XTRA_CONSTRAINT, "fname like '%_1asec.fit%'");
                  }
                  sizeStr= (irsaParams.getSize()/3600)+"";
              }
              else {
                  Assert.argTst(false, "unknown request type");
              }
          }
          else if (params instanceof ZtfImageParams) {
              ZtfImageParams ztfParams;
              ztfParams = (ZtfImageParams) params;
              ZtfIbeDataSource.DataProduct product= ZtfIbeDataSource.DataProduct.REF;

              ibeSource = new ZtfIbeDataSource(product);

              queryMap.put("filtercode", ztfParams.getBand());
              if (!Float.isNaN(ztfParams.getSize())) {
                sizeStr = ztfParams.getSize() + "";
              }
          }
          else if (params instanceof PtfImageParams) {
              PtfImageParams ptfParams;
              ptfParams = (PtfImageParams) params;
              PtfIbeDataSource.DataProduct product= PtfIbeDataSource.DataProduct.LEVEL2;

              ibeSource = new PtfIbeDataSource(product);

              queryMap.put("fid", ptfParams.getBand());
              if (!Float.isNaN(ptfParams.getSize())) {
                sizeStr = ptfParams.getSize() + "";
              }
          }
          else {
              Assert.argTst(false, "unknown request type");
          }


          IBE ibe= new IBE(ibeSource);


          File queryTbl= File.createTempFile("IbeQuery-", ".tbl", CacheHelper.getDir());

          IbeQueryParam queryParam= ibeSource.makeQueryParam(queryMap);
          queryParam.setPos(params.getRaJ2000String() + "," + params.getDecJ2000String());
          queryParam.setMcen(true);
          queryParam.setIntersect(IbeQueryParam.Intersect.CENTER);
          ibe.query(queryTbl, queryParam);

          DataGroup data = IpacTableReader.read(queryTbl);

          int count = data.values().size();
          if (count == 1) {
              DataObject row = data.get(0);
              Map<String, String> dataMap = IpacTableUtil.asMap(row);
              if (isWise) {
                  dataMap.put(WiseIbeDataSource.FTYPE, WiseIbeDataSource.DATA_TYPE.INTENSITY.name());
              }
              IbeDataParam dataParam= ibeSource.makeDataParam(dataMap);
              Map<String,String> sourceParams= new HashMap<>();
              sourceParams.put("ProgressKey", params.getStatusKey());
              sourceParams.put("plotId", params.getPlotId());

              if (!StringUtils.isEmpty(sizeStr) && !sizeStr.equalsIgnoreCase(NULL) && !sizeStr.equalsIgnoreCase(NaN)) {
                  dataParam.setCutout(true, params.getRaJ2000String() + "," + params.getDecJ2000String(), sizeStr);
              }
              dataParam.setDoZip(true);
              return ibe.getData(dataParam,sourceParams);
          }
          else {
              throw new FailedRequestException(count>1?"Too many results from "+params.getType():"Area not covered in "+params.getType());
          }

      } catch (IOException me){
          throw new FailedRequestException( "Could not parse results", "Details in exception", me );
      }

    }

}
