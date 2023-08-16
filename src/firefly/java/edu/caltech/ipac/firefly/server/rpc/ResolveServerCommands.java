/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;


import edu.caltech.ipac.astro.net.HorizonsEphPairs;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.Arrays;

import static edu.caltech.ipac.firefly.server.servlets.AnyFileUpload.ANALYZER_ID;

/**
 * @author Trey Roby
 */
public class ResolveServerCommands {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static class ResolveName extends ServCommand {

        public String doCommand(SrvParam sp) throws Exception {

            Resolver resolver= Resolver.NedThenSimbad;
            String name = sp.getRequired(ServerParams.OBJ_NAME);
            String resStr = sp.getOptional(ServerParams.RESOLVER);
            JSONArray wrapperAry= new JSONArray();
            JSONObject result = new JSONObject();

            try {
                resolver= (resStr!=null) ? Resolver.parse(resStr) : Resolver.NedThenSimbad;
                if (resolver==null) resolver= Resolver.NedThenSimbad;

                ResolvedWorldPt wp= TargetNetwork.resolveToWorldPt(name,resolver);
                if (wp==null) throw new Exception("Could not resolve");
                result.put("data", wp.toString());
                result.put("success", "true");
                wrapperAry.add(result);

            } catch (Exception e) {
                logger.error("Could not resolve object name: "+ sp.getRequired(ServerParams.OBJ_NAME) + " using " + resolver.toString());

                result.put("success", "false");
                result.put("error", e.getMessage());
                wrapperAry.add(result);
            }

            return wrapperAry.toJSONString();
        }
        public boolean getCanCreateJson() { return true; }
    }


    public static class ResolveNaifidName extends ServCommand {


        public String doCommand(SrvParam sp) throws Exception {
            JSONArray wrapperAry= new JSONArray();
            JSONObject result = new JSONObject();

            try {
                HorizonsEphPairs.HorizonsResults[] horizons_results = TargetNetwork.getEphInfo(sp.getRequired(ServerParams.OBJ_NAME));
                String naifIdFormat = sp.getOptional(ServerParams.NAIFID_FORMAT, "");

                JSONArray dataAry= new JSONArray();
                for (HorizonsEphPairs.HorizonsResults element : horizons_results) {
                    String naifID = element.getNaifID();

                    if (naifIdFormat.equals("7digit") && naifID.length() > 7) {
                        for (String alias : element.getAliases()) {
                            try {
                                Integer.parseInt(alias);
                                if (naifID.equals(alias.charAt(0) + "0" + alias.substring(1))) {
                                    naifID = alias;
                                    break;
                                }
                            } catch (NumberFormatException ignore) { }
                        }
                    }

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("name", element.getName());
                    dataObj.put("naifID", Integer.parseInt(naifID));
                    dataAry.add(dataObj);
                }

                result.put("data", dataAry);
                result.put("success", true);
                wrapperAry.add(result);

            }catch (Exception e){
                logger.error("Could not resolve object name: "+ sp.getRequired(ServerParams.OBJ_NAME));

                result.put("success", "false");
                result.put("error", e.getMessage());
                wrapperAry.add(result);

            }
            return wrapperAry.toJSONString();
        }

        public boolean getCanCreateJson() { return true; }
    }


    public static class FileAnalysisCmd extends ServCommand {


        public String doCommand(SrvParam sp) throws Exception {

            String infile = null;
            try {
                infile = sp.getRequired("filePath");
                String rtype = sp.getOptional("reportType");
                FileAnalysisReport.ReportType reportType = StringUtils.isEmpty(rtype) ? FileAnalysisReport.ReportType.Brief : FileAnalysisReport.ReportType.valueOf(rtype);   // defaults to Brief

                FileAnalysisReport report = FileAnalysis.analyze(
                        new FileInfo(new File(infile)), reportType,
                        sp.getOptional(ANALYZER_ID),
                        sp.getParamMapUsingExcludeList(Arrays.asList("filePath","reportType")));
                return FileAnalysis.toJsonString(report);
            }catch (Exception e){
                throw new Exception("Fail to analyze file: "+ infile);
            }
        }
    }

}

