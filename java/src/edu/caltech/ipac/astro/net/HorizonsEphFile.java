package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.astro.target.Ephemeris;
import edu.caltech.ipac.astro.target.NonStandardEphemeris;
import edu.caltech.ipac.astro.target.StandardEphemeris;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Booth Hartley
 * @version $Id: HorizonsEphFile.java,v 1.8 2012/01/23 22:09:55 roby Exp $
 */
public class HorizonsEphFile {

    private static final ClassProperties _prop= new ClassProperties(
                                                  HorizonsEphFile.class);
    private static final String  LOAD_DESC = _prop.getName("loading");

    private static final String CGI_CMD= "/x/smb_spk.cgi";

    private static String suggested_filename;




    public static byte[] lowlevelGetEphFile(HorizonsFileParams params)
                                         throws IOException,
                                                FailedRequestException {

        HostPort server= NetworkManager.getInstance().getServer(
                                              NetworkManager.HORIZONS_NAIF);



        ClientLog.message("Retrieving Ephemeris file");
        String urlStr=  "http://" +
                 server.getHost() + ":" + server.getPort() + CGI_CMD;
        if (SUTDebug.isDebug()) System.out.println("RBH URL = "+ urlStr);
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(buildParamString(params));
        wr.flush();
        wr.close();

        String contentType = conn.getContentType();
        URLDownload.logHeader(conn);

        if (contentType != null && contentType.startsWith("text/")) {
            String htmlErr= URLDownload.getStringFromOpenURL(conn,null);
            throw new FailedRequestException(
                    htmlErr,
                    "The Horizons server is reporting an error- " +
                            "the Horizons error message was displayed to the user.",
                    true, null );
        }
        suggested_filename = URLDownload.getSugestedFileName(conn);
        if (SUTDebug.isDebug())
            System.out.println("RBH suggested_filename = " + suggested_filename);

        byte retval[]= URLDownload.getDataFromOpenURL(conn, null);


        ClientLog.message("Done");
        return retval;
    }

    public static String getSuggestedFilename()
    {
	return suggested_filename;
    }

    private static String buildParamString(HorizonsFileParams params)
                   throws FailedRequestException {

        StringBuffer data=new StringBuffer(100);
        try {
            String qType=null;
            if(HorizonsFileParams.XSP_EXT.equals(params.getFileType())) {
                qType= "-T";
            }
            else if(HorizonsFileParams.BSP_EXT.equals(params.getFileType())) {
                qType= "-B";
            }
            else {
               throw new FailedRequestException("Could not get all parameters",
                     "parameter extension not "+
                      HorizonsFileParams.XSP_EXT + " or "+
                      HorizonsFileParams.BSP_EXT+
                      " - Caller sent extension: " +  params.getFileType());
            }

            addParam("OPTION", "Make SPK", data, true);
            addParam("OBJECT", params.getNaifID(), data, true);
            addParam("START", params.getBeginDateStr(), data, true);
            addParam("STOP", params.getEndDateStr(), data, true);
            addParam("EMAIL", "spot@caltech.edu", data, true);
            addParam("TYPE", qType, data, false);

            if(!params.isStandard()) {
                data.append("&");
                addParam("EPOCH", params.getEpoch(), data, true);
                addParam("EC", params.getE(), data, true);
                addParam("QR", params.getQ(), data, true);
                addParam("TP", params.getT(), data, true);
                addParam("OM", params.getBigOmega(), data, true);
                addParam("W", params.getLittleOmega(), data, true);
                addParam("IN", params.getI(), data, false);
            }

            ClientLog.brief("params: " + data);
        } catch (Exception e) {
            throw new FailedRequestException("Could not encode query",
                                             null, e);
        }
	if (SUTDebug.isDebug())
	    System.out.println("RBH passing to HORIZONS: " + data.toString());
        return data.toString();
    }
    
    private static void addParam(String       param,
                                 String       value,
                                 StringBuffer data,
                                 boolean       plus)
                                           throws UnsupportedEncodingException {
        data.append(URLEncoder.encode(param, "UTF-8"));
        data.append("=");
        data.append(URLEncoder.encode(value, "UTF-8"));
        if (plus) data.append("&");
    }


    public static void main(String args[]) {
        try {
            SimpleDateFormat df=  new SimpleDateFormat("dd-MMM-yyyy");
            Date begin= df.parse("1-jan-2004");
            Date end= df.parse("1-jan-2008");

            //Ephemeris eph= new StandardEphemeris(1000036, "");
            //Ephemeris eph= new StandardEphemeris(123, "");

            Ephemeris eph= new StandardEphemeris(999,"Pluto");

//            Ephemeris eph= new NonStandardEphemeris("2452944.5","244",
//                                                    .9680055D,
//                                                    .5714061D, 1.62D,
//                                                    111.712D,
//                                                    58.89801D);


            NonStandardEphemeris nonStdEph1= new NonStandardEphemeris("2452944.5","244",
                                                          .9680055D,
                                                          .5714061D, 1.62D,
                                                          111.712D,
                                                          58.89801D);

            int code1 = Arrays.hashCode( new Object [] { nonStdEph1.getEpoch(),
                                          nonStdEph1.getT(),
                                          nonStdEph1.getE(),
                                          nonStdEph1.getQ(),
                                          nonStdEph1.getI(),
                                          nonStdEph1.getBigOmega(),
                                          nonStdEph1.getLittleOmega() } );

            //NonStandardEphemeris nonStdEph2= new NonStandardEphemeris(new String("2452944.5"),"244",
            NonStandardEphemeris nonStdEph2= new NonStandardEphemeris(
                                          new String("2452944.5"),
                                                                      "244",
                                                                      .9680055D,
                                                                      .5714061D, 1.62D,
                                                                      111.712D,
                                                                      58.89801D);

            int code2 = Arrays.hashCode( new Object [] { nonStdEph2.getEpoch(),
                                          nonStdEph2.getT(),
                                          nonStdEph2.getE(),
                                          nonStdEph2.getQ(),
                                          nonStdEph2.getI(),
                                          nonStdEph2.getBigOmega(),
                                          nonStdEph2.getLittleOmega() } );


            NonStandardEphemeris nonStdEph3= new NonStandardEphemeris("2452944.5","244",
                                                                      .1680055D,
                                                                      .5714061D, 1.62D,
                                                                      111.712D,
                                                                      58.89801D);

            int code3 = Arrays.hashCode( new Object [] { nonStdEph3.getEpoch(),
                                          nonStdEph3.getT(),
                                          nonStdEph3.getE(),
                                          nonStdEph3.getQ(),
                                          nonStdEph3.getI(),
                                          nonStdEph3.getBigOmega(),
                                          nonStdEph3.getLittleOmega() } );




            System.out.println("code1= "+ code1);
            System.out.println("code2= "+ code2);
            System.out.println("code3= "+ code3);

            HorizonsFileParams params= new HorizonsFileParams(
                                                eph, begin, end,
                                                HorizonsFileParams.BSP_EXT);

            byte results[]= lowlevelGetEphFile(params);
            FileOutputStream out= new FileOutputStream(
                                         new File(params.toString()));
            out.write(results);
        } catch (Exception e) {
            System.out.println("e= " + e.toString());
            e.printStackTrace();
        }
    }


}
