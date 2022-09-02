/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HorizonsEphPairs {

    public static final String horizonsServer = AppProperties.getProperty("horizons.host", "https://ssd.jpl.nasa.gov");
    public static final String path= "/api/horizons_lookup.api";

    public static HorizonsResults[] lowlevelGetEphInfo(String idOrName) throws FailedRequestException {

        boolean isName = true;
        try {
            Integer.parseInt(idOrName);
            isName = false;
        } catch (NumberFormatException ignore) { }

        if (isName) {
            idOrName = StringUtils.crunch(idOrName);
            String[] s = idOrName.split(" ");
            if (s.length >= 2) {
                try {
                    Integer.parseInt(s[0]);
                    s = idOrName.split("^[0-9]* "); // now get the rest
                    if (inParens(s[1])) {
                        idOrName = stripFirstLast(s[1]);
                    }
                        //do not delete this else block, might need it later to deal with inputs like '2020 Ukko'
                        //else {
                        //char[] cAry = s[1].toCharArray();
                        //boolean hasDigit = false;
                        //for (int i = 0; (i < cAry.length && !hasDigit); i++) {
                        //    hasDigit = Character.isDigit(cAry[i]);
                        //}
                        //if (!hasDigit) idOrName = s[1];
                    //}
                } catch (NumberFormatException ignore) {
                }
            }
        }


        try {
            String urlStr = horizonsServer + path + "?sstr="+ URLEncoder.encode(idOrName, "UTF-8");
            URL url = new URL(urlStr);
            String jsonStrResult = URLDownload.getDataFromURL(url, null,null,null).getResultAsString();
            JsonHelper json= JsonHelper.parse(jsonStrResult);

            JSONArray rList= json.getValue(new JSONArray(), "result");
            List<HorizonsResults> horizonsResults= new ArrayList<>();
            for(Object r : rList) {
                String pdes= (String)((JSONObject)r).get("pdes");
                String name= (String)((JSONObject)r).get("name");
                String naifID= (String)((JSONObject)r).get("spkid");
                JSONArray aList = (JSONArray)((JSONObject)r).get("alias");
                String[] aStrAry= (String[])aList.toArray(new String[0]);
                horizonsResults.add(new HorizonsResults(name,naifID,pdes,aStrAry));
            }
            return horizonsResults.toArray(new HorizonsResults[0]);
        } catch (IOException e) {
            throw new FailedRequestException("failed to retrieve ephemeris pairs",
                                             "IOException in getting ephemeris info", e);
        }
    }



    public static class HorizonsResults implements Serializable {
        private final String name;
        private final String naifID;
        private final String primaryDes;
        private final String [] aliases;

        public HorizonsResults(String name, String naifID, String primaryDes, String[] aliases) {
            this.name = name;
            this.naifID = naifID;
            this.primaryDes = primaryDes;
            this.aliases = aliases;
        }

        public final String getName() { return name; }
        public final String getNaifID() { return naifID; }
        public final String getPrimaryDes() { return primaryDes; }
        public final String[] getAliases() { return aliases; }
        public String toString() { return name + "     " + naifID + "     " + primaryDes; }
    }


    public static void main(String args[]) {
        String name;
        System.out.println("args.length = " + args.length);
        if (args.length > 0) {
            name = args[0];
            ;
        } else {
            name = "jupiter";
        }
        System.out.println("name = " + name);

        try {
            HorizonsResults[] horizons_results =
                    lowlevelGetEphInfo(name);
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
