package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.client.net.CacheHelper;
import edu.caltech.ipac.client.net.DownloadListener;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 * @version $Id: DssImageGetter.java,v 1.9 2012/08/21 21:30:41 roby Exp $
 */
@Deprecated
public class WiseImageGetter extends  ThreadedService {

    private WiseImageParams _params;
    private File           _outFile;
    private static final ClassProperties _prop= new ClassProperties(
                                                  WiseImageGetter.class);
    private static final String   OP_DESC= _prop.getName("desc");
    private static final String   SEARCH_DESC= _prop.getName("searching");
    private static final String   LOAD_DESC= _prop.getName("loading");


    /**
     * @param params the parameter for the query
     * @param outFile file to write to
     * @param w a Window
     */
    private WiseImageGetter(WiseImageParams params, File outFile, Window w) {
        super(w);
       _params = params;
       _outFile= outFile;
       setOperationDesc(OP_DESC);
       setProcessingDesc(SEARCH_DESC);
    }

    protected void doService() throws Exception { 
        lowlevelGetWiseImage(_params, _outFile, this);
    }

    @Deprecated
    public static void getWiseImage(WiseImageParams params,
                                   File           outFile,
                                   Window         w) 
                                         throws FailedRequestException {
       WiseImageGetter action= new WiseImageGetter(params, outFile,w);
       action.execute(true);
    }

    @Deprecated
    public static void lowlevelGetWiseImage(WiseImageParams params,
                                           File           outFile) 
                                           throws FailedRequestException,
                                                  IOException {
       lowlevelGetWiseImage(params, outFile, null);
    }

    public static void lowlevelGetWiseImage(WiseImageParams   params,
                                           File             outFile,
                                           DownloadListener dl )
                                           throws FailedRequestException,
                                                  IOException {
      ClientLog.message("Retrieving WISE image");


      try  {
          WiseIbeDataSource.DataProduct product= params.getType().equals(WiseImageParams.WISE_3A) ?
                                                WiseIbeDataSource.DataProduct.ALLWISE_MULTIBAND_3A :
                                                WiseIbeDataSource.DataProduct.ALLSKY_4BAND_1B;

          WiseIbeDataSource wiseSource= new WiseIbeDataSource(product);
          IBE ibe= new IBE(wiseSource);


          File queryTbl= File.createTempFile("WiseQuery-", ".tbl", CacheHelper.getDir());
          Map<String,String> queryMap= new HashMap<String,String>(11);


          queryMap.put("band", params.getBand());

          IbeQueryParam queryParam= wiseSource.makeQueryParam(queryMap);
          queryParam.setPos(params.getRaJ2000String() + "," + params.getDecJ2000());
          queryParam.setMcen(true);
          queryParam.setIntersect(IbeQueryParam.Intersect.CENTER);
          ibe.query(queryTbl, queryParam);

          DataGroup data = IpacTableReader.readIpacTable(queryTbl, "results");


          if (data.values().size() == 1) {
              DataObject row = data.get(0);
              Map<String, String> dataMap = IpacTableUtil.asMap(row);
              dataMap.put(WiseIbeDataSource.FTYPE, WiseIbeDataSource.DATA_TYPE.INTENSITY.name());
              IbeDataParam dataParam= wiseSource.makeDataParam(dataMap);

              dataParam.setCutout(true, params.getRaJ2000String()+","+params.getDecJ2000String(), params.getSize()+"");
              dataParam.setDoZip(true);
              ibe.getData(outFile, dataParam,dl);
//              queryTbl.delete();
          }
          else {
              throw new FailedRequestException("No results found for this location");
          }





      } catch (IpacTableException me){
          ClientLog.warning(me.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Details in exception", me );
      }

      ClientLog.message("Done");
    }



   public static void main(String args[]) {
       WiseImageParams params= new WiseImageParams();
       params.setSize(.33F);
       params.setBand("1");
       params.setBand(WiseImageParams.WISE_3A);
       params.setRaJ2000(10.672);
       params.setDecJ2000(41.259);
       try {
           lowlevelGetWiseImage(params, new File("./a.fits.gz"),null);
       }
       catch (Exception e) {
           System.out.println(e);
       }
   }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
