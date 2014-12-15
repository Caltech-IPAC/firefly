package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.target.CoordinateSys;
import edu.caltech.ipac.astro.target.Position;
import edu.caltech.ipac.astro.target.PositionJ2000;
import edu.caltech.ipac.astro.target.UserPosition;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.ParseException;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.draw.FixedObject;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class IspyGetter {

    private static final ClassProperties _prop=new ClassProperties(
                                                   IspyGetter.class);

    private static final String CGI_CMD="/x/ispy.cgi";


    private static final int ID_POS=               0;
    private static final int IAU_POS=              1;
    private static final int NAME_POS=             2;
    private static final int RA_POS=               3;
    private static final int DEC_POS=              4;
    private static final int AMAG_POS=             5;
    private static final int RA_ARCSEC_PER_HR_POS= 6;
    private static final int DEC_ARCSEC_PER_HR_POS=7;
    private static final int CNT_DEST_ARCSEC_POS=  8;
    private static final int PS_ANG_DEG_POS=       9;
    private static final int ARC_SPAN_POS=        10;
    private static final int NOBS_POS=            11;
    private static final int SMAA_ERR_POS=        12;
    private static final int SMIA_ERR_POS=        13;
    private static final int THEATA_ERR_POS=      14;

    private static final DataType _extraData[];
    private static final Map<Integer,DataType> _useMap=
                                  new HashMap<Integer,DataType>();


    static {
        DataType id= new DataType("id", _prop.getColumnName("id"),
                                       String.class);
        DataType amag= new DataType("amag",
                                    _prop.getColumnName("amag"),
                                    Float.class);
        DataType raArcSecPerHr= new DataType("raArcSecPerHr",
                             _prop.getColumnName("raArcSecPerHr"),
                             Float.class);
        DataType decArcSecPerHr= new DataType("decArcSecPerHr",
                             _prop.getColumnName("decArcSecPerHr"),
                                              Float.class);
        DataType arcSpan= new DataType("arcSpan",
                                       _prop.getColumnName("arcSpan"),
                                       String.class);
        DataType nobs= new DataType("nobs",
                                    _prop.getColumnName("nobs"),
                                    String.class);
        DataType theata= new DataType("theata",
                                      _prop.getColumnName("theata"),
                                      String.class);
        _useMap.put(ID_POS, id);
        _useMap.put(AMAG_POS, amag);
        _useMap.put(RA_ARCSEC_PER_HR_POS,  raArcSecPerHr);
        _useMap.put(DEC_ARCSEC_PER_HR_POS, decArcSecPerHr);
        _useMap.put(ARC_SPAN_POS, arcSpan);
        _useMap.put(NOBS_POS, nobs);
        _useMap.put(THEATA_ERR_POS, theata);
        _extraData= new DataType[]  { id, amag, raArcSecPerHr, decArcSecPerHr,
                                       arcSpan, nobs, theata};
    }






    public static byte[] lowlevelGetIspy(HorizonsIspyParams params)
                                         throws IOException,
                                                FailedRequestException {

        HostPort server= NetworkManager.getInstance().getServer(
                                              NetworkManager.HORIZONS_NAIF);



        ClientLog.message("Retrieving IspyGetter file");
        String urlStr=  "http://" +
                 server.getHost() + ":" + server.getPort() + CGI_CMD;
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(buildParamString(params));
        wr.flush();
        wr.close();

        byte data[]= URLDownload.getDataFromOpenURL(conn, true, null);

        ClientLog.message("Done");
        return data;
    }

    private static String buildParamString(HorizonsIspyParams params)
                   throws FailedRequestException {

        StringBuffer data=new StringBuffer(100);
        String retval= null;
        try {
            data.append("TYPE= '2',");
            data.append("SPKID= '");
            data.append(params.getObserver());
            data.append("',");
            data.append("FOV_DATE= '");
            data.append(params.getEpochinDateStr());
            data.append("',");
            data.append("RA(1)= '");
            data.append(params.getRaJ2000());
            data.append("',");
            data.append("DEC(1)= '");
            data.append(params.getDecJ2000());
            data.append("',");
            data.append("RADIUS= '");
            data.append(params.getRadiusInArcsec());
            data.append("',");

            retval= URLEncoder.encode("varlist", "UTF-8") + "=" +
                    URLEncoder.encode(data.toString(), "UTF-8");


        } catch (Exception e) {
            throw new FailedRequestException("Could not encode query",
                                             null, e);
        }
        return retval;
    }





    public static FixedObjectGroup parseIspyData(byte data[])
                                             throws ParseException {


        String str= null;
        FixedObjectGroup retval= null;
        try {
            BufferedReader in=new BufferedReader(new InputStreamReader(
                                           new ByteArrayInputStream(data)));
            str=in.readLine();
            int objCnt= -1;
            boolean found=false;
            for(str=in.readLine(); (str!=null && !found); str=in.readLine()) {
                if(str.indexOf("Number of objects")>-1) {
                    String s[]=str.split(":");
                    if(s.length!=2) {
                        throw new IOException("parsing objCnt: "+str);
                    }
                    objCnt=Integer.parseInt(StringUtil.crunch(s[1]));
                    System.out.println("objCnt= "+objCnt);
                    found=true;
                }
		else if (str.indexOf("No asteroids were found") > -1) {
		    objCnt = 0;
		}
            }

            if (objCnt==0) return null;
	    if (objCnt == -1)
		throw new ParseException(
		    "Unexpected output from Horizons\n" +
		    "Please contact the help desk (help@spitzer.caltech.edu)");


            List<FieldBounds> boundsList= findBounds(in);

            String values[]= new String[boundsList.size()];

            int i=0;
            found=false;
            FieldBounds bounds;
            DataGroup dataGroup= new DataGroup("IspyGetter Data", _extraData);
            retval= new FixedObjectGroup("IspyGetter Data", dataGroup);
            for(str=in.readLine(); (str!=null && i<objCnt);
                                                 str=in.readLine(), i++) {
                System.out.println("parsing= "+str);
                for(int j=0; j<values.length; j++) {
                    try {
                        bounds= boundsList.get(j);
                        values[j]=str.substring(bounds._firstIdx,
                                                bounds._lastIdx+1);
                    } catch (IndexOutOfBoundsException e) {
                        values[j]=null;
                    }
                }
                retval.add(makeFixedObject(values, retval));
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new ParseException("Could not parse line: "+str, e);
        } catch (CoordException e) {
            e.printStackTrace();
            throw new ParseException("Could not parse line: "+str, e);
        }
        return retval;
    }


    private static List<FieldBounds> findBounds(BufferedReader in)
                                                 throws IOException {
        boolean found=false;
        String str;
        List<FieldBounds>  boundsList= new ArrayList<FieldBounds>(15);
        for(str=in.readLine(); (str!=null && !found);) {
            if(str.startsWith("------")) {
                found= true;
                int len= str.length();
                int start= -1;
                int stop= -1;
                for(int i=0; i<len; i++) {
                    if (start<0 && str.charAt(i)=='-') start= i;
                    if (start>-1 && str.charAt(i)!='-')  stop= i-1;

                    if(stop>-1 && start>-1) {
                        boundsList.add(new FieldBounds(start, stop));
                        start=-1;
                        stop=-1;
                    }
                }
                if(start>-1) {
                    boundsList.add(new FieldBounds(start, len-1));
                }
                if(boundsList.size()<3) {
                    throw new IOException( "parsing field sizes: "+str);
                }
                System.out.println("found= "+str);
                found=true;
            }
            else {
                str=in.readLine();
            }
        }
        return boundsList;
    }

    private static FixedObject makeFixedObject(String values[],
                                               FixedObjectGroup group)
                                               throws CoordException {
        UserPosition up= new UserPosition(
                       values[RA_POS], values[DEC_POS],
                       PositionJ2000.DEFAULT_PM,
                       CoordinateSys.EQ_J2000, Position.EPOCH2000);
        WorldPt  wp= new WorldPt(up.getLon(), up.getLat());
        FixedObject fixO= group.makeFixedObject(wp);
        fixO.setTargetName(StringUtil.crunch(values[NAME_POS]));

        addField( ID_POS, fixO, values);
        addField( IAU_POS, fixO, values);
//        addField( NAME_POS, fixO, values);
//        addField( RA_POS, fixO, values);
//        addField( DEC_POS, fixO, values);
        addField( AMAG_POS, fixO, values);
        addField( RA_ARCSEC_PER_HR_POS, fixO, values);
        addField( DEC_ARCSEC_PER_HR_POS, fixO, values);
        addField( CNT_DEST_ARCSEC_POS, fixO, values);
        addField( PS_ANG_DEG_POS, fixO, values);
        addField( ARC_SPAN_POS, fixO, values);
        addField( NOBS_POS, fixO, values);
        addField( SMAA_ERR_POS, fixO, values);
        addField( SMIA_ERR_POS, fixO, values);
        addField( THEATA_ERR_POS, fixO, values);
        System.out.println("parsed: "+fixO.toString());
        return fixO;
    }

    private static void addField(int fldIdx, FixedObject fixO, String values[]){
        DataType extraData;
        if (fldIdx<values.length) {
            extraData= (DataType)_useMap.get(new Integer(fldIdx));
            if (extraData!=null) {
                Object o= extraData.convertStringToData(values[fldIdx]);
                fixO.setExtraData(extraData, o);
            }
        }

    }


    public static void main(String args[]) {
        try {
            SimpleDateFormat df=  new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
            Date epoch= df.parse("2004-Jan-21 13:20:00");
            HorizonsIspyParams params= new HorizonsIspyParams(epoch,
                                          180.717D, -0.875D,600,
                                          HorizonsIspyParams.SIRTF);
            byte data[]= lowlevelGetIspy(params);
            FixedObjectGroup fixedGroup= parseIspyData(data);
            FileOutputStream out= new FileOutputStream(
                               new File("IspyGetter-data.dat"));
            out.write(data);
        } catch (Exception e) {
            System.out.println("e= " + e.toString());
            e.printStackTrace();
        }
    }


    private static class FieldBounds {
        int _firstIdx;
        int _lastIdx;
        FieldBounds(int firstIdx, int lastIdx) {
            _firstIdx= firstIdx;
            _lastIdx = lastIdx;
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
