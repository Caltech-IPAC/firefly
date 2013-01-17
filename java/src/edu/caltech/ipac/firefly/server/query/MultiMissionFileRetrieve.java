package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@SearchProcessorImpl(id = MultiMissionFileRetrieve.ID)
public class MultiMissionFileRetrieve extends URLFileInfoProcessor {

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
    public URL getURL(ServerRequest sr) throws MalformedURLException { return null; }
}


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
