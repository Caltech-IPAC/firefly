/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import VizieRBeta_pkg.VizieRBeta;
import VizieRBeta_pkg.VizieRBetaServiceLocator;
import VizieRBeta_pkg.VizieRCriteria;
import VizieRBeta_pkg.VizieRFilter;
import VizieRBeta_pkg.VizieRTarget;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.action.ClassProperties;

import javax.xml.rpc.ServiceException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * This class handles getting catalogs for SkyView.
 * @author Trey Roby
 */

public class SkyViewCatalogGetter {

  private final static ClassProperties _prop =
                          new ClassProperties(SkyViewCatalogGetter.class);

  // constants


  /**
   * get the catalog
   * @param params the Skyview Catalog Parameters
   * @param outfile the File
   * @exception FailedRequestException
   */  
  public static void lowlevelGetCatalog(SkyViewCatalogParams params,
                                        File                 outfile)
                                             throws FailedRequestException,
                                                    IOException {

      ClientLog.message("Retrieving SkyView Catalog:" + 
                                          params.getCatalogName());

      NetworkManager manager= NetworkManager.getInstance();

      try  {
          if (params.getSite()== SkyViewCatalogParams.Site.HEASARC) {
              HostPort server= manager.getServer(NetworkManager.HEASARC_SERVER);
              Assert.tst(server);
              String req= "http://" + server.getHost() +  "/cgi-bin/vo/cone/coneGet.pl?" +
                          "table=" + params.getCatalogName() +
                          "&RA="+params.getRaJ2000String() +
                          "&DEC="+params.getDecJ2000String()+
                          "&SR="+ params.getSize();
              URLDownload.getDataToFile(new URL(req), outfile,null);
          }
          else if (params.getSite()== SkyViewCatalogParams.Site.VIZIER) {

              HostPort server= manager.getServer(NetworkManager.VIZIER_SERVER);
              Assert.tst(server);
              /*

              String req= "http://" + server.getHost() +
                          "/viz-bin/VizieR?-out.add=_r%2C_RAJ%2C_DEJ&-sort=_r&-to=4&-out.max=unlimited"+
                          "&-source=" + params.getCatalogName() +
                          "&-c=" +params.getRaJ2000String() + "%2B" +
                          params.getDecJ2000String()+
                          "&-c.rs=" + (params.getSize()*60*60) +
                          "%-out.form=VOTable";
              URLDownload.getDataToFile(new URL(req), outfile, ts);
              */

//
//              String req= "http://" + server.getHost() + "/cgi-bin/asu-xml/";
//
//              StringBuffer data= new StringBuffer(100);
//              data.append("&-source=");
//              data.append(params.getCatalogName());
//              data.append("&-c=");
//              data.append(params.getRaJ2000String());
//              data.append("%2B");
//              data.append(params.getDecJ2000String());
//              data.append("&-c.rs=");
//              data.append(params.getSize()*60*60);
//              data.append("&-out.max=unlimited");
//              data.append("&-mime=XML");
//              data.append("&-out.form=VOTable");
//
//              URLDownload.getDataToFile(new URL(req), outfile, ts);



//              String req= "http://" + server.getHost() + "/viz-bin/VizieR";
//
//              StringBuffer data= new StringBuffer(100);
//              data.append("-out.add=_r%2C_RAJ%2C_DEJ&-sort=_r&-to=4");
//              data.append("&-source=");
//              data.append(params.getCatalogName());
//              data.append("&-c=");
//              data.append(params.getRaJ2000String());
//              data.append("%2B");
//              data.append(params.getDecJ2000String());
//              data.append("&-c.rs=");
//              data.append(params.getSize()*60*60);
//              data.append("&-out.max=unlimited");
//              data.append("&-out.form=VOTable");
//
//              StringBuffer dataTst= new StringBuffer(100);
//              dataTst.append("-to=4&-from=-4&-this=-4&-source=&-out.max=20&-out.form=VOTable&%21-ignore%3D-to%3D%21*%3B"+
//                          "-4%3B=++ReSubmit++&-c.rs=59&-c=m31&-out.add=_r%2C_RAJ%2C_DEJ&-sort=_r&-source=2MASS&-meta"+
//                          ".ucd=0");

//          data.append(URLEncoder.encode("OPTION", "UTF-8"));
//          data.append("=");
//          data.append(URLEncoder.encode("Look up", "UTF-8"));
//          data.append("&");
//              URL url = new URL(req);
//
//              URLDownload.getDataFromURLToFileUsingPost(url, dataTst.toString(),
//                                                        outfile, ts);


              String target= "position " + params.getRaJ2000String() + " " +  params.getDecJ2000String();
              String radius= params.getSize()*60*60 + " arcsec";


              VizieRBetaServiceLocator locator= new VizieRBetaServiceLocator();
              VizieRBeta viz= locator.getVizieRBeta();

              VizieRCriteria criteria=   new VizieRCriteria();
//              criteria.setCatalog("IRAS*");
              criteria.setCatalog(params.getCatalogName());
              VizieRTarget tar=   new VizieRTarget();
//              tar.setPosition(params.getRaJ2000String() + " " +  params.getDecJ2000String());
//              tar.setName("m31");
              tar.setName(params.getTargetName());
              if (params.getTargetName()==null) {
                  throw new FailedRequestException(_prop.getError("vizier.targetName"));
              }
//              tar.setReference("J2000");
//              tar.setSexaordec(false);
//              tar.setPosition(params.getRaJ2000String() + " +" +  params.getDecJ2000String());

              VizieRFilter filter=   new VizieRFilter();

//              String results= viz.coneCatalogs(criteria, tar, radius, filter);
              String results= viz.coneResults(criteria, tar, radius, filter);
              FileOutputStream ostr= new FileOutputStream(outfile);
              ostr.write(results.getBytes());




          }
          else {
              Assert.tst(false);
          }


//         String pos= URLEncoder.encode(params.getRaJ2000String()+ "," +
//                                        params.getDecJ2000String(),  "UTF-8");
//
//         String req= "http://" + server.getHost() + CAT_STR  +
//              "table=" + URLEncoder.encode(params.getCatalogName(),"UTF-8") +
//              "&" +
//              "coordinates=equatorial&" +
//              "position=" + pos + "&" +
//              "equinox=2000&" +
//              "radius="+sizeInArcMin;



          // System.out.println(data);



      } catch (MalformedURLException me){
          ClientLog.warning(me.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Details in exception", me );
      } catch (RemoteException e) {
          throw new FailedRequestException(
                                        FailedRequestException.SERVICE_FAILED,
                                        "Could not parse line one of query", e );
      } catch (ServiceException e) {
          throw new FailedRequestException(
                                        FailedRequestException.SERVICE_FAILED,
                                        "Could not parse line one of query", e );

      } catch (NumberFormatException nfe){
          ClientLog.warning(nfe.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Could not parse line one of query", nfe );

      }
  }




}
