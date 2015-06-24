/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.rpc.SearchServicesImpl;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class SearchServerCommands {

    public static abstract class BaseSearchServerCommand extends ServerCommandAccess.ServCommand {
        public boolean getCanCreateJson() { return false; }
    }

    public static class GetRawDataSet extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String reqString = sp.getRequired(ServerParams.REQUEST);
            TableServerRequest request = TableServerRequest.parse(reqString);
            RawDataSet dataSet = new SearchManager().getRawDataSet(request);
            return dataSet.serialize();
        }

    }

    public static class GetJSONData extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String reqString = sp.getRequired(ServerParams.REQUEST);
            ServerRequest request = ServerRequest.parse(reqString, new ServerRequest());
            String data = new SearchManager().getJSONData(request);
            return data;
        }

    }

    public static class ChkFileStatus extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            File f= new File(sp.getRequired(ServerParams.SOURCE));
            FileStatus fstatus = new SearchManager().getFileStatus(f);
            return fstatus.toString();
        }

    }

    public static class GetEnumValues extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            File f= new File(sp.getRequired(ServerParams.SOURCE));
            RawDataSet data = new SearchManager().getEnumValues(f);
            return data.serialize();
        }

    }


    public static class SubmitBackgroundSearch extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String servReqStr = sp.getRequired(ServerParams.REQUEST);
            int waitMil = sp.getRequiredInt(ServerParams.WAIT_MILS);

            TableServerRequest serverRequest= ServerRequest.parse(servReqStr, new TableServerRequest());
            Request clientRequest= null;
            if (sp.contains(ServerParams.CLIENT_REQUEST))  {
                clientRequest= ServerRequest.parse(sp.getOptional(ServerParams.CLIENT_REQUEST),
                                                   new Request());
            }
            BackgroundStatus bgStat= new SearchServicesImpl().submitBackgroundSearch(
                                                   serverRequest,clientRequest,waitMil);
            return bgStat.serialize();
        }
    }

    public static class GetStatus extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            BackgroundStatus bgStat= new SearchServicesImpl().getStatus(sp.getID(),
                                                                        sp.getRequiredBoolean(ServerParams.POLLING));
            return bgStat.serialize();
        }
    }

    public static class AddIDToPushCriteria extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            new SearchServicesImpl().addIDToPushCriteria(sp.getID());
            return "true";
        }
    }

    public static class Cancel extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            new SearchServicesImpl().cancel(sp.getID());
            return "true";
        }
    }

    public static class CleanUp extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            new SearchServicesImpl().cleanup(sp.getID());
            return "true";
        }
    }

    public static class DownloadProgress extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            SearchServices.DownloadProgress dp= new SearchServicesImpl().getDownloadProgress(file);
            return dp.toString();
        }
        public boolean getCanCreateJson() { return false; }
    }

    public static class SetEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList = sp.getIDList();
            String email = sp.getRequired(ServerParams.EMAIL);
            new SearchServicesImpl().setEmail(idList,email);
            return "true";
        }
    }

    public static class SetAttribute extends BaseSearchServerCommand  {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList= sp.getIDList();
            String attStr= sp.getRequired(ServerParams.ATTRIBUTE);
            JobAttributes att= StringUtils.getEnum(attStr, JobAttributes.Unknown);
            new SearchServicesImpl().setAttribute(idList, att);
            return "true";
        }
    }

    public static class GetEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            String id = new SrvParam(paramMap).getID();
            return new SearchServicesImpl().getEmail(id);
        }
    }

    public static class ResendEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList = sp.getIDList();
            String email= sp.getRequired(ServerParams.EMAIL);
            new SearchServicesImpl().resendEmail(idList, email);
            return "true";
        }
    }

    public static class ClearPushEntry extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String id= sp.getRequired(ServerParams.ID);
            int idx=   sp.getRequiredInt(ServerParams.IDX);
            new SearchServicesImpl().clearPushEntry(id,idx);
            return "true";
        }
    }

    public static class ReportUserAction extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String channel= sp.getRequired(ServerParams.CHANNEL_ID);
            String desc= sp.getRequired(ServerParams.DESC);
            String data= sp.getRequired(ServerParams.DATA);
            new SearchServicesImpl().reportUserAction(channel,desc,data);
            return "true";
        }
    }

    public static class CreateDownloadScript extends BaseSearchServerCommand  {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String id = sp.getID();
            String file = sp.getRequired(ServerParams.FILE);
            String source = sp.getRequired(ServerParams.SOURCE);
            List<String> attStrList = sp.getOptionalList(ServerParams.ATTRIBUTE);
            List<ScriptAttributes> attList= new ArrayList<ScriptAttributes>(5);
            for(String a : attStrList) {
                attList.add(Enum.valueOf(ScriptAttributes.class,a));
            }
            return new SearchServicesImpl().createDownloadScript(id,file,source,attList);
        }
    }

    public static class GetDataFileValues extends BaseSearchServerCommand {

        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String filePath = sp.getRequired(ServerParams.SOURCE);
            String rowsStr = sp.getRequired(ServerParams.ROWS);
            List<Integer> rows = new ArrayList<Integer>();
            for (String s : rowsStr.split(", ")) {
                rows.add(Integer.parseInt(s));
            }
            String colName = sp.getRequired(ServerParams.COL_NAME);
            List<String> result = (new SearchManager().getDataFileValues(new File(filePath), rows, colName));
            return CollectionUtil.toString(result);
        }
    }
}

