/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;

import static edu.caltech.ipac.firefly.server.query.AsyncTapQuery.*;
import static edu.caltech.ipac.firefly.server.query.DaliUtil.*;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;


@SearchProcessorImpl(id = AsyncTapQuery.ID, params = {
        @ParamDoc(name = SVC_URL, desc = "base TAP url endpoint excluding '/async'"),
        @ParamDoc(name = QUERY, desc = "query string"),
        @ParamDoc(name = UPLOAD_TNAME, desc = "adql upload select table"),
        @ParamDoc(name = LANG, desc = "defaults to ADQL"),
        @ParamDoc(name = MAXREC, desc = MAXREC_DESC),
        @ParamDoc(name = UPLOAD, desc = UPLOAD_DESC),
        @ParamDoc(name = UPLOAD_COLUMNS, desc = UPLOAD_COLUMNS_DESC),
})
public class AsyncTapQuery extends UwsJobProcessor {
    public static final String ID = "AsyncTapQuery";
    public static final String LANG = "LANG";
    public static final String QUERY = "QUERY";
    public static final String SVC_URL = "serviceUrl";
    public static final String UPLOAD_TNAME = "adqlUploadSelectTable";



    public HttpServiceInput createInput(TableServerRequest request) throws DataAccessException {
        var serviceUrl = request.getParam(SVC_URL);
        var uploadTable= request.getParam(UPLOAD_TNAME);

        HttpServiceInput inputs = HttpServiceInput.createWithCredential(serviceUrl + "/async");
        DaliUtil.handleMaxrec(inputs, request);
        DaliUtil.handleUpload(inputs, request, uploadTable);

        applyIfNotEmpty(request.getParam(QUERY), (v) -> inputs.setParam(QUERY, v));
        inputs.setParam(LANG, request.getParam(LANG, "ADQL"));
        inputs.setParam(REQUEST, "doQuery");
        return inputs;
    }

    @Override
    public DataGroup getResult(TableServerRequest request) throws DataAccessException {
        return getTableResult(getJobUrl() + "/results/result", QueryUtil.getTempDir(request));
    }
}
