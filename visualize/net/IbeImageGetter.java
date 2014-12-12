package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.TwoMassIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.client.net.CacheHelper;
import edu.caltech.ipac.client.net.DownloadListener;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.util.Assert;
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
public class IbeImageGetter extends  ThreadedService {

    private BaseIrsaParams _params;
    private File           _outFile;
    private static final ClassProperties _prop= new ClassProperties(
                                                  IbeImageGetter.class);
    private static final String   OP_DESC_WISE= _prop.getName("desc.wise");
    private static final String   OP_DESC_2MASS= _prop.getName("desc.2mass");
    private static final String   SEARCH_DESC= _prop.getName("searching");
    private static final String   LOAD_DESC= _prop.getName("loading");


    /**
     * @param params the parameter for the query
     * @param outFile file to write to
     * @param w a Window
     */
    private IbeImageGetter(BaseIrsaParams params, File outFile, Window w) {
        super(w);
       _params = params;
       _outFile= outFile;
       if (params instanceof WiseImageParams) {
           setOperationDesc(OP_DESC_WISE);
       }
       else if (params instanceof IrsaImageParams) {
           IrsaImageParams p= (IrsaImageParams)params;
           switch (p.getType()) {
               case TWOMASS:
                   setOperationDesc(OP_DESC_2MASS);
                   break;
               case ISSA:
               case IRIS:
               case MSX:
                    setOperationDesc("IRSA Image");
                   break;
           }
       }
       setProcessingDesc(SEARCH_DESC);
    }

    protected void doService() throws Exception { 
        lowlevelGetIbeImage(_params, _outFile, this);
    }

    public static void getIbeImage(BaseIrsaParams params,
                                   File           outFile,
                                   Window         w,
                                   boolean        moreCallsComming)
                                         throws FailedRequestException {
       IbeImageGetter action= new IbeImageGetter(params, outFile,w);
        action.setMoreRequestComming(moreCallsComming);
       action.execute(true);
    }

    public static void lowlevelGetIbeImage(BaseIrsaParams params,
                                           File           outFile) 
                                           throws FailedRequestException,
                                                  IOException {
       lowlevelGetIbeImage(params, outFile, null);
    }

    public static void lowlevelGetIbeImage(BaseIrsaParams  params,
                                           File             outFile,
                                           DownloadListener dl )
                                           throws FailedRequestException,
                                                  IOException {
      ClientLog.message("Retrieving WISE image");
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
              ibe.getData(outFile, dataParam,dl);
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
           lowlevelGetIbeImage(params, new File("./a.fits.gz"), null);
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
