package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.action.ClassProperties;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Booth Hartley
 * @version $Id: HorizonsEphPairs.java,v 1.12 2012/03/13 22:23:02 roby Exp $
 */
public class HorizonsEphPairs extends ThreadedService {

    private static final ClassProperties _prop = new ClassProperties(
            HorizonsEphPairs.class);
    private static final String OP_DESC = _prop.getName("desc");

    private static final String NAME_IDENT = "Object Name";
    private static final String ID_IDENT = "Primary SPKID";
    private static final String DES_IDENT = "Primary designation";
    private static final String ALIAS_IDENT = "Aliases";

    //    private static final String CGI_CMD= "/cgi-bin/smb_spk.cgi";
    private static final String CGI_CMD = "/x/smb_spk.cgi";


    private HorizonsResults _out[] = null;
    private String _idOrName;

    private HorizonsEphPairs(String idOrName, boolean showError, Window w) {
        super(w);
        if (!showError) {
            setShowUserErrors(false);
            setShowNetworkDownError(false);
            setProceedIfNetworkDown(true);
        }
        _idOrName = idOrName;
        setOperationDesc(OP_DESC);
    }

    protected void doService() throws Exception {
        _out = lowlevelGetEphInfo(_idOrName, this);
    }


    public static HorizonsResults[] getEphInfo(String idOrName, Window w)
            throws FailedRequestException {
        return getEphInfo(idOrName, true, w);
    }

    public static HorizonsResults[] getEphInfo(String idOrName,
                                               boolean showError,
                                               Window w)
            throws FailedRequestException {
        HorizonsEphPairs action = new HorizonsEphPairs(idOrName, showError, w);
        action.execute();
        return action._out;
    }


