package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 * @version $Id: SkyViewCatalogList.java,v 1.4 2007/10/22 20:01:01 roby Exp $
 */
public class SkyViewCatalogList {

    private static final ClassProperties _prop= new ClassProperties(
                                                  SkyViewCatalogList.class);
    private static final String   OP_DESC= _prop.getName("desc");

    public enum SearchType { HEASARC_CATALOGS, VIZIER_CATALOGS, SKYVIEW_SURVEYS }

//    private static final String CAT_STR=  "/java/text/curver/Jcatalogs.txt";
//    private static final String SUR_STR=  "/java/text/curver/Jsurveys.txt";
    private static final String SUR_REASOURCE=  "edu/caltech/ipac/visualize/net/resources/Jsurveys.txt";
    private static final String HEA_CAT_REASOURCE=  "edu/caltech/ipac/visualize/net/resources/Heasarc_surveys.txt";
    private static final String VIZ_CAT_REASOURCE=  "edu/caltech/ipac/visualize/net/resources/Vizier_surveys.txt";

    //private SkyViewCatalog skyviewCatalog[] = null;
    private CatalogEntry _skyviewCatalog[] = null;
    private SurveyEntry  _skyviewSurvey[]  = null;
    private SearchType   _searchType;



    public static CatalogEntry[] lowlevelGetCatalogList(SearchType searchType)
                                           throws FailedRequestException,
                                                  IOException {
        Assert.argTst(searchType==SearchType.HEASARC_CATALOGS ||
                      searchType==SearchType.VIZIER_CATALOGS,
                      "search type must be either HEASARC_CATALOGS or VIZIER_CATALOGS");
      ClientLog.message("Retrieving SkyView Catalog List");
      String results[]= lowlevelGetList(searchType);
      return packageCatalogResults(results);
    }

    public static SurveyEntry[] lowlevelGetSurveyList()
                                           throws FailedRequestException,
                                                  IOException {
      ClientLog.message("Retrieving SkyView Survey List");
      String results[]= lowlevelGetList(SearchType.SKYVIEW_SURVEYS);
      return packageSurveyResults(results);
    }




    public static String[] lowlevelGetList(SearchType      searchType)
                                           throws FailedRequestException,
                                                  IOException {


      String results[]= null;

      NetworkManager manager= NetworkManager.getInstance();
      HostPort server= manager.getServer(NetworkManager.SKYVIEW_SERVER);
      Assert.tst(server);

      LineNumberReader lnr= null;
      try  {

          byte data[]= null;
          if (searchType==SearchType.HEASARC_CATALOGS ||
              searchType==SearchType.VIZIER_CATALOGS) {

              String resource= null;
              if (searchType==SearchType.HEASARC_CATALOGS ) {
                  resource= HEA_CAT_REASOURCE;
              }
              else if (searchType==SearchType.VIZIER_CATALOGS) {
                  resource= VIZ_CAT_REASOURCE;
              }
              else {
                  Assert.tst(false);
              }
//              String req= "http://" + server.getHost() + CAT_STR;
//              data= URLDownload.getDataFromURL(new URL(req), ts);
              URL url= ClassLoader.getSystemResource(resource);
              data= URLDownload.getDataFromURL(url, null);



              lnr= new LineNumberReader(new InputStreamReader(
                                            new ByteArrayInputStream(data)));

              List<String> sList= new ArrayList<String>(300);
              for(String inS = lnr.readLine(); inS!=null; inS= lnr.readLine()) {
                  if (inS!=null) {
                      sList.add(inS);
                  }
              }
              results= sList.toArray(new String[sList.size()]);
          }
          else if (searchType==SearchType.SKYVIEW_SURVEYS) {
              URL url= ClassLoader.getSystemResource(SUR_REASOURCE);
              data= URLDownload.getDataFromURL(url, null);
              results= readInfo( new ByteArrayInputStream(data));
          }
          else {
              Assert.tst(false, "unsupported SearchType");
          }



      } catch (MalformedURLException me){
          ClientLog.warning(me.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Details in exception", me );
      } catch (NumberFormatException nfe){
          ClientLog.warning(nfe.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Could not parse line one of query", nfe );
      } finally {
          FileUtil.silentClose(lnr);
      }

      return results;
    }
    




  /**
   * Code from Skyview to parse the output
   * @param is the Stream to read from
   * @return an array that has on entry per line of the file
   * @throws IOException if any read problem
   * @throws NumberFormatException if the first line is not an integer
   */
  private static String[] readInfo(InputStream is)
                                         throws IOException,
                                                NumberFormatException {

    LineNumberReader lnr= new LineNumberReader(new InputStreamReader(is));

    String lines = lnr.readLine();

    // the next line tells how many lines follow
    //System.out.println("lines = " + lines);
    
    // if the result is null -- an error occurred with
    // catalog_browse and should be treated as if a 0
    // was returned
    int intlines;
    if (lines != null) {
      intlines = Integer.parseInt(lines);
    } else {
      //System.out.println("No catalog data for selection(s)");
      intlines = 0;
    }

    String[] catdata=null;

    // if there is catalog information (intlines>0) read it in
    if (intlines > 0) {

      // initialize an array to hold the catalog info
      catdata = new String[intlines];

      // loop and collect the catalog data and put in into the
      // catdata array
      for (int i=0;i<intlines;i++) {
	catdata[i] = lnr.readLine();
	if (catdata[i] == null) break;
      }
    } 

    // return the value

    lnr.close();

    return catdata;
     
  }



  public static CatalogEntry[] packageCatalogResults(String strAry[]) {
      CatalogEntry entry[]= new CatalogEntry[strAry.length];
      String parts[];
      for(int i=0; (i<strAry.length); i++) {
          parts= strAry[i].split("\\|");
          if (parts.length!=2) {
              ClientLog.warning("Ignoring line " + (i+1) + " : " + strAry[i] );
          }
          entry[i]= new CatalogEntry(parts[1], parts[0]);
      }
      return entry;
  }

  public static SurveyEntry[] packageSurveyResults(String strAry[]) {
      String       parseStr[];
      SurveyEntry entry[]= new SurveyEntry[strAry.length];
      for(int i=0; (i<strAry.length); i++) {
           parseStr = strAry[i].split("\\|\\|");
           entry[i] = new SurveyEntry( parseStr[0], parseStr[1] );
      }
      return entry;
  }




   public static class CatalogEntry {
      private String _description;
      private String _table;

      public CatalogEntry(String description, String table) {
          _description= description;
          _table      = table;
      }

      public String getDescription() { return _description; }
      public String getTable()       { return _table; }
   }


   public static class SurveyEntry {
      private String _survey;
      private String _type;

      public SurveyEntry(String survey, String type) {
          _survey= survey;
          _type  = type;
      }

      public String getSurvey()    { return _survey; }
      public String getSurveyType() { return _type; }
   }


   public static void main(String args[]) {
       try {
          CatalogEntry[] catAry= lowlevelGetCatalogList(SearchType.HEASARC_CATALOGS);
          for(CatalogEntry entry : catAry) {
               System.out.println(entry.getDescription() + "\t" +
                                  entry.getTable() );
          }
          SurveyEntry[] surAry= lowlevelGetSurveyList();
          for(SurveyEntry entry : surAry) {
               System.out.println(entry.getSurveyType() + "\t" +
                                  entry.getSurvey() );
          }
      } catch (Exception e) {

           System.out.println("main: failed: " + e);
           e.printStackTrace();
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
