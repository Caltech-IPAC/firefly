/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;

import java.io.IOException;
import java.util.Map;

@SearchProcessorImpl(id = MultiMissionFileRetrieve.ID)
public class MultiMissionFileRetrieve extends BaseFileInfoProcessor {

    public final static String ID= "MultiMissionFileRetrieve";

    public FileInfo getData(ServerRequest sr) throws DataAccessException {

        TableServerRequest workingReq= new TableServerRequest(sr.getRequestId(),sr);
        insertMissionParameters(workingReq);
        FileInfo retval= null;

        if (!workingReq.getRequestId().equals(sr.getRequestId())) {
            retval= new SearchManager().getFileInfo(workingReq);
        }

        return retval;

    }

    public void insertMissionParameters(ServerRequest sr) {
       Map<String,String>  params= TableResultSearch.getCacheParams(sr);
//        if (params.containsKey(DynUtils.HYDRA_PROJECT_ID)) {
//            sr.setParam(DynUtils.HYDRA_PROJECT_ID, params.get(DynUtils.HYDRA_PROJECT_ID));
//        }
//        if (params.containsKey(DynUtils.QUERY_ID)) {
//            sr.setParam(DynUtils.QUERY_ID,params.get(DynUtils.QUERY_ID));
//        }
//        if (params.containsKey(DynUtils.SEARCH_NAME)) {
//            sr.setParam(DynUtils.SEARCH_NAME,params.get(DynUtils.SEARCH_NAME));
//        }
        if (params.containsKey(CommonParams.SEARCH_PROCESSOR_ID)) {
            sr.setParam(TableServerRequest.ID_KEY, params.get(CommonParams.SEARCH_PROCESSOR_ID));
        }
        for(Map.Entry<String,String> entry : params.entrySet()) {
            sr.setParam(entry.getKey(), entry.getValue());
        }
        DynQueryProcessor.setXmlParams(sr);

    }

    @Override
    protected FileInfo loadData(ServerRequest sr) throws IOException, DataAccessException {
        return null;  // not used.. override getData() directly to use the manager..
    }
}


