package edu.caltech.ipac.targetgui.net;

import edu.caltech.ipac.astro.ned.NedException;
import edu.caltech.ipac.astro.ned.NedObject;
import edu.caltech.ipac.astro.ned.NedReader;
import edu.caltech.ipac.astro.ned.NedResultSet;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.target.NedAttribute;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.visualize.draw.FixedObject;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.FixedObjectGroupUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.apache.xmlbeans.XmlOptions;
import org.usVo.xml.voTable.VOTABLEDocument;

import java.awt.Window;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;


/**
 * @author Xiuqin Wu, based on Trey Roby's CoordConvert
 */
public class NedNameResolver extends ThreadedService {

   private static final String CGI_CMD="/cgi-bin/nph-objsearch?extend=no&out_csys=Equatorial&out_equinox=J2000.0&obj_sort=RA+or+Longitude&of=xml_main&zv_breaker=30000.0&list_limit=1&img_stamp=NO&objname=";

   private final static String OP_DESC = "Ned Name Resolver";
   private final static String PROC_DESC = "Searching for Name: ";

   private String _in = null;
   private boolean _useVOTable;
   private NedAttribute _out = null;
   private NedReader _nedReader = null;

   private NedNameResolver(String objname, Window w, boolean useVOTable) {
      super(w);
      setOperationDesc(OP_DESC);
      setProcessingDesc(PROC_DESC + objname);
      _in = objname;
      _useVOTable = useVOTable;
      if (!_useVOTable) _nedReader = new NedReader();
   }

   private NedNameResolver(String objname, Window w) {
      this(objname, w, false);
   }

   protected void doService() throws Exception {
      _out = lowlevelNameResolver(_in);

   }

   public static NedAttribute getPosition(String objname, Window w, boolean useVOTable)
           throws FailedRequestException {
      NedNameResolver action = new NedNameResolver(objname, w,useVOTable);
      action.execute();
      return action._out;
   }

   public static NedAttribute getPosition(String objname, Window w)
           throws FailedRequestException {
      return getPosition(objname, w, false);
   }

   public NedAttribute lowlevelNameResolver(String objname)
           throws ClassNotFoundException,
           FailedRequestException {
      NedResultSet nedResult;
      PositionJ2000 positionOut = null;

      if (_useVOTable)
         positionOut = getPositionVOTable(objname, this);
      else {
         nedResult = getNedResult(objname);

         if (nedResult.size() > 0) {
            Enumeration elements = nedResult.elements();
            NedObject nObject = (NedObject) elements.nextElement();
            positionOut = new PositionJ2000(nObject.getRA(), nObject.getDec());
         } else {
         throw new FailedRequestException("No NED object returned");
         }
      }

      NedAttribute na = new NedAttribute(positionOut);
      return na;
   }

   public NedResultSet getNedResult(String objname) throws FailedRequestException {

      NedResultSet nedResult;
      try {
         _nedReader.connect();
         nedResult = _nedReader.searchByName(objname);
         _nedReader.disconnect();
      }
      catch (NedException ne) {
         throw new FailedRequestException(
                 "NED did not find the object: " + objname,
                 ne.getMessage(), ne);
      }
      catch (UnknownHostException uhe) {
         throw new FailedRequestException(
                 " NED Service Unavailable", null, uhe);
      }
      catch (IOException ioe) {
         throw new FailedRequestException(
                 "NED Service Unavailable", null, ioe);
      }
      return nedResult;
   }

   public static PositionJ2000 getPositionVOTable(String objname, ThreadedService ts ) throws FailedRequestException {
      PositionJ2000 pos = null;
      ClientLog.message("Requesting name resolution for \"" +
            objname + "\"...");

      HostPort hp = NetworkManager.getInstance().getServer(
            NetworkManager.NED_SERVER);

       try {


           String urlStr = "http://" + hp.getHost() + ":" + hp.getPort() + CGI_CMD + URLEncoder.encode(objname, "UTF-8");
           System.out.printf("url: %s%n", urlStr);

           URL url = new URL(urlStr);

           String data = URLDownload.getStringFromURL(url, ts);
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
