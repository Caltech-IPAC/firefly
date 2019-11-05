/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;


import edu.caltech.ipac.astro.net.HorizonsEphPairs;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ResolveServerCommands {

    public static class ResolveName extends ServCommand {

        public String doCommand(SrvParam sp) throws Exception {

            Resolver resolver= Resolver.NedThenSimbad;
            String name = sp.getRequired(ServerParams.OBJ_NAME);
            String resStr = sp.getOptional(ServerParams.RESOLVER);
            try {
                resolver= (resStr!=null) ? Resolver.parse(resStr) : Resolver.NedThenSimbad;
                if (resolver==null) resolver= Resolver.NedThenSimbad;
                ResolvedWorldPt wp= new TargetServicesImpl().resolveName(name, resolver);
                return wp.toString();
            } catch (Exception e) {
                throw new Exception("Could not resolve " +  name + " using " + resolver.toString());
            }
        }

        public boolean getCanCreateJson() { return false; }
    }

    public static class ResolveNaifidName extends ServCommand {


        public String doCommand(SrvParam sp) throws Exception {

            try {
                HorizonsEphPairs.HorizonsResults[] horizons_results = HorizonsEphPairs.lowlevelGetEphInfo(sp.getRequired(ServerParams.OBJ_NAME));

                Map<String, Integer> values = new HashMap<>();
                for (HorizonsEphPairs.HorizonsResults element : horizons_results) {
                    values.put(element.getName(), Integer.parseInt(element.getNaifID()));
                }

                JSONObject jsonObj = new JSONObject(values);


                JSONArray wrapperAry= new JSONArray();
                JSONObject result = new JSONObject();
                result.put("data", jsonObj);
                result.put("success", true);
                wrapperAry.add(result);


                return wrapperAry.toJSONString();

            }catch (Exception e){
                throw new Exception("Could not resolve object name: "+ sp.getRequired(ServerParams.OBJ_NAME));
            }
        }

        public boolean getCanCreateJson() { return true; }
    }


    public static class FileAnalysisCmd extends ServCommand {


        public String doCommand(SrvParam sp) throws Exception {

            String infile = null;
            try {
                infile = sp.getRequired("filePath");
                String rtype = sp.getOptional("reportType");
                FileAnalysis.ReportType reportType = StringUtils.isEmpty(rtype) ? FileAnalysis.ReportType.Brief : FileAnalysis.ReportType.valueOf(rtype);   // defaults to Brief
                FileAnalysis.Report report = FileAnalysis.analyze(new File(infile), reportType);
                return FileAnalysis.toJsonString(report);
            }catch (Exception e){
                throw new Exception("Fail to analyze file: "+ infile);
            }
        }
    }

}

