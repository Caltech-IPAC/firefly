/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.rpc.SearchServicesImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Trey Roby
 */
public class SearchServerCommands {




    public static abstract class BaseSearchServerCommand extends ServCommand {
        public boolean getCanCreateJson() { return true; }
    }

    public static class TableSearch extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            TableServerRequest tsr = sp.getTableServerRequest();
            DataGroupPart dgp = new SearchManager().getDataGroup(tsr);
            JSONObject json = JsonTableUtil.toJsonTableModel(dgp, tsr);
            return json.toJSONString();
        }
    }

    public static class SelectedValues extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String filePath = sp.getRequired("filePath");
            try {
                String selRows = sp.getRequired("selectedRows");
                String columnName = sp.getRequired("columnName");
                List<Integer> rows = StringUtils.convertToListInteger(selRows, ",");
                List<String> values =  new SearchManager().getDataFileValues(ServerContext.convertToFile(filePath), rows, columnName);
                Map<String, List<String>> rval = new HashMap<>(1);
                rval.put("values", values);
                return QueryUtil.toJsonObject(rval).toJSONString();
            } catch (IOException e) {
                throw new DataAccessException("Unable to resolve a search processor for this request.  SelectedValues aborted.");
            }
        }
    }

    public static class JsonSearch extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            TableServerRequest tsr = sp.getTableServerRequest();
            SearchProcessor processor = new SearchManager().getProcessor(tsr.getRequestId());
            if (processor instanceof JsonDataProcessor) {
                return  ((JsonDataProcessor)processor).getData(tsr);
            } else {
                throw new DataAccessException("Unable to resolve a search processor for this request.  Operation aborted:" + tsr.getRequestId());
            }
        }
    }

    public static class GetJSONData extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String reqString = sp.getRequired(ServerParams.REQUEST);
            ServerRequest request = ServerRequest.parse(reqString, new ServerRequest());
            return new SearchManager().getJSONData(request);
        }

    }

    public static class PackageRequest extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String tableReqStr = sp.getRequired(ServerParams.REQUEST);
            String selInfoStr = sp.getOptional(ServerParams.SELECTION_INFO);
            String dlReqStr = sp.getOptional(ServerParams.DOWNLOAD_REQUEST);

            DownloadRequest dlreq = QueryUtil.convertToDownloadRequest(dlReqStr, tableReqStr, selInfoStr);
            SearchManager sman = new SearchManager();
            SearchProcessor processor = sman.getProcessor(dlreq.getRequestId());
            if (processor instanceof FileGroupsProcessor) {
                BackgroundStatus bgStatus = sman.packageRequest(dlreq);
                bgStatus.setParam(ServerParams.TITLE, dlreq.getTitle());
                return QueryUtil.convertToJsonObject(bgStatus).toJSONString();
            } else {
                throw new DataAccessException("Unable to resolve a search processor for this request.  Operation aborted:" + dlreq.getRequestId());
            }
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

    public static class RemoveBgJob extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            BackgroundEnv.removeUserBackgroundInfo(sp.getID());
            return "true";
        }
    }

    public static class Cancel extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            BackgroundEnv.cancel(sp.getID());
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
        public boolean getCanCreateJson() { return false; }

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            SearchServices.DownloadProgress dp= new SearchServicesImpl().getDownloadProgress(file);
            return dp.toString();
        }
    }

    public static class SetEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList = sp.getIDList();
            String email = sp.getRequired(ServerParams.EMAIL);
            BackgroundEnv.setEmail(idList,email);
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
            BackgroundEnv.setAttribute(idList,att);
            return "true";
        }
    }

    public static class GetEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            String id = new SrvParam(paramMap).getID();
            return BackgroundEnv.getEmail(id);
        }
    }

    public static class ResendEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String email= sp.getRequired(ServerParams.EMAIL);
            List<String> idList = BackgroundEnv.getUserBackgroundInfo().stream()
                                  .filter( (bic) -> bic.isSuccess())
                                  .map( (bic) -> bic.getBID() )
                                  .collect(Collectors.toList());
            BackgroundEnv.resendEmail(idList,email);
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
        public boolean getCanCreateJson() { return false; }

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
            BackgroundEnv.ScriptRet retval= BackgroundEnv.createDownloadScript(id, file, source, attList);
            return retval!=null ? retval.getServlet() : null;
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

    @Deprecated
    public static class GetRawDataSet extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String reqString = sp.getRequired(ServerParams.REQUEST);
            TableServerRequest request = TableServerRequest.parse(reqString);
            RawDataSet dataSet = new SearchManager().getRawDataSet(request);
            return dataSet.serialize();
        }

    }


}

