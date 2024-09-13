/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;

import java.util.Arrays;
import java.util.List;

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

    // links taken from src/firefly/js/ui/tap/TapKnownServices.js#makeServices
    private static final List<String> SVC_RUNID_NOT_SUPPORTED = Arrays.asList(
            "https://irsa.ipac.caltech.edu/TAP"                // Return exception, BAD_REQUEST: RUNID not implemented
//            "https://exoplanetarchive.ipac.caltech.edu/TAP/",   // Bad implementation. it replaces it with its own identifier, e.g. 109294
//            "https://koa.ipac.caltech.edu/TAP/",                // Bad implementation. it replaces it with its own identifier, e.g. 109294
//            "https://heasarc.gsfc.nasa.gov/xamin/vo/tap",       // Accepted the parameter, but did not return its value
//            "https://vao.stsci.edu/CAOMTAP/TapService.aspx",    // Accepted the parameter, but did not return its value
//            "https://gea.esac.esa.int/tap-server/tap",          // Accepted the parameter, but did not return its value
//            "https://dc.g-vo.org/tap",                          // Accepted the parameter, but did not return its value
//            "https://archives.esac.esa.int/hsa/whsa-tap-server/tap"     // Accepted the parameter, but did not return its value
    );

    public HttpServiceInput createInput(TableServerRequest request) throws DataAccessException {
        var serviceUrl = request.getParam(SVC_URL);
        var uploadTable= request.getParam(UPLOAD_TNAME);
        boolean runIdSupported = !SVC_RUNID_NOT_SUPPORTED.contains(serviceUrl);

        HttpServiceInput inputs = HttpServiceInput.createWithCredential(serviceUrl + "/async");
        DaliUtil.handleMaxrec(inputs, request);
        DaliUtil.handleUpload(inputs, request, uploadTable);

        applyIfNotEmpty(request.getParam(QUERY), (v) -> inputs.setParam(QUERY, v));
        // use table's title as RUNID.  RUNID is limited to 64 chars.  If more than 64, truncate then add '...' to indicate it was truncated.
        applyIfNotEmpty(request.getMeta("title"), (v) -> {
            String runId = v.length() > 64 ? v.substring(0, 61) + "..." : v;
            // only send RUNID if it's supported.
            if (runIdSupported)     inputs.setParam(RUNID, runId);
            getJob().getJobInfo().setLocalRunId(runId);        // save the value locally for display
        });
        inputs.setParam(LANG, request.getParam(LANG, "ADQL"));
        inputs.setParam(REQUEST, "doQuery");

        return inputs;
    }

    @Override
    public DataGroup getResult(TableServerRequest request) throws DataAccessException {
        return getTableResult(getJobUrl() + "/results/result", QueryUtil.getTempDir(request));
    }
}
