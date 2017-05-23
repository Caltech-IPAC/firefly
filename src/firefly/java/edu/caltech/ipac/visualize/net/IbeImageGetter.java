/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.TwoMassIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
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


    public static File lowlevelGetIbeImage(BaseIrsaParams  params)
                                           throws FailedRequestException,
                                                  IOException {
        boolean isWise= false;


      try  {
          String sizeStr= null;
          Map<String,String> queryMap= new HashMap<String,String>(11);
          IbeDataSource ibeSource= null;

          if (params instanceof WiseImageParams) {
              WiseImageParams wiseParams= (WiseImageParams)params;

              WiseIbeDataSource.DataProduct product= wiseParams.getType().equals(WiseImageParams.WISE_3A) ?
                                                     WiseIbeDataSource.DataProduct.ALLWISE_MULTIBAND_3A :
                                                     WiseIbeDataSource.DataProduct.ALLSKY_4BAND_1B;

              ibeSource= new WiseIbeDataSource(product);

              queryMap.put("band", wiseParams.getBand());
              sizeStr= wiseParams.getSize()+"";
              isWise= true;
          }
          else if (params instanceof IrsaImageParams) {
              IrsaImageParams irsaParams= (IrsaImageParams)params;
              if (irsaParams.getType()== IrsaImageParams.IrsaTypes.TWOMASS || irsaParams.getType()== IrsaImageParams.IrsaTypes.TWOMASS6) {

                  ibeSource= new TwoMassIbeDataSource();


                  Map<String,String> m= new HashMap<String, String>(1);
                  if (irsaParams.getType()== IrsaImageParams.IrsaTypes.TWOMASS)  {
                      m.put(TwoMassIbeDataSource.DS_KEY, "ASKY");
                  }
                  else {
                      m.put(TwoMassIbeDataSource.DS_KEY, "SX");
                  }
                  ibeSource.initialize(m);


                  queryMap.put("band", irsaParams.getBand());
                  sizeStr= (irsaParams.getSize()/3600)+"";
              }
              else {
                  Assert.argTst(false, "unknown request type");
              }
          }
          else {
              Assert.argTst(false, "unknown request type");
          }


          IBE ibe= new IBE(ibeSource);


          File queryTbl= File.createTempFile("IbeQuery-", ".tbl", CacheHelper.getDir());

          IbeQueryParam queryParam= ibeSource.makeQueryParam(queryMap);
          queryParam.setPos(params.getRaJ2000String() + "," + params.getDecJ2000());
          queryParam.setMcen(true);
          queryParam.setIntersect(IbeQueryParam.Intersect.CENTER);
          ibe.query(queryTbl, queryParam);

          DataGroup data = IpacTableReader.readIpacTable(queryTbl, "results");


          if (data.values().size() == 1) {
              DataObject row = data.get(0);
              Map<String, String> dataMap = IpacTableUtil.asMap(row);
              if (isWise) {
                  dataMap.put(WiseIbeDataSource.FTYPE, WiseIbeDataSource.DATA_TYPE.INTENSITY.name());
              }
              IbeDataParam dataParam= ibeSource.makeDataParam(dataMap);

              dataParam.setCutout(true, params.getRaJ2000String()+","+params.getDecJ2000String(), sizeStr);
              dataParam.setDoZip(true);
              FileInfo result= ibe.getData(dataParam,null);
              return new File(result.getInternalFilename());
          }
          else {
              throw new FailedRequestException("Area not covered");
          }

      } catch (IpacTableException me){
          throw new FailedRequestException( "Could not parse results", "Details in exception", me );
      }

    }



   public static void main(String args[]) {
//       WiseImageParams params= new WiseImageParams();
//       params.setSize(.33F);
//       params.setBand("1");
//       params.setBand(WiseImageParams.WISE_3A);
//       params.setRaJ2000(10.672);
//       params.setDecJ2000(41.259);
//       try {
//           lowlevelGetIbeImage(params, new File("./a.fits.gz"), null);
//       }
//       catch (Exception e) {
//           System.out.println(e);
//       }
   }
}
