package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.astro.target.PositionJ2000;
import edu.caltech.ipac.visualize.draw.FixedObject;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.FixedObjectGroupUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.apache.xmlbeans.XmlOptions;
import org.usVo.xml.voTable.VOTABLEDocument;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;


/**
 * @author Xiuqin Wu, based on Trey Roby's CoordConvert
 */
public class NedNameResolver {

   private static final String CGI_CMD="/cgi-bin/nph-objsearch?extend=no&out_csys=Equatorial&out_equinox=J2000.0&obj_sort=RA+or+Longitude&of=xml_main&zv_breaker=30000.0&list_limit=1&img_stamp=NO&objname=";


   public static PositionJ2000 getPositionVOTable(String objname) throws FailedRequestException {
      PositionJ2000 pos = null;
      ClientLog.message("Requesting name resolution for \"" +
            objname + "\"...");

      HostPort hp = NetworkManager.getInstance().getServer(
            NetworkManager.NED_SERVER);

       try {


           String urlStr = "http://" + hp.getHost() + ":" + hp.getPort() + CGI_CMD + URLEncoder.encode(objname, "UTF-8");
           System.out.printf("url: %s%n", urlStr);

           URL url = new URL(urlStr);

           String data = URLDownload.getStringFromURL(url, null);
           // System.out.println(data);

           XmlOptions xmlOptions = new XmlOptions();
           HashMap<String, String> substituteNamespaceList =
                   new HashMap<String, String>();
           substituteNamespaceList.put("", "http://us-vo.org/xml/VOTable.xsd");
           xmlOptions.setLoadSubstituteNamespaces(substituteNamespaceList);
           xmlOptions.setSavePrettyPrint();
           xmlOptions.setSavePrettyPrintIndent(4);

           //VOTABLEDocument voTableDoc = parseVoTable(data, xmlOptions);
           VOTABLEDocument voTableDoc = VOTABLEDocument.Factory.parse(
                   data,xmlOptions);
//        PrintWriter outF= new PrintWriter(new File("vo.dat"));
//        outF.println(voTableDoc.toString());

           //System.out.println(voTableDoc.toString());

           //nedResult = NedVOTableParser.makeNedResult(voTableDoc);

           FixedObjectGroup fixGroup = FixedObjectGroupUtils.makeFixedObjectGroup(voTableDoc);
           if (fixGroup.size() >0) {
               FixedObject fixedObj = fixGroup.get(0);
               WorldPt wpt = Plot.convert(fixedObj.getPosition(), CoordinateSys.EQ_J2000);
               pos = new PositionJ2000(wpt.getLon(), wpt.getLat());
           }
           else
                throw new FailedRequestException("NED did not find the object: " +objname,
                   "NED could not resolve the input object name: " +objname);


       } catch (Exception e) {
           throw new FailedRequestException("NED did not find the object: " +objname,
                   "NED could not resolve the input object name: " +objname, e);
       }


      return pos;
   }
}
