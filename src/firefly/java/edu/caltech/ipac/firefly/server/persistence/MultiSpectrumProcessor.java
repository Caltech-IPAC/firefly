/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author loi
 *         $Id: $
 */

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataGroup;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

@SearchProcessorImpl(id = MultiSpectrumProcessor.PROC_ID, params=
        {@ParamDoc(name=MultiSpectrumProcessor.SOURCE, desc="URL or local path of the table file"),
         @ParamDoc(name=MultiSpectrumProcessor.MODE, desc="one of 'fetch' or 'extract'. Defaults to fetch."),
         @ParamDoc(name=MultiSpectrumProcessor.SPECTR_IDX, desc="Index of spectrum to extract.  Defaults to 0."),
        })
public class MultiSpectrumProcessor extends EmbeddedDbProcessor {
    public static final String PROC_ID = "multi_spectrum";
    public static final String SOURCE = ServerParams.SOURCE;
    public static final String MODE = "mode";
    public static final String SPECTR_IDX = TableServerRequest.TBL_INDEX;

    private enum Mode {fetch, extract};


    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        try {
            String mission = request.getParam(SOURCE);
            Mode mode = Mode.valueOf(request.getParam(MODE, Mode.fetch.name()));
            int spIdx = request.getIntParam(SPECTR_IDX, 0);

        }catch (IllegalArgumentException iax) {
            throw new DataAccessException("Invalid mode value.  Expecting one of 'fetch' or 'extract', but got " + request.getParam(MODE));
        } catch (Exception e) {
            throw new DataAccessException("Fail during QueryIBE:" + e.getMessage(), e);
        }
    }

}
