/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;

import java.io.File;

import static edu.caltech.ipac.firefly.server.query.DaliQuery.SVC_URL;
import static edu.caltech.ipac.firefly.server.query.DaliUtil.*;


@SearchProcessorImpl(id = DaliQuery.ID, params = {
        @ParamDoc(name = SVC_URL, desc = "service url endpoint"),
        @ParamDoc(name = MAXREC, desc = MAXREC_DESC),
        @ParamDoc(name = UPLOAD, desc = UPLOAD_DESC),
        @ParamDoc(name = UPLOAD_COLUMNS, desc = UPLOAD_COLUMNS_DESC),
        @ParamDoc(name = REQUEST, desc = REQUEST_DESC),
        @ParamDoc(name = RESPONSEFORMAT, desc = RESPONSEFORMAT_DESC),
        @ParamDoc(name = RUNID, desc = RUNID_DESC),
})
public class DaliQuery extends EmbeddedDbProcessor {
    public static final String ID = "DaliQuery";
    public static final String SVC_URL = "serviceUrl";

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        var inputs = createInput(req);
        try {
            File outFile = File.createTempFile(req.getRequestId()+"-", ".vot", ServerContext.getTempWorkDir());
            HttpServices.Status status = HttpServices.getWithAuth(inputs, HttpServices.defaultHandler(outFile));
            if (status.isError()) {
                throw DaliUtil.createDax("Failed to retrieve the result from", inputs.getRequestUrl(), status.getException());
            }
            DataGroup[] results = VoTableReader.voToDataGroups(outFile.getAbsolutePath());
            return results.length > 0 ? results[0] : null;

        } catch (Exception e) {
            throw DaliUtil.createDax("Failed to retrieve the result from", inputs.getRequestUrl(), e);
        }
    }

    /**
     * Return a HttpServiceInput with the standard DALI parameters taken from the given request.
     * override this method to be able to create a HttpServiceInput object
     * @param request request info needed to create HttpServiceInput object
     * @return HttpServiceInput object or null
     * @throws DataAccessException when encountering an error
     */
    public HttpServiceInput createInput(TableServerRequest request) throws DataAccessException {
        HttpServiceInput inputs = HttpServiceInput.createWithCredential(getSvcUrl(request));
        DaliUtil.populateKnownInputs(inputs, request);
        return inputs;
    }

    /**
     * Override if serviceUrl is not part of the request.
     * @param request 
     * @return
     */
    protected String getSvcUrl(TableServerRequest request) { return request.getParam(SVC_URL); }

}