    public static HorizonsResults[] lowlevelGetEphInfo(String idOrName,
                                                       ThreadedService ts)
            throws FailedRequestException {

        boolean isName = true;
        HorizonsResults retval[] = null;
        try {
            Integer.parseInt(idOrName);
            isName = false;
        } catch (NumberFormatException e) {
        }

        if (isName) {
            idOrName = StringUtil.crunch(idOrName);
            String s[] = idOrName.split(" ");
            if (s.length >= 2) {
                try {
                    Integer.parseInt(s[0]);
                    s = idOrName.split("^[0-9]* "); // now get the rest
                    if (inParens(s[1])) {
                        idOrName = stripFirstLast(s[1]);
                    } else {
                        char cAry[] = s[1].toCharArray();
                        boolean hasDigit = false;
                        for (int i = 0; (i < cAry.length && !hasDigit); i++) {
                            hasDigit = Character.isDigit(cAry[i]);
                        }
                        if (!hasDigit) idOrName = s[1];
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        HostPort server = NetworkManager.getInstance().getServer(
                NetworkManager.HORIZONS_NAIF);

        StringBuffer data = new StringBuffer(100);

        try {
            data.append(URLEncoder.encode("OPTION", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode("Look up", "UTF-8"));
            data.append("&");
            data.append(URLEncoder.encode("OBJECT", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(idOrName, "UTF-8"));


            //String data = URLEncoder.encode("OBJECT", "UTF-8") + "=" +
            //              URLEncoder.encode(idOrName, "UTF-8");
            String urlStr = "http://" +
                    server.getHost() + ":" + server.getPort() + CGI_CMD;
            URL url = new URL(urlStr);
            String line;
            String result = URLDownload.getStringFromURLUsingPost(url, data.toString(), ts);
            BufferedReader rd = new BufferedReader(new StringReader(result));


            List<String> list = new ArrayList<String>(12);
            boolean error = false;
            while ((line = rd.readLine()) != null) {
                list.add(line);
                if (line.indexOf("ERROR") > -1) error = true;
            }
            rd.close();


            if (error) {
                StringBuffer errStrBuff = new StringBuffer(100);
                errStrBuff.append("<html>");
                for (String s : list) errStrBuff.append(s);
                throw new FailedRequestException("<html>" +
                                                         "<br><br><b>ERROR:</b><br><br>" +
                                                         "No matches found for: " + idOrName);
            }

            retval = buildHorizonsResults(list);
        } catch (IOException e) {
            throw new FailedRequestException("failed to retrieve ephermeris pairs",
                                             "IOException in getting ephermeris info", e);
        }
        return retval;
    }


    private static String appendAll(List l) {
        StringBuffer buff = new StringBuffer(100);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            buff.append(i.next().toString());
            buff.append("\n");
        }
        return buff.toString();
    }


    private static HorizonsResults[] buildHorizonsResults(List<String> list)
            throws FailedRequestException {

        String label;
        String sAry[];
        String name = null;
        String id = null;
        String des = null;
        String alias = null;
        String aliases[] = null;
        List retList = new ArrayList(4);
        for (String s : list) {
            if (s.length() == 0) {

                if (name == null || id == null || des == null || aliases == null) {
                    throw new FailedRequestException(
                            _prop.getError("parse"),
                            "One of the fields was null.\n" +
                                    "Results from query:\n" +
                                    appendAll(list));
                } else {
                    if (StringUtils.isEmpty(name)) {
                        name = StringUtils.isEmpty(des) ? "No Name*" : des;
                    }
                    retList.add(new HorizonsResults(name, id, des, aliases));
                }
                name = null;
                id = null;
                des = null;
                alias = null;
            } else {
                sAry = s.split("=");
                if (sAry.length != 2) {
                    throw new FailedRequestException(
                            _prop.getError("parse"),
                            "Missing a value pair combination\n" +
                                    "Results from query:\n" +
                                    appendAll(list));
                }

                label = StringUtil.crunch(sAry[0]);
                if (label.equals(NAME_IDENT)) {
                    name = StringUtil.crunch(sAry[1]);
                    if (StringUtils.isEmpty(name)) {
                        name = "";
                    } else if (inParens(name)) {
                        name = stripFirstLast(name);
                    }
                } else if (label.equals(ID_IDENT)) {
                    id = StringUtil.crunch(sAry[1]);
                } else if (label.equals(DES_IDENT)) {
                    des = StringUtil.crunch(sAry[1]);
                } else if (label.equals(ALIAS_IDENT)) {
                    alias = StringUtil.crunch(sAry[1]);
                    if (alias.length() == 0) {
                        aliases = new String[0];
                    } else {
                        aliases = alias.split(",");
                        for (int k = 0; k < aliases.length; k++) {
                            aliases[k] = aliases[k].trim();
                        }
                    }
                } else {
                    // if not one of the four ignore this line
                }
            }
        } // end loop
        if (retList.size() == 0) {
            throw new FailedRequestException(
                    _prop.getError("parse"),
                    "No error indicated but no results\n" +
                            "Results from query:\n" +
                            appendAll(list));
        }

        return (HorizonsResults[]) retList.toArray(new HorizonsResults[0]);
    }


    public static class HorizonsResults implements Serializable {
        private final String _name;
        private final String _naifID;
        private final String _primaryDes;
        private final String _aliases[];

        public HorizonsResults(String name, String naifID, String primaryDes,
                               String aliases[]) {
            _name = name;
            _naifID = naifID;
            _primaryDes = primaryDes;
            _aliases = aliases;
        }

        public final String getName() {
            return _name;
        }

        public final String getNaifID() {
            return _naifID;
        }

        public final String getPrimaryDes() {
            return _primaryDes;
        }

        public final String[] getAliases() {
            return _aliases;
        }

        public String toString() {
            return _name + "     " + _naifID + "     " + _primaryDes;
        }
    }


    public static void main(String args[]) {
        String name;
        System.out.println("args.length = " + args.length);
        if (args.length > 0) {
            name = args[0];
            ;
        } else {
            name = "2150642";
        }
        System.out.println("name = " + name);

        try {
            HorizonsResults[] horizons_results =
                    lowlevelGetEphInfo(name, null);
            //printOut(horizons_results);
            for (int i = 0; i < horizons_results.length; i++) {
                System.out.println("Result # " + i + ":");
                HorizonsResults horizons_result = horizons_results[i];
                System.out.println("getName() =  " + horizons_result.getName());
                System.out.println("getNaifID() =  " + horizons_result.getNaifID());
                System.out.println("getPrimaryDes() =  " + horizons_result.getPrimaryDes());
                String aliases[] = horizons_result.getAliases();

                System.out.println("RBH  aliases.length = " + aliases.length);
                for (int k = 0; k < aliases.length; k++) {
                    System.out.println("RBH  aliases[" + k + "]  = ["
                                               + aliases[k] + "]");
                }
            }
            //printOut(lowlevelGetEphInfo("vesta",null));
            //printOut(lowlevelGetEphInfo("halley",null));
            //printOut(lowlevelGetEphInfo("2000002",null));

        } catch (Exception e) {
            System.out.println("e= " + e.toString());
        }
    }

    private static void printOut(HorizonsResults res[]) {
        for (int i = 0; (i < res.length); i++) {
            System.out.println(res[i]);
        }
    }

    private static boolean inParens(String s) {
        boolean retval = false;
        if (!StringUtils.isEmpty(s)) {
            retval = (s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')');
        }
        return retval;
    }

    private static String stripFirstLast(String s) {
        return s.substring(1, s.length() - 1);
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
