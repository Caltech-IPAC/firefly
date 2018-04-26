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
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.packagedata.BackgroundInfoCacher;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Trey Roby
 */
public class SearchServerCommands {




    public static class TableSearch extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            TableServerRequest tsr = params.getTableServerRequest();
            DataGroupPart dgp = new SearchManager().getDataGroup(tsr);
            JSONObject json = JsonTableUtil.toJsonTableModel(dgp, tsr);
            return json.toJSONString();
        }
    }

    public static class QueryTable extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            TableServerRequest treq = (TableServerRequest) params.getTableServerRequest().cloneRequest();
            treq.setParam(TableServerRequest.INCL_COLUMNS, params.getOptional(TableServerRequest.INCL_COLUMNS));
            treq.setFilters(StringUtils.asList(params.getOptional(TableServerRequest.FILTERS), ","));
            String sortInfo = params.getOptional(TableServerRequest.SORT_INFO);
            if (!StringUtils.isEmpty(sortInfo)) {
                treq.setSortInfo(SortInfo.parse(sortInfo));
            }

            DataGroupPart page = new SearchManager().getDataGroup(treq);
            return JsonTableUtil.toJsonTableModel(page, treq).toJSONString();
        }
    }

    public static class SelectedValues extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String requestJson = params.getRequired(ServerParams.REQUEST);
            TableServerRequest treq = QueryUtil.convertToServerRequest(requestJson);
            try {
                List<String> cols = StringUtils.asList(params.getRequired("columnNames"), ",");
                String[] colsAry = cols == null ? null : cols.toArray(new String[cols.size()]);
                List<Integer> rows = StringUtils.convertToListInteger(params.getRequired("selectedRows"), ",");
                DataGroupPart page = EmbeddedDbUtil.getSelectedDataAsDGPart(treq, rows, colsAry);
                return JsonTableUtil.toJsonTableModel(page, treq).toJSONString();
            } catch (IOException e) {
                throw new DataAccessException("Unable to resolve a search processor for this request.  SelectedValues aborted.");
            }
        }
    }

    public static class JsonSearch extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            TableServerRequest tsr = params.getTableServerRequest();
            SearchProcessor processor = new SearchManager().getProcessor(tsr.getRequestId());
            if (processor instanceof JsonDataProcessor) {
                return  ((JsonDataProcessor)processor).getData(tsr);
            } else {
                throw new DataAccessException("Unable to resolve a search processor for this request.  Operation aborted:" + tsr.getRequestId());
            }
        }
    }

    public static class GetJSONData extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String reqString = params.getRequired(ServerParams.REQUEST);
            ServerRequest request = ServerRequest.parse(reqString, new ServerRequest());
            return new SearchManager().getJSONData(request);
        }

    }

    public static class PackageRequest extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String tableReqStr = params.getRequired(ServerParams.REQUEST);
            String selInfoStr = params.getOptional(ServerParams.SELECTION_INFO);
            String dlReqStr = params.getOptional(ServerParams.DOWNLOAD_REQUEST);

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

    public static class SubmitBackgroundSearch extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            TableServerRequest serverRequest = params.getTableServerRequest();
            int waitMil = params.getRequiredInt(ServerParams.WAIT_MILS);

            BackgroundStatus bgStat =  new SearchManager().submitBackgroundSearch(serverRequest, null, waitMil);
            return QueryUtil.convertToJsonObject(bgStat).toJSONString();
        }
    }

    public static class GetStatus extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            BackgroundStatus bgStat= BackgroundEnv.getStatus(params.getID(),
                    params.getRequiredBoolean(ServerParams.POLLING));
            return QueryUtil.convertToJsonObject(bgStat).toJSONString();
        }
    }

    public static class AddBgJob extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            BackgroundStatus bgStatus = QueryUtil.convertToBackgroundStatus(params.getRequired("bgStatus"));
            BackgroundEnv.addUserBackgroundInfo(bgStatus);
            return "true";
        }
    }

    public static class RemoveBgJob extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            BackgroundEnv.removeUserBackgroundInfo(params.getID());
            return "true";
        }
    }

    public static class Cancel extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            BackgroundEnv.cancel(params.getID());
            return "true";
        }
    }

    public static class CleanUp extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            BackgroundEnv.cleanup(params.getID());
            return "true";
        }
    }

    public static class DownloadProgress extends ServCommand {
        public boolean getCanCreateJson() { return false; }

        public String doCommand(SrvParam params) throws Exception {
            String file= params.getRequired(ServerParams.FILE);
            BackgroundEnv.DownloadProgress dp= BackgroundEnv.getDownloadProgress(file);
            return dp.toString();
        }
    }

    public static class SetEmail extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            List<String> idList = params.getIDList();
            String email = params.getRequired(ServerParams.EMAIL);
            BackgroundEnv.setEmail(idList,email);
            return "true";
        }
    }

    public static class SetAttribute extends ServCommand  {

        public String doCommand(SrvParam params) throws Exception {
            List<String> idList= params.getIDList();
            String attStr= params.getRequired(ServerParams.ATTRIBUTE);
            JobAttributes att= StringUtils.getEnum(attStr, JobAttributes.Unknown);
            BackgroundEnv.setAttribute(idList,att);
            return "true";
        }
    }

    public static class GetEmail extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String id = params.getID();
            return BackgroundEnv.getEmail(id);
        }
    }

    public static class ResendEmail extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String email= params.getRequired(ServerParams.EMAIL);
            List<String> idList = BackgroundEnv.getUserBackgroundInfo().stream()
                    .filter(BackgroundInfoCacher::isSuccess)
                    .map(BackgroundInfoCacher::getBID)
                    .collect(Collectors.toList());
            BackgroundEnv.resendEmail(idList,email);
            return "true";
        }
    }

    public static class ClearPushEntry extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String id= params.getRequired(ServerParams.ID);
            int idx=   params.getRequiredInt(ServerParams.IDX);
            BackgroundEnv.clearPushEntry(id,idx);
            return "true";
        }
    }

    public static class ReportUserAction extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String channel= params.getRequired(ServerParams.CHANNEL_ID);
            String desc= params.getRequired(ServerParams.DESC);
            String data= params.getRequired(ServerParams.DATA);
            BackgroundEnv.reportUserAction(channel,desc,data);
            return "true";
        }
    }

    public static class CreateDownloadScript extends ServCommand  {
        public boolean getCanCreateJson() { return false; }

        @Override
        public String doCommand(SrvParam params) throws Exception {
            String id = params.getID();
            String file = params.getRequired(ServerParams.FILE);
            String source = params.getRequired(ServerParams.SOURCE);
            List<String> attStrList = params.getOptionalList(ServerParams.ATTRIBUTE);
            List<ScriptAttributes> attList= new ArrayList<ScriptAttributes>(5);
            for(String a : attStrList) {
                attList.add(Enum.valueOf(ScriptAttributes.class,a));
            }
            BackgroundEnv.ScriptRet retval= BackgroundEnv.createDownloadScript(id, file, source, attList);
            return retval!=null ? retval.getServlet() : null;
        }
    }

}

