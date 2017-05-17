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
import edu.caltech.ipac.firefly.server.BaseJsonCmd;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.BackgroundInfoCacher;
import edu.caltech.ipac.firefly.server.rpc.SearchServicesImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Trey Roby
 */
public class SearchServerCommands {




    public static class TableSearch extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            TableServerRequest tsr = params.getTableServerRequest();
            DataGroupPart dgp = new SearchManager().getDataGroup(tsr);
            JSONObject json = JsonTableUtil.toJsonTableModel(dgp, tsr);
            return json.toJSONString();
        }
    }

    public static class TableFindIndex extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            TableServerRequest tsr = params.getTableServerRequest();
            List<String> filterInfo = Arrays.asList(params.getRequired("filterInfo").split(";"));
            tsr.setPageSize(Integer.MAX_VALUE);
            tsr.setStartIndex(0);
            CollectionUtil.Filter<DataObject>[] filters = QueryUtil.convertToDataFilter(filterInfo);
            DataGroupPart dgp = new SearchManager().getDataGroup(tsr);
            return String.valueOf(CollectionUtil.findIndex(dgp.getData().values(), filters));
        }
    }

    public static class SelectedValues extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String filePath = params.getRequired("filePath");
            try {
                String selRows = params.getRequired("selectedRows");
                String columnName = params.getRequired("columnName");
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

    public static class JsonSearch extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            TableServerRequest tsr = params.getTableServerRequest();
            SearchProcessor processor = new SearchManager().getProcessor(tsr.getRequestId());
            if (processor instanceof JsonDataProcessor) {
                return  ((JsonDataProcessor)processor).getData(tsr);
            } else {
                throw new DataAccessException("Unable to resolve a search processor for this request.  Operation aborted:" + tsr.getRequestId());
            }
        }
    }

    public static class GetJSONData extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String reqString = params.getRequired(ServerParams.REQUEST);
            ServerRequest request = ServerRequest.parse(reqString, new ServerRequest());
            return new SearchManager().getJSONData(request);
        }

    }

    public static class PackageRequest extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
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

    public static class ChkFileStatus extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            File f= new File(params.getRequired(ServerParams.SOURCE));
            FileStatus fstatus = new SearchManager().getFileStatus(f);
            return fstatus.toString();
        }

    }

    public static class GetEnumValues extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            File f= new File(params.getRequired(ServerParams.SOURCE));
            RawDataSet data = new SearchManager().getEnumValues(f);
            return data.serialize();
        }

    }


    public static class SubmitBackgroundSearch extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String servReqStr = params.getRequired(ServerParams.REQUEST);
            int waitMil = params.getRequiredInt(ServerParams.WAIT_MILS);

            TableServerRequest serverRequest= ServerRequest.parse(servReqStr, new TableServerRequest());
            Request clientRequest= null;
            if (params.contains(ServerParams.CLIENT_REQUEST))  {
                clientRequest= ServerRequest.parse(params.getOptional(ServerParams.CLIENT_REQUEST),
                        new Request());
            }
            BackgroundStatus bgStat= new SearchServicesImpl().submitBackgroundSearch(
                    serverRequest,clientRequest,waitMil);
            return bgStat.serialize();
        }
    }

    public static class GetStatus extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            BackgroundStatus bgStat= new SearchServicesImpl().getStatus(params.getID(),
                    params.getRequiredBoolean(ServerParams.POLLING));
            return bgStat.serialize();
        }
    }

    public static class AddIDToPushCriteria extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            new SearchServicesImpl().addIDToPushCriteria(params.getID());
            return "true";
        }
    }

    public static class RemoveBgJob extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            BackgroundEnv.removeUserBackgroundInfo(params.getID());
            return "true";
        }
    }

    public static class Cancel extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            BackgroundEnv.cancel(params.getID());
            return "true";
        }
    }

    public static class CleanUp extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            new SearchServicesImpl().cleanup(params.getID());
            return "true";
        }
    }

    public static class DownloadProgress extends BaseJsonCmd {
        public boolean getCanCreateJson() { return false; }

        public String execute(SrvParam params) throws Exception {
            String file= params.getRequired(ServerParams.FILE);
            SearchServices.DownloadProgress dp= new SearchServicesImpl().getDownloadProgress(file);
            return dp.toString();
        }
    }

    public static class SetEmail extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            List<String> idList = params.getIDList();
            String email = params.getRequired(ServerParams.EMAIL);
            BackgroundEnv.setEmail(idList,email);
            return "true";
        }
    }

    public static class SetAttribute extends BaseJsonCmd  {

        public String execute(SrvParam params) throws Exception {
            List<String> idList= params.getIDList();
            String attStr= params.getRequired(ServerParams.ATTRIBUTE);
            JobAttributes att= StringUtils.getEnum(attStr, JobAttributes.Unknown);
            BackgroundEnv.setAttribute(idList,att);
            return "true";
        }
    }

    public static class GetEmail extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String id = params.getID();
            return BackgroundEnv.getEmail(id);
        }
    }

    public static class ResendEmail extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String email= params.getRequired(ServerParams.EMAIL);
            List<String> idList = BackgroundEnv.getUserBackgroundInfo().stream()
                    .filter(BackgroundInfoCacher::isSuccess)
                    .map(BackgroundInfoCacher::getBID)
                    .collect(Collectors.toList());
            BackgroundEnv.resendEmail(idList,email);
            return "true";
        }
    }

    public static class ClearPushEntry extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String id= params.getRequired(ServerParams.ID);
            int idx=   params.getRequiredInt(ServerParams.IDX);
            new SearchServicesImpl().clearPushEntry(id,idx);
            return "true";
        }
    }

    public static class ReportUserAction extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String channel= params.getRequired(ServerParams.CHANNEL_ID);
            String desc= params.getRequired(ServerParams.DESC);
            String data= params.getRequired(ServerParams.DATA);
            new SearchServicesImpl().reportUserAction(channel,desc,data);
            return "true";
        }
    }

    public static class CreateDownloadScript extends BaseJsonCmd  {
        public boolean getCanCreateJson() { return false; }

        @Override
        public String execute(SrvParam params) throws Exception {
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

    public static class GetDataFileValues extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String filePath = params.getRequired(ServerParams.SOURCE);
            String rowsStr = params.getRequired(ServerParams.ROWS);
            List<Integer> rows = new ArrayList<Integer>();
            for (String s : rowsStr.split(", ")) {
                rows.add(Integer.parseInt(s));
            }
            String colName = params.getRequired(ServerParams.COL_NAME);
            List<String> result = (new SearchManager().getDataFileValues(new File(filePath), rows, colName));
            return CollectionUtil.toString(result);
        }
    }

    @Deprecated
    public static class GetRawDataSet extends BaseJsonCmd {

        public String execute(SrvParam params) throws Exception {
            String reqString = params.getRequired(ServerParams.REQUEST);
            TableServerRequest request = TableServerRequest.parse(reqString);
            RawDataSet dataSet = new SearchManager().getRawDataSet(request);
            return dataSet.serialize();
        }

    }

}

